param(
    [string]$BcConfigPath = '',
    [string]$CellConfigPath = '',
    [string]$BotCellId = 'cell-02',
    [string]$DefaultBackendId = 'cell-02',
    [string]$TargetBackendId = 'cell-03',
    [string[]]$BackendCellIds = @(),
    [string]$BcConnectHost = 'localhost',
    [string]$Owner = '',
    [string]$CrossServerCommandTemplate = 'bbswitch {target}'
)

$ErrorActionPreference = 'Stop'
$OutputEncoding = [Console]::OutputEncoding = [Text.UTF8Encoding]::new($false)

if ([string]::IsNullOrWhiteSpace($BcConfigPath)) {
    $BcConfigPath = Join-Path $PSScriptRoot 'cells-bc.json'
}
if ([string]::IsNullOrWhiteSpace($CellConfigPath)) {
    $CellConfigPath = Join-Path $PSScriptRoot 'cells.json'
}
if ([string]::IsNullOrWhiteSpace($Owner)) {
    $Owner = 'bc-smoke-' + (Get-Date -Format 'yyyyMMdd-HHmmss') + "-$PID"
}

. (Join-Path $PSScriptRoot 'TestCellCommon.ps1')
. (Join-Path $PSScriptRoot 'TestCellBcCommon.ps1')

$bcConfig = Load-TestCellBcConfig -ConfigPath $BcConfigPath
$cellConfig = Load-TestCellConfig -ConfigPath $CellConfigPath
$botCell = Get-TestCell -Config $cellConfig -CellId $BotCellId
$normalizedBackendIds = @($BackendCellIds)
if ($normalizedBackendIds.Count -eq 1 -and $normalizedBackendIds[0] -match ',') {
    $normalizedBackendIds = @($normalizedBackendIds[0].Split(',') | ForEach-Object { $_.Trim() } | Where-Object { -not [string]::IsNullOrWhiteSpace($_) })
}
if ($normalizedBackendIds.Count -eq 0) {
    $normalizedBackendIds = @($DefaultBackendId, $TargetBackendId)
}
$normalizedBackendIds = Get-TestCellBcUniqueCellIds -CellIds $normalizedBackendIds
if (-not ($normalizedBackendIds -contains $DefaultBackendId)) {
    $normalizedBackendIds = @($DefaultBackendId) + @($normalizedBackendIds)
}
$backendCells = @($normalizedBackendIds | ForEach-Object { Get-TestCell -Config $cellConfig -CellId $_ })
$defaultBackend = @($backendCells | Where-Object { $_.id -eq $DefaultBackendId } | Select-Object -First 1)[0]
$targetBackends = @($backendCells | Where-Object { $_.id -ne $defaultBackend.id })
$targetBackend = if ($targetBackends.Count -gt 0) { $targetBackends[0] } else { $null }

$result = [ordered]@{
    ok = $false
    startedAt = (Get-Date).ToString('s')
    owner = $Owner
    bcConfigPath = $bcConfig.path
    cellConfigPath = $cellConfig.path
    botCellId = $botCell.id
    defaultBackendId = $defaultBackend.id
    targetBackendId = if ($null -ne $targetBackend) { $targetBackend.id } else { $null }
    backendCellIds = @($backendCells | ForEach-Object { $_.id })
    targetBackendIds = @($targetBackends | ForEach-Object { $_.id })
    bcListen = "$($bcConfig.listenHost):$($bcConfig.listenPort)"
    bcConnect = "$($BcConnectHost):$($bcConfig.listenPort)"
    crossServerCommandTemplate = $CrossServerCommandTemplate
    steps = [ordered]@{
        prepare = $null
        helperBuild = $null
        helperDeploy = $null
        backendStart = @()
        bcEnsure = $null
        botStart = $null
        connectToBc = $null
        defaultBackendRelay = $null
        crossServerCommand = $null
        targetBackendRelay = $null
        backendRelays = @()
        crossServerCommands = @()
        cleanup = [ordered]@{}
    }
    errors = New-Object System.Collections.Generic.List[string]
}

function Add-Error {
    param([string]$Message)
    $result.errors.Add($Message) | Out-Null
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

function Invoke-ExternalJsonScript {
    param(
        [string]$ScriptPath,
        [string[]]$Arguments = @()
    )

    $output = & powershell -ExecutionPolicy Bypass -File $ScriptPath @Arguments
    $exitCode = $LASTEXITCODE
    $text = ($output | Out-String).Trim()
    $json = $null
    if ($text) {
        $json = $text | ConvertFrom-Json
    }

    return [pscustomobject]@{
        exitCode = $exitCode
        text = $text
        json = $json
    }
}

function Get-ListeningPort {
    param([int]$Port)

    Get-NetTCPConnection -State Listen -ErrorAction SilentlyContinue |
        Where-Object { $_.LocalPort -eq $Port } |
        Select-Object -First 1
}

function Wait-Until {
    param(
        [scriptblock]$Condition,
        [int]$TimeoutSec = 30,
        [int]$IntervalMs = 1000
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSec)
    while ((Get-Date) -lt $deadline) {
        $value = & $Condition
        if ($value) {
            return $value
        }
        Start-Sleep -Milliseconds $IntervalMs
    }
    return $null
}

function Invoke-PluginAction {
    param(
        [object]$Cell,
        [string]$Action,
        [hashtable]$Params = @{},
        [int]$TimeoutSec = 30
    )

    $uri = "http://127.0.0.1:$($Cell.pluginHttpPort)/execute"
    return Invoke-JsonRequest -Uri $uri -Method POST -TimeoutSec $TimeoutSec -Body @{
        id = "bc-smoke-$Action-$([DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds())"
        action = $Action
        params = $Params
        target = $cellConfig.defaults.botPlayer
    }
}

function Invoke-ModAction {
    param(
        [object]$Cell,
        [string]$Action,
        [hashtable]$Params = @{},
        [int]$TimeoutSec = 30
    )

    $uri = "http://127.0.0.1:$($Cell.modHttpPort)/execute"
    return Invoke-JsonRequest -Uri $uri -Method POST -TimeoutSec $TimeoutSec -Body @{
        id = "bc-smoke-$Action-$([DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds())"
        action = $Action
        params = $Params
    }
}

function Expand-CrossServerCommand {
    param(
        [string]$Template,
        [object]$SourceCell,
        [object]$TargetCell,
        [object]$BotCell
    )

    $command = if ([string]::IsNullOrWhiteSpace($Template)) { 'bbswitch {target}' } else { $Template }
    return $command.
        Replace('{source}', [string]$SourceCell.id).
        Replace('{target}', [string]$TargetCell.id).
        Replace('{player}', [string]$BotCell.botPlayer)
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

function Get-CommandLinePathPattern {
    param([string]$Path)

    $variants = @(
        $Path,
        ($Path -replace '/', '\'),
        ($Path -replace '\\', '/')
    ) | Where-Object { -not [string]::IsNullOrWhiteSpace($_) } | Select-Object -Unique

    return '(' + (@($variants | ForEach-Object { [regex]::Escape($_) }) -join '|') + ')'
}

function Get-BackendServerProcess {
    param([object]$Cell)

    $ids = New-Object 'System.Collections.Generic.HashSet[int]'
    foreach ($port in @($Cell.serverPort, $Cell.pluginHttpPort)) {
        $listener = Get-ListeningPort -Port $port
        if ($null -ne $listener) {
            $ids.Add([int]$listener.OwningProcess) | Out-Null
        }
    }

    $fromPorts = @(Get-ProcessesByIds -ProcessIds @($ids)) |
        Where-Object { $_.Name -eq 'java.exe' } |
        Select-Object -First 1
    if ($null -ne $fromPorts) {
        return $fromPorts
    }

    return Get-CimInstance Win32_Process |
        Where-Object {
            $_.Name -eq 'java.exe' -and
            $_.CommandLine -match (Get-CommandLinePathPattern -Path $Cell.serverDir)
        } |
        Select-Object -First 1
}

function Get-BackendCmdProcesses {
    param([object]$Cell)

    @(Get-CimInstance Win32_Process |
        Where-Object {
            $_.Name -eq 'cmd.exe' -and
            $_.CommandLine -match (Get-CommandLinePathPattern -Path $Cell.serverDir)
        })
}

function Stop-BackendServer {
    param([object]$Cell)

    $stopped = [ordered]@{
        cmdPids = @()
        javaPids = @()
    }

    foreach ($proc in @(Get-BackendCmdProcesses -Cell $Cell)) {
        Stop-Process -Id $proc.ProcessId -Force -ErrorAction SilentlyContinue
        $stopped.cmdPids += $proc.ProcessId
    }

    $javaIds = New-Object 'System.Collections.Generic.HashSet[int]'
    foreach ($port in @($Cell.serverPort, $Cell.pluginHttpPort)) {
        $listener = Get-ListeningPort -Port $port
        if ($null -ne $listener) {
            $javaIds.Add([int]$listener.OwningProcess) | Out-Null
        }
    }
    foreach ($proc in @(Get-CimInstance Win32_Process |
            Where-Object {
                $_.Name -eq 'java.exe' -and
                $_.CommandLine -match (Get-CommandLinePathPattern -Path $Cell.serverDir)
            })) {
        $javaIds.Add([int]$proc.ProcessId) | Out-Null
    }

    foreach ($javaId in @($javaIds)) {
        Stop-Process -Id $javaId -Force -ErrorAction SilentlyContinue
        $stopped.javaPids += $javaId
    }

    return [pscustomobject]$stopped
}

function Start-BackendServer {
    param([object]$Cell)

    $launcherPath = Join-Path $Cell.serverDir "start-bc-backend-$($Cell.id).cmd"
    $launcherLog = Join-Path $Cell.serverDir 'logs/bc-smoke-launch.log'
    $launcherContent = @(
        '@echo off'
        ('cd /d "{0}"' -f $Cell.serverDir)
        ('title BC-Backend-{0}' -f $Cell.id)
        ('"{0}" -Xms{1}M -Xmx{2}M -XX:+UseG1GC -XX:+AggressiveOpts -XX:+UseCompressedOops -noverify -jar "{3}"' -f
            $Cell.serverJava,
            $Cell.serverMinMemoryMb,
            $Cell.serverMaxMemoryMb,
            $Cell.serverJar)
    ) -join "`r`n"
    [System.IO.File]::WriteAllText($launcherPath, $launcherContent + "`r`n", [System.Text.UTF8Encoding]::new($false))

    $arg = '/c start "BC-Backend-{0}" /min cmd /k ""{1}" >> "{2}" 2>>&1"' -f
        $Cell.id,
        $launcherPath,
        $launcherLog

    Start-Process -FilePath 'cmd.exe' -ArgumentList $arg -WindowStyle Hidden | Out-Null

    $ready = Wait-Until -TimeoutSec 180 -Condition {
        (Get-ListeningPort -Port $Cell.serverPort) -and (Get-ListeningPort -Port $Cell.pluginHttpPort)
    }
    return [pscustomobject]@{
        ready = [bool]$ready
        launcherPath = $launcherPath
        launcherLog = $launcherLog
    }
}

function Get-BotProcess {
    param([object]$Cell)

    $listener = Get-ListeningPort -Port $Cell.modHttpPort
    if ($null -ne $listener) {
        $proc = Get-CimInstance Win32_Process -Filter "ProcessId=$($listener.OwningProcess)" -ErrorAction SilentlyContinue |
            Select-Object -First 1
        if ($null -ne $proc) {
            return $proc
        }
    }

    return Get-CimInstance Win32_Process |
        Where-Object {
            $_.Name -eq 'javaw.exe' -and
            $_.CommandLine -match [regex]::Escape($Cell.botVersionDir)
        } |
        Select-Object -First 1
}

function Get-BotClasspath {
    param([object]$Cell)

    $botJsonPath = Join-Path $Cell.botVersionDir "$($Cell.botVersionName).json"
    if (-not (Test-Path -LiteralPath $botJsonPath)) {
        throw "Bot json not found: $botJsonPath"
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
            $full = Join-Path $Cell.librariesDir $artifactPath
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
        $full = Join-Path $Cell.librariesDir $rel
        if ((Test-Path -LiteralPath $full) -and -not $entries.Contains($full)) {
            $entries.Add($full) | Out-Null
        }
    }

    $versionJar = Join-Path $Cell.botVersionDir "$($Cell.botVersionName).jar"
    if (-not (Test-Path -LiteralPath $versionJar)) {
        throw "Bot version jar not found: $versionJar"
    }
    if (-not $entries.Contains($versionJar)) {
        $entries.Add($versionJar) | Out-Null
    }

    return [string]::Join(';', $entries)
}

function Stop-Bot {
    param([object]$Cell)

    $proc = Get-BotProcess -Cell $Cell
    if ($null -ne $proc) {
        Stop-Process -Id $proc.ProcessId -Force -ErrorAction SilentlyContinue
    }
}

function Start-Bot {
    param([object]$Cell)

    $cp = Get-BotClasspath -Cell $Cell
    $args = @(
        '-XX:+UseG1GC',
        '-XX:-UseAdaptiveSizePolicy',
        '-XX:-OmitStackTraceInFastThrow',
        '-Dfml.ignoreInvalidMinecraftCertificates=True',
        '-Dfml.ignorePatchDiscrepancies=True',
        '-Dlog4j2.formatMsgNoLookups=true',
        "-Dblackboxpro.httpPort=$($Cell.modHttpPort)",
        "-Xms$([int]$Cell.botMinMemoryMb)m",
        "-Xmx$([int]$Cell.botMaxMemoryMb)m",
        "-Djava.library.path=$($Cell.botVersionDir)/bot-natives",
        '-cp', $cp,
        'net.minecraft.launchwrapper.Launch',
        '--username', $Cell.botPlayer,
        '--version', $Cell.botVersionName,
        '--gameDir', $Cell.botVersionDir,
        '--assetsDir', $Cell.assetsDir,
        '--assetIndex', '1.12',
        '--uuid', '0000000000003007998FFD47A593C368',
        '--accessToken', '0000000000003007998FFD47A593C368',
        '--userType', 'msa',
        '--tweakClass', 'net.minecraftforge.fml.common.launcher.FMLTweaker',
        '--versionType', 'Forge',
        '--height', '480',
        '--width', '854'
    )

    Start-Process -FilePath $Cell.botJava -ArgumentList $args -WorkingDirectory $Cell.botVersionDir | Out-Null
    $ready = Wait-Until -TimeoutSec 180 -Condition { Get-ListeningPort -Port $Cell.modHttpPort }
    return [bool]$ready
}

try {
    $backendCellIdsArg = (@($backendCells | ForEach-Object { $_.id }) -join ',')
    $prepare = Invoke-ExternalJsonScript -ScriptPath (Join-Path $PSScriptRoot 'Prepare-TestCellBcBackends.ps1') -Arguments (
        @(
            '-Mode', 'prepare',
            '-CellConfigPath', $cellConfig.path,
            '-BotCellId', $botCell.id,
            '-BackendCellIds', $backendCellIdsArg
        ) + @(
            '-Owner', $Owner
        )
    )
    $result.steps.prepare = $prepare.json
    if ($prepare.exitCode -ne 0 -or -not $prepare.json.ok) {
        throw 'Prepare-TestCellBcBackends prepare failed.'
    }

    $helper = Invoke-ExternalJsonScript -ScriptPath (Join-Path $PSScriptRoot 'Build-TestCellBcBridgeHelper.ps1') -Arguments @()
    $result.steps.helperBuild = $helper.json
    if ($helper.exitCode -ne 0 -or -not $helper.json.ok) {
        throw 'Failed to build TestCellBcBridgeHelper.'
    }

    $result.steps.helperDeploy = @()
    foreach ($cell in $backendCells) {
        $helperTarget = Join-Path (Join-Path $cell.serverDir 'plugins') 'TestCellBcBridgeHelper.jar'
        Copy-Item -LiteralPath $helper.json.outputJar -Destination $helperTarget -Force
        $result.steps.helperDeploy += [ordered]@{
            cellId = $cell.id
            targetPath = $helperTarget
        }
    }

    Stop-Bot -Cell $botCell
    foreach ($cell in $backendCells) {
        $startResult = Start-BackendServer -Cell $cell
        $result.steps.backendStart += [ordered]@{
            cellId = $cell.id
            started = $startResult.ready
            processId = if (Get-BackendServerProcess -Cell $cell) { (Get-BackendServerProcess -Cell $cell).ProcessId } else { $null }
            launcherPath = $startResult.launcherPath
            launcherLog = $startResult.launcherLog
        }
        if (-not $startResult.ready) {
            throw "Failed to start backend server for $($cell.id)."
        }
    }

    $bcEnsure = Invoke-ExternalJsonScript -ScriptPath (Join-Path $PSScriptRoot 'Invoke-TestCellBc.ps1') -Arguments @('-Mode', 'ensure', '-ConfigPath', $bcConfig.path)
    $result.steps.bcEnsure = $bcEnsure.json
    if ($bcEnsure.exitCode -ne 0 -or -not $bcEnsure.json.ok) {
        throw 'BC ensure failed.'
    }

    $botStarted = Start-Bot -Cell $botCell
    $result.steps.botStart = [ordered]@{
        cellId = $botCell.id
        started = $botStarted
        processId = if (Get-BotProcess -Cell $botCell) { (Get-BotProcess -Cell $botCell).ProcessId } else { $null }
    }
    if (-not $botStarted) {
        throw "Failed to start bot for $($botCell.id)."
    }

    $connect = Invoke-ModAction -Cell $botCell -Action 'connect_to_server' -Params @{
        ip = $BcConnectHost
        port = $bcConfig.listenPort
    } -TimeoutSec 45
    $result.steps.connectToBc = $connect
    if (-not $connect.ok -or -not $connect.json -or $connect.json.status -ne 'success') {
        throw "Bot failed to connect to BC."
    }

    $defaultRelay = Wait-Until -TimeoutSec 45 -Condition {
        $relay = Invoke-PluginAction -Cell $defaultBackend -Action 'query_player_state'
        if ($relay.ok -and $relay.json -and $relay.json.status -eq 'success') {
            return $relay
        }
        return $null
    }
    $result.steps.defaultBackendRelay = $defaultRelay
    $result.steps.backendRelays += [ordered]@{
        cellId = $defaultBackend.id
        relay = $defaultRelay
    }
    if ($null -eq $defaultRelay) {
        throw "Bot did not reach default backend $($defaultBackend.id) through BC."
    }

    $currentBackend = $defaultBackend
    foreach ($nextBackend in $targetBackends) {
        $crossServerCommand = Expand-CrossServerCommand -Template $CrossServerCommandTemplate -SourceCell $currentBackend -TargetCell $nextBackend -BotCell $botCell
        $cross = Invoke-PluginAction -Cell $currentBackend -Action 'chat_command' -Params @{
            command = $crossServerCommand
        } -TimeoutSec 20
        $result.steps.crossServerCommands += [ordered]@{
            fromCellId = $currentBackend.id
            toCellId = $nextBackend.id
            commandText = $crossServerCommand
            command = $cross
        }
        if ($null -eq $result.steps.crossServerCommand) {
            $result.steps.crossServerCommand = $cross
        }
        if (-not $cross.ok -or -not $cross.json -or $cross.json.status -ne 'success') {
            throw "Cross-server command failed from $($currentBackend.id) to target $($nextBackend.id)."
        }

        $targetRelay = Wait-Until -TimeoutSec 45 -Condition {
            $relay = Invoke-PluginAction -Cell $nextBackend -Action 'query_player_state'
            if ($relay.ok -and $relay.json -and $relay.json.status -eq 'success') {
                return $relay
            }
            return $null
        }
        $result.steps.backendRelays += [ordered]@{
            cellId = $nextBackend.id
            relay = $targetRelay
        }
        if ($null -eq $result.steps.targetBackendRelay) {
            $result.steps.targetBackendRelay = $targetRelay
        }
        if ($null -eq $targetRelay) {
            $screen = Invoke-ModAction -Cell $botCell -Action 'query_screen_state'
            $chat = Invoke-PluginAction -Cell $nextBackend -Action 'query_chat_history' -Params @{ count = 15 } -TimeoutSec 15
            $result.steps.targetBackendScreen = $screen
            $result.steps.targetBackendChat = $chat
            throw "Bot did not reach target backend $($nextBackend.id) through BC."
        }

        $currentBackend = $nextBackend
    }

    $result.ok = $true
}
catch {
    Add-Error $_.Exception.Message
    $result.ok = $false
}
finally {
    $result.steps.cleanup.bcStop = (Invoke-ExternalJsonScript -ScriptPath (Join-Path $PSScriptRoot 'Invoke-TestCellBc.ps1') -Arguments @('-Mode', 'stop', '-ConfigPath', $bcConfig.path)).json
    Stop-Bot -Cell $botCell

    $restore = Invoke-ExternalJsonScript -ScriptPath (Join-Path $PSScriptRoot 'Prepare-TestCellBcBackends.ps1') -Arguments @(
        '-Mode', 'restore',
        '-CellConfigPath', $cellConfig.path,
        '-Owner', $Owner
    )
    $result.steps.cleanup.restore = $restore.json
    $result.steps.cleanup.backendStop = @($backendCells | ForEach-Object { Stop-BackendServer -Cell $_ })
    $result.steps.cleanup.launcherRemoved = @()
    foreach ($cell in $backendCells) {
        $launcherPath = Join-Path $cell.serverDir "start-bc-backend-$($cell.id).cmd"
        if (Test-Path -LiteralPath $launcherPath) {
            Remove-Item -LiteralPath $launcherPath -Force
            $result.steps.cleanup.launcherRemoved += $launcherPath
        }
    }

    $result.steps.cleanup.helperRemoved = @()
    foreach ($cell in $backendCells) {
        $helperTarget = Join-Path (Join-Path $cell.serverDir 'plugins') 'TestCellBcBridgeHelper.jar'
        if (Test-Path -LiteralPath $helperTarget) {
            try {
                Remove-Item -LiteralPath $helperTarget -Force
                $result.steps.cleanup.helperRemoved += $helperTarget
            } catch {
                Add-Error ("Failed to remove helper jar: " + $_.Exception.Message)
                $result.steps.cleanup.helperRemoveError = $_.Exception.Message
                $result.ok = $false
            }
        }
    }

    $result.finishedAt = (Get-Date).ToString('s')
    $result | ConvertTo-Json -Depth 12
    if (-not $result.ok) {
        exit 1
    }
}
