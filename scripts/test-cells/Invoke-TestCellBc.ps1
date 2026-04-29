param(
    [ValidateSet('status', 'ensure', 'stop')]
    [string]$Mode = 'status',
    [string]$ConfigPath = ''
)

$ErrorActionPreference = 'Stop'
$OutputEncoding = [Console]::OutputEncoding = [Text.UTF8Encoding]::new($false)

if ([string]::IsNullOrWhiteSpace($ConfigPath)) {
    $ConfigPath = Join-Path $PSScriptRoot 'cells-bc.json'
}

. (Join-Path $PSScriptRoot 'TestCellBcCommon.ps1')

$bcConfig = Load-TestCellBcConfig -ConfigPath $ConfigPath

$result = [ordered]@{
    mode = $Mode
    ok = $false
    startedAt = (Get-Date).ToString('s')
    configPath = $bcConfig.path
    serverDir = $bcConfig.serverDir
    listenHost = $bcConfig.listenHost
    listenPort = $bcConfig.listenPort
    javaPath = $bcConfig.javaPath
    serverJar = $bcConfig.serverJar
    discoveredBackends = @()
    defaultBackend = $null
    pid = $null
    running = $false
    configChanged = $false
    skipped = @()
    errors = New-Object System.Collections.Generic.List[string]
}

function Add-Error {
    param([string]$Message)
    $result.errors.Add($Message) | Out-Null
}

function Get-ListeningPort {
    param([int]$Port)

    Get-NetTCPConnection -State Listen -ErrorAction SilentlyContinue |
        Where-Object { $_.LocalPort -eq $Port } |
        Select-Object -First 1
}

function Get-BcProcessIdsByPort {
    param([int]$Port)

    $ids = New-Object 'System.Collections.Generic.HashSet[int]'
    $listener = Get-ListeningPort -Port $Port
    if ($null -ne $listener) {
        $ids.Add([int]$listener.OwningProcess) | Out-Null
    }
    return @($ids)
}

function Get-ProcessesByIds {
    param([int[]]$ProcessIds)

    $items = @()
    foreach ($processId in @($ProcessIds | Where-Object { $_ })) {
        $process = Get-CimInstance Win32_Process -Filter "ProcessId=$processId" -ErrorAction SilentlyContinue |
            Select-Object -First 1
        if ($null -ne $process) {
            $items += $process
        }
    }
    return $items
}

function Get-BcJavaProcess {
    $portProcess = @(Get-ProcessesByIds -ProcessIds (Get-BcProcessIdsByPort -Port $bcConfig.listenPort)) |
        Where-Object { $_.Name -eq 'java.exe' } |
        Select-Object -First 1
    if ($null -ne $portProcess) {
        return $portProcess
    }

    return Get-CimInstance Win32_Process |
        Where-Object {
            $_.Name -eq 'java.exe' -and
            $_.CommandLine -match (Get-TestCellCommandLinePathPattern -Path $bcConfig.serverDir)
        } |
        Select-Object -First 1
}

function Get-BcCmdProcesses {
    @(Get-CimInstance Win32_Process |
        Where-Object {
            $_.Name -eq 'cmd.exe' -and
            $_.CommandLine -match (Get-TestCellCommandLinePathPattern -Path $bcConfig.serverDir)
        })
}

function Wait-ForListeningPort {
    param(
        [int]$Port,
        [int]$TimeoutSec
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSec)
    while ((Get-Date) -lt $deadline) {
        if (Get-ListeningPort -Port $Port) {
            return $true
        }
        Start-Sleep -Seconds 2
    }
    return $false
}

function Wait-ForPortRelease {
    param(
        [int]$Port,
        [int]$TimeoutSec
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSec)
    while ((Get-Date) -lt $deadline) {
        if (-not (Get-ListeningPort -Port $Port)) {
            return $true
        }
        Start-Sleep -Seconds 1
    }
    return $false
}

function Ensure-BcDirectories {
    foreach ($path in @(
        $bcConfig.serverDir,
        (Join-Path $bcConfig.serverDir 'plugins'),
        (Join-Path $bcConfig.serverDir 'logs')
    )) {
        if (-not (Test-Path -LiteralPath $path)) {
            New-Item -ItemType Directory -Path $path | Out-Null
        }
    }
}

function Get-BcConfigFilePath {
    return Join-Path $bcConfig.serverDir 'config.yml'
}

function Sync-BcConfigFile {
    param(
        [object[]]$Backends,
        [string]$DefaultBackendId
    )

    Ensure-BcDirectories
    $targetPath = Get-BcConfigFilePath
    $text = New-TestCellBcWaterfallConfigText -Config @{
        listenHost = $bcConfig.listenHost
        listenPort = $bcConfig.listenPort
        onlineMode = $bcConfig.onlineMode
        ipForward = $bcConfig.ipForward
        forgeSupport = $bcConfig.forgeSupport
        motd = $bcConfig.motd
        maxPlayers = $bcConfig.maxPlayers
        serverConnectTimeoutMs = $bcConfig.serverConnectTimeoutMs
    } -Backends $Backends -DefaultBackendId $DefaultBackendId

    $changed = $true
    if (Test-Path -LiteralPath $targetPath) {
        $existing = Get-Content -Raw -Encoding UTF8 -LiteralPath $targetPath
        $changed = ($existing -ne $text)
    }

    if ($changed) {
        [System.IO.File]::WriteAllText($targetPath, $text, [System.Text.UTF8Encoding]::new($false))
    }

    return [pscustomobject]@{
        path = $targetPath
        changed = $changed
    }
}

function Start-BcProxy {
    $jarPath = Join-Path $bcConfig.serverDir $bcConfig.serverJar
    if (-not (Test-Path -LiteralPath $jarPath)) {
        throw "BC server jar not found: $jarPath"
    }
    if (-not (Test-Path -LiteralPath $bcConfig.javaPath)) {
        throw "BC java runtime not found: $($bcConfig.javaPath)"
    }

    $inner = ('cd /d "{0}" & title BlackBoxPro-BC & "{1}" -Xms{2}M -Xmx{3}M -jar "{4}"' -f
        $bcConfig.serverDir,
        $bcConfig.javaPath,
        $bcConfig.minMemoryMb,
        $bcConfig.maxMemoryMb,
        $jarPath)

    $arg = '/c start "" /min cmd /k "' + $inner + '"'
    Start-Process -FilePath 'cmd.exe' -ArgumentList $arg -WindowStyle Hidden | Out-Null
    return (Wait-ForListeningPort -Port $bcConfig.listenPort -TimeoutSec 60)
}

function Stop-BcProxy {
    $stopped = [ordered]@{
        cmdPids = @()
        javaPids = @()
        portReleased = $false
    }

    $bcCmds = @(Get-BcCmdProcesses)
    $bcCmdDescendants = @(Get-TestCellDescendantProcesses -RootProcessIds @($bcCmds | Select-Object -ExpandProperty ProcessId))
    $javaIds = New-Object 'System.Collections.Generic.HashSet[int]'
    foreach ($proc in $bcCmdDescendants) {
        if ($proc.Name -eq 'java.exe') {
            $javaIds.Add([int]$proc.ProcessId) | Out-Null
            if ($stopped.javaPids -notcontains $proc.ProcessId) {
                $stopped.javaPids += $proc.ProcessId
            }
        }
    }
    $bcTree = Stop-TestCellProcessTree -RootProcesses $bcCmds
    $stopped.cmdPids = @($stopped.cmdPids + @($bcTree.rootPids) | Select-Object -Unique)

    foreach ($proc in @(Get-ProcessesByIds -ProcessIds (Get-BcProcessIdsByPort -Port $bcConfig.listenPort))) {
        if ($proc.Name -eq 'java.exe') {
            $javaIds.Add([int]$proc.ProcessId) | Out-Null
        }
    }
    foreach ($proc in @(Get-CimInstance Win32_Process |
            Where-Object {
                $_.Name -eq 'java.exe' -and
                $_.CommandLine -match (Get-TestCellCommandLinePathPattern -Path $bcConfig.serverDir)
            })) {
        $javaIds.Add([int]$proc.ProcessId) | Out-Null
    }

    foreach ($javaId in @($javaIds)) {
        Stop-Process -Id $javaId -Force -ErrorAction SilentlyContinue
        if ($stopped.javaPids -notcontains $javaId) {
            $stopped.javaPids += $javaId
        }
    }

    $stopped.portReleased = Wait-ForPortRelease -Port $bcConfig.listenPort -TimeoutSec 20
    return [pscustomobject]$stopped
}

function Update-ResultBackends {
    param([object]$Discovery)

    $result.discoveredBackends = @($Discovery.backends | ForEach-Object {
        [ordered]@{
            id = $_.id
            address = $_.address
            serverDir = $_.serverDir
            sourceConfigPath = $_.sourceConfigPath
        }
    })
    $result.defaultBackend = $Discovery.defaultBackendId
    $result.skipped = @($Discovery.skipped)
}

function Get-BcStatusSnapshot {
    $process = Get-BcJavaProcess
    $listener = Get-ListeningPort -Port $bcConfig.listenPort
    return [pscustomobject]@{
        running = ($null -ne $process -and $null -ne $listener)
        pid = if ($null -ne $process) { [int]$process.ProcessId } else { $null }
        configPath = Get-BcConfigFilePath
        listener = $listener
    }
}

$discovery = Get-TestCellBcBackends -SourceConfigPaths $bcConfig.sourceConfigs -DefaultBackendId $bcConfig.defaultBackend
Update-ResultBackends -Discovery $discovery

switch ($Mode) {
    'status' {
        $snapshot = Get-BcStatusSnapshot
        $result.running = $snapshot.running
        $result.pid = $snapshot.pid
        $result.ok = $snapshot.running
        break
    }

    'ensure' {
        if ($discovery.backends.Count -eq 0) {
            Add-Error 'No backend test cells discovered.'
            break
        }

        $sync = Sync-BcConfigFile -Backends $discovery.backends -DefaultBackendId $discovery.defaultBackendId
        $result.configChanged = $sync.changed

        $snapshot = Get-BcStatusSnapshot
        if ($snapshot.running -and $sync.changed) {
            $null = Stop-BcProxy
            $snapshot = Get-BcStatusSnapshot
        }

        if (-not $snapshot.running) {
            $started = Start-BcProxy
            if (-not $started) {
                Add-Error "BC proxy did not start listening on $($bcConfig.listenHost):$($bcConfig.listenPort)."
                break
            }
            $snapshot = Get-BcStatusSnapshot
        }

        $result.running = $snapshot.running
        $result.pid = $snapshot.pid
        $result.ok = $snapshot.running
        break
    }

    'stop' {
        $stopped = Stop-BcProxy
        $snapshot = Get-BcStatusSnapshot
        $result.running = $snapshot.running
        $result.pid = $snapshot.pid
        $result.ok = (-not $snapshot.running)
        $result.stop = $stopped
        if (-not $stopped.portReleased -and -not $result.ok) {
            Add-Error "BC proxy port $($bcConfig.listenPort) is still listening after stop."
        }
        break
    }
}

$result | ConvertTo-Json -Depth 10
