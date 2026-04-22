param(
    [ValidateSet('status', 'ensure', 'smoke', 'stop')]
    [string]$Mode = 'status',
    [string]$CellId,
    [string]$ConfigPath = '',
    [switch]$NoAutoStartServer,
    [switch]$NoAutoStartBot
)

$ErrorActionPreference = 'Stop'
$OutputEncoding = [Console]::OutputEncoding = [Text.UTF8Encoding]::new($false)
if ([string]::IsNullOrWhiteSpace($ConfigPath)) {
    $ConfigPath = Join-Path $PSScriptRoot 'cells.json'
}
. (Join-Path $PSScriptRoot 'TestCellCommon.ps1')

$config = Load-TestCellConfig -ConfigPath $ConfigPath
$cell = Get-TestCell -Config $config -CellId $CellId

$PluginStatusUrl = "http://127.0.0.1:$($cell.pluginHttpPort)/status"
$PluginExecuteUrl = "http://127.0.0.1:$($cell.pluginHttpPort)/execute"
$ModStatusUrl = "http://127.0.0.1:$($cell.modHttpPort)/status"
$ModExecuteUrl = "http://127.0.0.1:$($cell.modHttpPort)/execute"

$result = [ordered]@{
    mode = $Mode
    cellId = $cell.id
    ok = $false
    startedAt = (Get-Date).ToString('s')
    steps = [ordered]@{
        server = [ordered]@{}
        bot = [ordered]@{}
        relay = $null
        test = $null
    }
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

function Get-ListeningProcessIds {
    param([int[]]$Ports)

    $ids = New-Object System.Collections.Generic.HashSet[int]
    foreach ($port in $Ports) {
        $listener = Get-ListeningPort -Port $port
        if ($null -ne $listener) {
            $ids.Add([int]$listener.OwningProcess) | Out-Null
        }
    }
    return @($ids)
}

function Get-ProcessesByIds {
    param([int[]]$ProcessIds)

    $processes = @()
    foreach ($processId in @($ProcessIds | Where-Object { $_ })) {
        $process = Get-CimInstance Win32_Process -Filter "ProcessId=$processId" -ErrorAction SilentlyContinue |
            Select-Object -First 1
        if ($null -ne $process) {
            $processes += $process
        }
    }
    return $processes
}

function Wait-ForPorts {
    param(
        [int[]]$Ports,
        [int]$TimeoutSec
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSec)
    while ((Get-Date) -lt $deadline) {
        $allReady = $true
        foreach ($port in $Ports) {
            if (-not (Get-ListeningPort -Port $port)) {
                $allReady = $false
                break
            }
        }
        if ($allReady) {
            return $true
        }
        Start-Sleep -Seconds 2
    }
    return $false
}

function Invoke-JsonRequest {
    param(
        [string]$Uri,
        [string]$Method = 'GET',
        [object]$Body = $null,
        [int]$TimeoutSec = 30
    )

    try {
        if ($null -eq $Body) {
            $content = Invoke-WebRequest -Uri $Uri -Method $Method -UseBasicParsing -TimeoutSec $TimeoutSec |
                Select-Object -ExpandProperty Content
        } else {
            $jsonBody = if ($Body -is [string]) { $Body } else { $Body | ConvertTo-Json -Compress -Depth 20 }
            $content = Invoke-WebRequest -Uri $Uri -Method $Method -UseBasicParsing -TimeoutSec $TimeoutSec -ContentType 'application/json; charset=utf-8' -Body $jsonBody |
                Select-Object -ExpandProperty Content
        }

        return [ordered]@{
            ok = $true
            content = $content
            json = if ($content) { $content | ConvertFrom-Json } else { $null }
        }
    } catch {
        return [ordered]@{
            ok = $false
            error = $_.Exception.Message
            content = $null
            json = $null
        }
    }
}

function Get-ServerProcess {
    $portOwner = @(Get-ProcessesByIds -ProcessIds (Get-ListeningProcessIds -Ports @($cell.serverPort, $cell.pluginHttpPort))) |
        Where-Object { $_.Name -eq 'java.exe' } |
        Select-Object -First 1

    if ($null -ne $portOwner) {
        return $portOwner
    }

    return Get-CimInstance Win32_Process |
        Where-Object {
            $_.Name -eq 'java.exe' -and
            $_.CommandLine -match [regex]::Escape($cell.serverJar)
        } |
        Select-Object -First 1
}

function Get-ServerCmdProcesses {
    @(Get-CimInstance Win32_Process |
        Where-Object {
            $_.Name -eq 'cmd.exe' -and
            $_.CommandLine -match [regex]::Escape($cell.serverDir)
        })
}

function Update-ServerConfigForCell {
    $configPath = Join-Path $cell.serverDir 'plugins/BlackBoxPro/config.yml'
    if (-not (Test-Path -LiteralPath $configPath)) {
        return
    }

    $pluginConfig = @(
        '# BlackBoxPro plugin config'
        'debug: false'
        'response-timeout-ms: 10000'
        "http-port: $($cell.pluginHttpPort)"
        'test-mode: dual'
        "mod-http-address: `"http://localhost:$($cell.modHttpPort)`""
        ''
    )
    [System.IO.File]::WriteAllLines($configPath, $pluginConfig, [System.Text.UTF8Encoding]::new($false))
}

function Start-Server {
    Update-ServerConfigForCell

    $inner = ('cd /d "{0}" & title BlackBoxPro-{1} & "{2}" -Xms{3}M -Xmx{4}M -XX:+UseG1GC -XX:+AggressiveOpts -XX:+UseCompressedOops -jar {5}' -f
        $cell.serverDir,
        $cell.id,
        $cell.serverJava,
        $cell.serverMinMemoryMb,
        $cell.serverMaxMemoryMb,
        $cell.serverJar)

    $arg = '/c start "" /min cmd /k "' + $inner + '"'
    Start-Process -FilePath 'cmd.exe' -ArgumentList $arg -WindowStyle Hidden | Out-Null
    return (Wait-ForPorts -Ports @($cell.serverPort, $cell.pluginHttpPort) -TimeoutSec 120)
}

function Get-BotProcess {
    $portOwner = @(Get-ProcessesByIds -ProcessIds (Get-ListeningProcessIds -Ports @($cell.modHttpPort))) |
        Where-Object { $_.Name -in @('javaw.exe', 'java.exe') } |
        Select-Object -First 1

    if ($null -ne $portOwner) {
        return $portOwner
    }

    $direct = Get-CimInstance Win32_Process |
        Where-Object {
            $_.Name -eq 'javaw.exe' -and
            $_.CommandLine -match [regex]::Escape($cell.botVersionDir)
        } |
        Select-Object -First 1

    if ($null -ne $direct) {
        return $direct
    }

    $listener = Get-ListeningPort -Port $cell.modHttpPort
    if ($null -eq $listener) {
        return $null
    }

    return Get-CimInstance Win32_Process -Filter "ProcessId=$($listener.OwningProcess)" | Select-Object -First 1
}

function Stop-CellProcesses {
    $stopped = [ordered]@{
        serverCmdPids = @()
        serverJavaPids = @()
        botJavawPids = @()
        freedPorts = @{}
    }

    $serverCmds = @(Get-ServerCmdProcesses)
    foreach ($proc in $serverCmds) {
        Stop-Process -Id $proc.ProcessId -Force -ErrorAction SilentlyContinue
        $stopped.serverCmdPids += $proc.ProcessId
    }

    $serverJavaIds = New-Object System.Collections.Generic.HashSet[int]
    foreach ($proc in @(Get-ProcessesByIds -ProcessIds (Get-ListeningProcessIds -Ports @($cell.serverPort, $cell.pluginHttpPort)))) {
        if ($proc.Name -eq 'java.exe') {
            $serverJavaIds.Add([int]$proc.ProcessId) | Out-Null
        }
    }
    foreach ($proc in @(Get-CimInstance Win32_Process |
        Where-Object {
            $_.Name -eq 'java.exe' -and
            $_.CommandLine -match [regex]::Escape($cell.serverJar)
        })) {
        $serverJavaIds.Add([int]$proc.ProcessId) | Out-Null
    }
    $serverJava = @(Get-ProcessesByIds -ProcessIds @($serverJavaIds))
    foreach ($proc in $serverJava) {
        Stop-Process -Id $proc.ProcessId -Force -ErrorAction SilentlyContinue
        $stopped.serverJavaPids += $proc.ProcessId
    }

    $botJavaIds = New-Object System.Collections.Generic.HashSet[int]
    foreach ($proc in @(Get-ProcessesByIds -ProcessIds (Get-ListeningProcessIds -Ports @($cell.modHttpPort)))) {
        if ($proc.Name -in @('javaw.exe', 'java.exe')) {
            $botJavaIds.Add([int]$proc.ProcessId) | Out-Null
        }
    }
    foreach ($proc in @(Get-CimInstance Win32_Process |
        Where-Object {
            $_.Name -eq 'javaw.exe' -and
            $_.CommandLine -match [regex]::Escape($cell.botVersionDir)
        })) {
        $botJavaIds.Add([int]$proc.ProcessId) | Out-Null
    }
    $botJavaw = @(Get-ProcessesByIds -ProcessIds @($botJavaIds))
    foreach ($proc in $botJavaw) {
        Stop-Process -Id $proc.ProcessId -Force -ErrorAction SilentlyContinue
        $stopped.botJavawPids += $proc.ProcessId
    }

    Start-Sleep -Seconds 3
    $stopped.freedPorts = [ordered]@{
        serverPort = (-not (Get-ListeningPort -Port $cell.serverPort))
        pluginHttpPort = (-not (Get-ListeningPort -Port $cell.pluginHttpPort))
        modHttpPort = (-not (Get-ListeningPort -Port $cell.modHttpPort))
    }
    return [pscustomobject]$stopped
}

function Get-BotClasspath {
    $botJsonPath = Join-Path $cell.botVersionDir 'bot.json'
    if (-not (Test-Path -LiteralPath $botJsonPath)) {
        throw "bot.json not found: $botJsonPath"
    }

    $botJson = Get-Content -Raw -Encoding UTF8 -LiteralPath $botJsonPath | ConvertFrom-Json
    $entries = New-Object System.Collections.Generic.List[string]

    foreach ($lib in $botJson.libraries) {
        $artifactPath = $null
        if (($lib.PSObject.Properties.Name -contains 'downloads') -and $lib.downloads -and ($lib.downloads.PSObject.Properties.Name -contains 'artifact')) {
            $artifact = $lib.downloads.artifact
            if ($null -ne $artifact -and ($artifact.PSObject.Properties.Name -contains 'path') -and -not [string]::IsNullOrWhiteSpace($artifact.path)) {
                $artifactPath = $artifact.path
            }
        }
        if ($artifactPath) {
            $full = Join-Path $cell.librariesDir $artifactPath
            if ((Test-Path -LiteralPath $full) -and -not $entries.Contains($full)) {
                $entries.Add($full) | Out-Null
            }
        }
    }

    $extra = @(
        'net/minecraftforge/forge/1.12.2-14.23.5.2768/forge-1.12.2-14.23.5.2768.jar',
        'net/minecraft/launchwrapper/1.12/launchwrapper-1.12.jar',
        'org/ow2/asm/asm-all/5.2/asm-all-5.2.jar',
        'org/jline/jline/3.5.1/jline-3.5.1.jar',
        'com/typesafe/akka/akka-actor_2.11/2.3.3/akka-actor_2.11-2.3.3.jar',
        'com/typesafe/config/1.2.1/config-1.2.1.jar',
        'org/scala-lang/scala-actors-migration_2.11/1.1.0/scala-actors-migration_2.11-1.1.0.jar',
        'org/scala-lang/scala-compiler/2.11.1/scala-compiler-2.11.1.jar',
        'org/scala-lang/plugins/scala-continuations-library_2.11/1.0.2/scala-continuations-library_2.11-1.0.2.jar',
        'org/scala-lang/plugins/scala-continuations-plugin_2.11.1/1.0.2/scala-continuations-plugin_2.11.1-1.0.2.jar',
        'org/scala-lang/scala-library/2.11.1/scala-library-2.11.1.jar',
        'org/scala-lang/scala-parser-combinators_2.11/1.0.1/scala-parser-combinators_2.11-1.0.1.jar',
        'org/scala-lang/scala-reflect/2.11.1/scala-reflect-2.11.1.jar',
        'org/scala-lang/scala-swing_2.11/1.0.1/scala-swing_2.11-1.0.1.jar',
        'org/scala-lang/scala-xml_2.11/1.0.2/scala-xml_2.11-1.0.2.jar',
        'lzma/lzma/0.0.1/lzma-0.0.1.jar',
        'java3d/vecmath/1.5.2/vecmath-1.5.2.jar',
        'net/sf/trove4j/trove4j/3.0.3/trove4j-3.0.3.jar',
        'org/apache/maven/maven-artifact/3.5.3/maven-artifact-3.5.3.jar'
    )

    foreach ($rel in $extra) {
        $full = Join-Path $cell.librariesDir $rel
        if ((Test-Path -LiteralPath $full) -and -not $entries.Contains($full)) {
            $entries.Add($full) | Out-Null
        }
    }

    $versionJar = Join-Path $cell.botVersionDir "$($cell.botVersionName).jar"
    if (-not (Test-Path -LiteralPath $versionJar)) {
        throw "Bot version jar not found: $versionJar"
    }
    if (-not $entries.Contains($versionJar)) {
        $entries.Add($versionJar) | Out-Null
    }

    return [string]::Join(';', $entries)
}

function Start-Bot {
    $botMinMemoryMb = if ($null -ne $cell.botMinMemoryMb) { [int]$cell.botMinMemoryMb } else { 512 }
    $botMaxMemoryMb = if ($null -ne $cell.botMaxMemoryMb) { [int]$cell.botMaxMemoryMb } else { 1024 }
    $cp = Get-BotClasspath
    $args = @(
        '-XX:+UseG1GC',
        '-XX:-UseAdaptiveSizePolicy',
        '-XX:-OmitStackTraceInFastThrow',
        '-Dfml.ignoreInvalidMinecraftCertificates=True',
        '-Dfml.ignorePatchDiscrepancies=True',
        '-Dlog4j2.formatMsgNoLookups=true',
        "-Dblackboxpro.httpPort=$($cell.modHttpPort)",
        "-Xms$($botMinMemoryMb)m",
        "-Xmx$($botMaxMemoryMb)m",
        "-Djava.library.path=$($cell.botVersionDir)/bot-natives",
        '-cp', $cp,
        'net.minecraft.launchwrapper.Launch',
        '--username', $cell.botPlayer,
        '--version', $cell.botVersionName,
        '--gameDir', $cell.botVersionDir,
        '--assetsDir', $cell.assetsDir,
        '--assetIndex', '1.12',
        '--uuid', '0000000000003007998FFD47A593C368',
        '--accessToken', '0000000000003007998FFD47A593C368',
        '--userType', 'msa',
        '--tweakClass', 'net.minecraftforge.fml.common.launcher.FMLTweaker',
        '--versionType', 'Forge',
        '--height', '480',
        '--width', '854'
    )

    Start-Process -FilePath $cell.botJava -ArgumentList $args -WorkingDirectory $cell.botVersionDir | Out-Null
    return (Wait-ForPorts -Ports @($cell.modHttpPort) -TimeoutSec 180)
}

function Ensure-Server {
    $proc = Get-ServerProcess
    $serverPort = Get-ListeningPort -Port $cell.serverPort
    $pluginPort = Get-ListeningPort -Port $cell.pluginHttpPort
    $started = $false

    if (-not $proc -or -not $serverPort -or -not $pluginPort) {
        if ($NoAutoStartServer) {
            Add-Error "Server for $($cell.id) is not ready and -NoAutoStartServer was set."
        } else {
            $started = Start-Server
            if (-not $started) {
                Add-Error "Failed to start server for $($cell.id)."
            }
        }
    }

    $status = Invoke-JsonRequest -Uri $PluginStatusUrl -TimeoutSec 5
    $result.steps.server = [ordered]@{
        processId = if (Get-ServerProcess) { (Get-ServerProcess).ProcessId } else { $null }
        started = $started
        pluginStatus = $status
    }
}

function Ensure-Bot {
    $proc = Get-BotProcess
    $modPort = Get-ListeningPort -Port $cell.modHttpPort
    $started = $false

    if (-not $proc -or -not $modPort) {
        if ($NoAutoStartBot) {
            Add-Error "Bot for $($cell.id) is not ready and -NoAutoStartBot was set."
        } else {
            $started = Start-Bot
            if (-not $started) {
                Add-Error "Failed to start bot for $($cell.id)."
            }
        }
    }

    $statusBefore = Invoke-JsonRequest -Uri $ModStatusUrl -TimeoutSec 5
    $connectResult = $null
    if ($statusBefore.ok -and $statusBefore.json -and -not $statusBefore.json.ready) {
        $connectResult = Invoke-JsonRequest -Uri $ModExecuteUrl -Method POST -TimeoutSec 40 -Body @{
            id = "cell-connect-$($cell.id)-$([DateTimeOffset]::UtcNow.ToUnixTimeSeconds())"
            action = 'connect_to_server'
            params = @{
                ip = '127.0.0.1'
                port = $cell.serverPort
            }
        }
        Start-Sleep -Seconds 3
    }

    $statusAfter = Invoke-JsonRequest -Uri $ModStatusUrl -TimeoutSec 5
    $result.steps.bot = [ordered]@{
        processId = if (Get-BotProcess) { (Get-BotProcess).ProcessId } else { $null }
        started = $started
        connectResult = $connectResult
        statusBefore = $statusBefore
        statusAfter = $statusAfter
    }
}

function Run-RelayCheck {
    $relay = Invoke-JsonRequest -Uri $PluginExecuteUrl -Method POST -TimeoutSec 20 -Body @{
        id = "cell-relay-$($cell.id)-$([DateTimeOffset]::UtcNow.ToUnixTimeSeconds())"
        action = 'query_player_state'
        params = @{}
        target = $cell.botPlayer
    }
    $result.steps.relay = $relay
    if (-not $relay.ok) {
        Add-Error "Relay check failed for $($cell.id): $($relay.error)"
    } elseif (-not $relay.json -or $relay.json.status -ne 'success') {
        Add-Error "Relay check did not return success for $($cell.id)."
    }
}

function Run-Smoke {
    $response = Invoke-JsonRequest -Uri $PluginExecuteUrl -Method POST -TimeoutSec 1200 -Body @{
        id = "cell-smoke-$($cell.id)-$([DateTimeOffset]::UtcNow.ToUnixTimeSeconds())"
        action = 'run_test'
        params = @{
            player = $cell.botPlayer
            scope = 'smoke'
        }
    }

    $result.steps.test = $response
    if (-not $response.ok) {
        Add-Error "Smoke request failed for $($cell.id): $($response.error)"
    } elseif (-not $response.json -or $response.json.status -ne 'success') {
        Add-Error "Smoke did not return success JSON for $($cell.id)."
    }
}

try {
    if ($Mode -eq 'status') {
        $result.steps.server = [ordered]@{
            processId = if (Get-ServerProcess) { (Get-ServerProcess).ProcessId } else { $null }
            started = $false
            pluginStatus = Invoke-JsonRequest -Uri $PluginStatusUrl -TimeoutSec 5
        }
        $result.steps.bot = [ordered]@{
            processId = if (Get-BotProcess) { (Get-BotProcess).ProcessId } else { $null }
            started = $false
            connectResult = $null
            statusBefore = Invoke-JsonRequest -Uri $ModStatusUrl -TimeoutSec 5
            statusAfter = Invoke-JsonRequest -Uri $ModStatusUrl -TimeoutSec 5
        }
    } elseif ($Mode -eq 'stop') {
        $result.steps.server = [ordered]@{
            stop = Stop-CellProcesses
        }
    } else {
        Ensure-Server
        Ensure-Bot
        if ($result.errors.Count -eq 0) {
            Run-RelayCheck
        }
        if ($Mode -eq 'smoke' -and $result.errors.Count -eq 0) {
            Run-Smoke
        }
    }

    $result.ok = ($result.errors.Count -eq 0)
}
catch {
    Add-Error $_.Exception.Message
    $result.ok = $false
}
finally {
    $result.finishedAt = (Get-Date).ToString('s')
    $result | ConvertTo-Json -Depth 12
    if (-not $result.ok) {
        exit 1
    }
}
