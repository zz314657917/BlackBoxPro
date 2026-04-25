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
    $ConfigPath = Join-Path $PSScriptRoot 'cells-1201.json'
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

function New-RequestId {
    param([string]$Prefix)
    return "$Prefix-$($cell.id)-$([DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds())"
}

function Invoke-ModAction {
    param(
        [string]$Action,
        [hashtable]$Params = @{},
        [int]$TimeoutSec = 30
    )

    return Invoke-JsonRequest -Uri $ModExecuteUrl -Method POST -TimeoutSec $TimeoutSec -Body @{
        id = New-RequestId -Prefix $Action
        action = $Action
        params = $Params
    }
}

function Invoke-PluginAction {
    param(
        [string]$Action,
        [hashtable]$Params = @{},
        [int]$TimeoutSec = 30
    )

    return Invoke-JsonRequest -Uri $PluginExecuteUrl -Method POST -TimeoutSec $TimeoutSec -Body @{
        id = New-RequestId -Prefix "relay-$Action"
        action = $Action
        params = $Params
    }
}

function Set-ConfigLine {
    param(
        [string]$Content,
        [string]$Pattern,
        [string]$Replacement
    )

    if ($Content -match $Pattern) {
        return [regex]::Replace($Content, $Pattern, $Replacement)
    }

    if (-not $Content.EndsWith("`n")) {
        $Content += "`r`n"
    }
    return $Content + $Replacement + "`r`n"
}

function Update-ServerConfigForCell {
    $configPath = Join-Path $cell.serverDir 'plugins/BlackBoxPro/config.yml'
    if (Test-Path -LiteralPath $configPath) {
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

    $serverPropertiesPath = Join-Path $cell.serverDir 'server.properties'
    if (Test-Path -LiteralPath $serverPropertiesPath) {
        $serverContent = Get-Content -Raw -LiteralPath $serverPropertiesPath
        $serverContent = Set-ConfigLine -Content $serverContent -Pattern '(?m)^server-port=.*$' -Replacement "server-port=$($cell.serverPort)"
        $serverContent = Set-ConfigLine -Content $serverContent -Pattern '(?m)^online-mode=.*$' -Replacement 'online-mode=false'
        $serverContent = Set-ConfigLine -Content $serverContent -Pattern '(?m)^enforce-secure-profile=.*$' -Replacement 'enforce-secure-profile=false'
        $serverContent = Set-ConfigLine -Content $serverContent -Pattern '(?m)^difficulty=.*$' -Replacement 'difficulty=peaceful'
        [System.IO.File]::WriteAllText($serverPropertiesPath, $serverContent, [System.Text.UTF8Encoding]::new($false))
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
            $_.CommandLine -match [regex]::Escape($cell.serverDir)
        } |
        Select-Object -First 1
}

function Get-ServerControllerProcesses {
    @(Get-CimInstance Win32_Process |
        Where-Object {
            $_.Name -in @('cmd.exe', 'powershell.exe') -and
            $_.CommandLine -match [regex]::Escape($cell.serverDir)
        })
}

function Get-BotProcess {
    $portOwner = @(Get-ProcessesByIds -ProcessIds (Get-ListeningProcessIds -Ports @($cell.modHttpPort))) |
        Where-Object { $_.Name -in @('javaw.exe', 'java.exe') } |
        Select-Object -First 1

    if ($null -ne $portOwner) {
        return $portOwner
    }

    return Get-CimInstance Win32_Process |
        Where-Object {
            $_.Name -in @('javaw.exe', 'java.exe') -and
            $_.CommandLine -match [regex]::Escape($cell.botVersionDir)
        } |
        Select-Object -First 1
}

function Stop-CellProcesses {
    $stopped = [ordered]@{
        serverControllerPids = @()
        serverJavaPids = @()
        botJavaPids = @()
        freedPorts = @{}
    }

    foreach ($proc in @(Get-ServerControllerProcesses)) {
        Stop-Process -Id $proc.ProcessId -Force -ErrorAction SilentlyContinue
        $stopped.serverControllerPids += $proc.ProcessId
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
            $_.CommandLine -match [regex]::Escape($cell.serverDir)
        })) {
        $serverJavaIds.Add([int]$proc.ProcessId) | Out-Null
    }
    foreach ($proc in @(Get-ProcessesByIds -ProcessIds @($serverJavaIds))) {
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
            $_.Name -in @('javaw.exe', 'java.exe') -and
            $_.CommandLine -match [regex]::Escape($cell.botVersionDir)
        })) {
        $botJavaIds.Add([int]$proc.ProcessId) | Out-Null
    }
    foreach ($proc in @(Get-ProcessesByIds -ProcessIds @($botJavaIds))) {
        Stop-Process -Id $proc.ProcessId -Force -ErrorAction SilentlyContinue
        $stopped.botJavaPids += $proc.ProcessId
    }

    Start-Sleep -Seconds 3
    $stopped.freedPorts = [ordered]@{
        serverPort = (-not (Get-ListeningPort -Port $cell.serverPort))
        pluginHttpPort = (-not (Get-ListeningPort -Port $cell.pluginHttpPort))
        modHttpPort = (-not (Get-ListeningPort -Port $cell.modHttpPort))
    }
    return [pscustomobject]$stopped
}

function Stop-BotProcesses {
    $stopped = [ordered]@{
        botJavaPids = @()
        freedPorts = @{}
    }

    $botJavaIds = New-Object System.Collections.Generic.HashSet[int]
    foreach ($proc in @(Get-ProcessesByIds -ProcessIds (Get-ListeningProcessIds -Ports @($cell.modHttpPort)))) {
        if ($proc.Name -in @('javaw.exe', 'java.exe')) {
            $botJavaIds.Add([int]$proc.ProcessId) | Out-Null
        }
    }
    foreach ($proc in @(Get-CimInstance Win32_Process |
        Where-Object {
            $_.Name -in @('javaw.exe', 'java.exe') -and
            $_.CommandLine -match [regex]::Escape($cell.botVersionDir)
        })) {
        $botJavaIds.Add([int]$proc.ProcessId) | Out-Null
    }
    foreach ($proc in @(Get-ProcessesByIds -ProcessIds @($botJavaIds))) {
        Stop-Process -Id $proc.ProcessId -Force -ErrorAction SilentlyContinue
        $stopped.botJavaPids += $proc.ProcessId
    }

    Start-Sleep -Seconds 2
    $stopped.freedPorts = [ordered]@{
        modHttpPort = (-not (Get-ListeningPort -Port $cell.modHttpPort))
    }
    return [pscustomobject]$stopped
}

function Get-OfflineUuid {
    param([string]$Username)

    $md5 = [System.Security.Cryptography.MD5]::Create()
    try {
        $hash = $md5.ComputeHash([System.Text.Encoding]::UTF8.GetBytes("OfflinePlayer:$Username"))
    } finally {
        $md5.Dispose()
    }

    $hash[6] = ($hash[6] -band 0x0F) -bor 0x30
    $hash[8] = ($hash[8] -band 0x3F) -bor 0x80

    return (
        ('{0:x2}{1:x2}{2:x2}{3:x2}' -f $hash[0], $hash[1], $hash[2], $hash[3]) + '-' +
        ('{0:x2}{1:x2}' -f $hash[4], $hash[5]) + '-' +
        ('{0:x2}{1:x2}' -f $hash[6], $hash[7]) + '-' +
        ('{0:x2}{1:x2}' -f $hash[8], $hash[9]) + '-' +
        ('{0:x2}{1:x2}{2:x2}{3:x2}{4:x2}{5:x2}' -f $hash[10], $hash[11], $hash[12], $hash[13], $hash[14], $hash[15])
    )
}

function Test-RuleSet {
    param(
        [object[]]$Rules,
        [hashtable]$Features
    )

    $normalizedRules = @($Rules | Where-Object { $null -ne $_ })
    if ($normalizedRules.Count -eq 0) {
        return $true
    }

    $allowed = $false
    foreach ($rule in $normalizedRules) {
        $matches = $true
        if ($rule.os) {
            if ($rule.os.name -and $rule.os.name -ne 'windows') {
                $matches = $false
            }
            if ($matches -and $rule.os.arch) {
                $wantedArch = [string]$rule.os.arch
                $currentArch = [System.Runtime.InteropServices.RuntimeInformation]::OSArchitecture.ToString().ToLowerInvariant()
                if ($wantedArch -eq 'x86') {
                    $matches = $currentArch -eq 'x86'
                } elseif ($wantedArch -in @('x64', 'amd64')) {
                    $matches = $currentArch -in @('x64', 'amd64')
                }
            }
        }
        if ($matches -and $rule.features) {
            foreach ($property in $rule.features.PSObject.Properties) {
                $expected = [bool]$property.Value
                $actual = if ($Features.ContainsKey($property.Name)) { [bool]$Features[$property.Name] } else { $false }
                if ($actual -ne $expected) {
                    $matches = $false
                    break
                }
            }
        }
        if ($matches) {
            $allowed = ($rule.action -eq 'allow')
        }
    }

    return $allowed
}

function Expand-Template {
    param(
        [string]$Value,
        [hashtable]$Variables
    )

    return [regex]::Replace($Value, '\$\{([^}]+)\}', {
            param($match)
            $key = $match.Groups[1].Value
            if ($Variables.ContainsKey($key)) {
                return [string]$Variables[$key]
            }
            return $match.Value
        })
}

function Resolve-ArgumentList {
    param(
        [object[]]$Items,
        [hashtable]$Variables,
        [hashtable]$Features
    )

    $resolved = New-Object System.Collections.Generic.List[string]
    foreach ($item in @($Items)) {
        if ($item -is [string]) {
            $resolved.Add((Expand-Template -Value $item -Variables $Variables)) | Out-Null
            continue
        }

        if (-not (Test-RuleSet -Rules @($item.rules) -Features $Features)) {
            continue
        }

        if ($item.value -is [System.Array]) {
            foreach ($valueItem in $item.value) {
                $resolved.Add((Expand-Template -Value ([string]$valueItem) -Variables $Variables)) | Out-Null
            }
        } else {
            $resolved.Add((Expand-Template -Value ([string]$item.value) -Variables $Variables)) | Out-Null
        }
    }
    return $resolved.ToArray()
}

function Get-BotVersionJson {
    $versionJsonPath = Join-Path $cell.botVersionDir "$($cell.botVersionName).json"
    if (-not (Test-Path -LiteralPath $versionJsonPath)) {
        throw "Bot version json not found: $versionJsonPath"
    }

    return Get-Content -Raw -Encoding UTF8 -LiteralPath $versionJsonPath | ConvertFrom-Json
}

function Get-BotClasspath {
    $versionJson = Get-BotVersionJson
    $entries = New-Object System.Collections.Generic.List[string]
    $features = @{
        has_custom_resolution = $true
        is_demo_user = $false
        has_quick_plays_support = $false
        is_quick_play_singleplayer = $false
        is_quick_play_multiplayer = $false
        is_quick_play_realms = $false
    }

    foreach ($lib in @($versionJson.libraries)) {
        if (-not (Test-RuleSet -Rules @($lib.rules) -Features $features)) {
            continue
        }
        $artifactPath = $null
        if (($lib.PSObject.Properties.Name -contains 'downloads') -and $lib.downloads -and ($lib.downloads.PSObject.Properties.Name -contains 'artifact')) {
            $artifact = $lib.downloads.artifact
            if ($null -ne $artifact -and ($artifact.PSObject.Properties.Name -contains 'path') -and -not [string]::IsNullOrWhiteSpace($artifact.path)) {
                $artifactPath = $artifact.path
            }
        }
        if ($artifactPath) {
            $full = Join-Path $cell.librariesDir $artifactPath
            if ($full -match '-natives-[^\\/]+\.jar$') {
                continue
            }
            if ((Test-Path -LiteralPath $full) -and -not $entries.Contains($full)) {
                $entries.Add($full) | Out-Null
            }
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

function Resolve-JavaExecutablePath {
    param([string]$ConfiguredPath)

    if (Test-Path -LiteralPath $ConfiguredPath) {
        return $ConfiguredPath
    }

    $leaf = Split-Path -Leaf $ConfiguredPath
    $binDir = Split-Path -Parent $ConfiguredPath
    $jreRoot = Split-Path -Parent $binDir
    if (-not (Test-Path -LiteralPath $jreRoot)) {
        throw "Java executable not found: $ConfiguredPath"
    }

    $candidate = Get-ChildItem -LiteralPath $jreRoot -Directory -ErrorAction SilentlyContinue |
        ForEach-Object { Join-Path $_.FullName "bin/$leaf" } |
        Where-Object { Test-Path -LiteralPath $_ } |
        Select-Object -First 1

    if ($candidate) {
        return $candidate
    }

    throw "Java executable not found: $ConfiguredPath"
}

function Start-Server {
    Update-ServerConfigForCell

    $runBat = Join-Path $cell.serverDir 'run.bat'
    if (-not (Test-Path -LiteralPath $runBat)) {
        throw "Server launch script not found: $runBat"
    }

    $serverMinMemoryMb = if ($null -ne $cell.serverMinMemoryMb) { [int]$cell.serverMinMemoryMb } else { 2048 }
    $serverMaxMemoryMb = if ($null -ne $cell.serverMaxMemoryMb) { [int]$cell.serverMaxMemoryMb } else { 4096 }
    $inner = 'set "ARCLIGHT_XMS=' + $serverMinMemoryMb + 'M" & set "ARCLIGHT_XMX=' + $serverMaxMemoryMb + 'M" & call "' + $runBat + '" --nopause'
    $arg = '/c start "BlackBoxPro-' + $cell.id + '" /min cmd /v:on /c "' + $inner + '"'
    Start-Process -FilePath 'cmd.exe' -ArgumentList $arg -WindowStyle Hidden | Out-Null
    return (Wait-ForPorts -Ports @($cell.serverPort, $cell.pluginHttpPort) -TimeoutSec 180)
}

function Start-Bot {
    $versionJson = Get-BotVersionJson
    $botMinMemoryMb = if ($null -ne $cell.botMinMemoryMb) { [int]$cell.botMinMemoryMb } else { 1024 }
    $botMaxMemoryMb = if ($null -ne $cell.botMaxMemoryMb) { [int]$cell.botMaxMemoryMb } else { 2048 }
    $nativesDir = Join-Path $cell.botVersionDir "$($cell.botVersionName)-natives"
    if (-not (Test-Path -LiteralPath $nativesDir)) {
        throw "Bot natives dir not found: $nativesDir"
    }

    $offlineUuid = Get-OfflineUuid -Username $cell.botPlayer
    $classpath = Get-BotClasspath
    $assetIndexName = if ($versionJson.assetIndex -and $versionJson.assetIndex.id) {
        [string]$versionJson.assetIndex.id
    } else {
        [string]$versionJson.assets
    }

    $variables = @{
        auth_player_name = $cell.botPlayer
        version_name = $cell.botVersionName
        game_directory = $cell.botVersionDir
        assets_root = $cell.assetsDir
        assets_index_name = $assetIndexName
        auth_uuid = $offlineUuid
        auth_access_token = $offlineUuid.Replace('-', '')
        clientid = '00000000-0000-0000-0000-000000000000'
        auth_xuid = '0'
        user_type = 'msa'
        version_type = 'Forge'
        resolution_width = '854'
        resolution_height = '480'
        natives_directory = $nativesDir
        classpath = $classpath
        classpath_separator = ';'
        library_directory = $cell.librariesDir
        launcher_name = 'BlackBoxProTestCells'
        launcher_version = '1.0'
        quickPlayPath = ''
        quickPlaySingleplayer = ''
        quickPlayMultiplayer = ''
        quickPlayRealms = ''
    }
    $features = @{
        has_custom_resolution = $true
        is_demo_user = $false
        has_quick_plays_support = $false
        is_quick_play_singleplayer = $false
        is_quick_play_multiplayer = $false
        is_quick_play_realms = $false
    }

    $jvmArgs = Resolve-ArgumentList -Items @($versionJson.arguments.jvm) -Variables $variables -Features $features
    $gameArgs = Resolve-ArgumentList -Items @($versionJson.arguments.game) -Variables $variables -Features $features

    $args = @(
        "-Xms$($botMinMemoryMb)m",
        "-Xmx$($botMaxMemoryMb)m"
    ) + @($jvmArgs) + @(
        "-Dblackboxpro.httpPort=$($cell.modHttpPort)",
        [string]$versionJson.mainClass
    ) + @($gameArgs)

    $botJava = Resolve-JavaExecutablePath -ConfiguredPath $cell.botJava
    Start-Process -FilePath $botJava -ArgumentList $args -WorkingDirectory $cell.botVersionDir | Out-Null
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
            $null = Stop-CellProcesses
            $started = Start-Server
            if (-not $started) {
                Add-Error "Failed to start server for $($cell.id)."
            }
        }
    }

    $status = Invoke-JsonRequest -Uri $PluginStatusUrl -TimeoutSec 10
    $result.steps.server = [ordered]@{
        processId = if (Get-ServerProcess) { (Get-ServerProcess).ProcessId } else { $null }
        started = $started
        pluginStatus = $status
    }
}

function Ensure-BotProcess {
    $proc = Get-BotProcess
    $modPort = Get-ListeningPort -Port $cell.modHttpPort
    $started = $false

    if (-not $proc -or -not $modPort) {
        if ($NoAutoStartBot) {
            Add-Error "Bot for $($cell.id) is not ready and -NoAutoStartBot was set."
        } else {
            $null = Stop-BotProcesses
            $started = Start-Bot
            if (-not $started) {
                Add-Error "Failed to start bot for $($cell.id)."
            }
        }
    }

    $status = Invoke-JsonRequest -Uri $ModStatusUrl -TimeoutSec 10
    $result.steps.bot = [ordered]@{
        processId = if (Get-BotProcess) { (Get-BotProcess).ProcessId } else { $null }
        started = $started
        modStatus = $status
    }
}

function Wait-ForPlayerReady {
    param([int]$TimeoutSec = 120)

    $deadline = (Get-Date).AddSeconds($TimeoutSec)
    $lastConnectAt = Get-Date '2000-01-01'
    $lastScreen = $null
    $lastPlayer = $null

    while ((Get-Date) -lt $deadline) {
        $player = Invoke-ModAction -Action 'query_player_state' -TimeoutSec 20
        $lastPlayer = $player
        if ($player.ok -and $player.json -and $player.json.status -eq 'success') {
            return [pscustomobject]@{
                ok = $true
                player = $player
                screen = $lastScreen
            }
        }

        $screen = Invoke-ModAction -Action 'query_screen_state' -TimeoutSec 20
        $lastScreen = $screen

        $reason = ''
        if ($screen.ok -and $screen.json -and $screen.json.data) {
            $reason = [string]$screen.json.data.reason
        }

        if ($screen.ok -and $screen.json -and $screen.json.data -and $screen.json.data.screenType -eq 'disconnected') {
            $null = Invoke-ModAction -Action 'close_screen' -TimeoutSec 10
            Start-Sleep -Milliseconds 800
            $null = Invoke-ModAction -Action 'connect_to_server' -Params @{
                ip = 'localhost'
                port = $cell.serverPort
            } -TimeoutSec 30
            $lastConnectAt = Get-Date
            if ($reason -match 'Server is still starting') {
                Start-Sleep -Seconds 4
            } else {
                Start-Sleep -Seconds 2
            }
            continue
        }

        if (((Get-Date) - $lastConnectAt).TotalSeconds -ge 5) {
            $null = Invoke-ModAction -Action 'connect_to_server' -Params @{
                ip = 'localhost'
                port = $cell.serverPort
            } -TimeoutSec 30
            $lastConnectAt = Get-Date
        }

        Start-Sleep -Seconds 3
    }

    return [pscustomobject]@{
        ok = $false
        player = $lastPlayer
        screen = $lastScreen
    }
}

function Ensure-BotConnected {
    $connection = Wait-ForPlayerReady -TimeoutSec 150
    $result.steps.bot.connection = $connection
    if (-not $connection.ok) {
        Add-Error "Bot failed to connect to server for $($cell.id)."
    }
}

switch ($Mode) {
    'status' {
        $result.steps.server = [ordered]@{
            processId = if (Get-ServerProcess) { (Get-ServerProcess).ProcessId } else { $null }
            cmdPids = @((Get-ServerControllerProcesses | Select-Object -ExpandProperty ProcessId))
            ports = [ordered]@{
                serverPort = [bool](Get-ListeningPort -Port $cell.serverPort)
                pluginHttpPort = [bool](Get-ListeningPort -Port $cell.pluginHttpPort)
            }
            pluginStatus = Invoke-JsonRequest -Uri $PluginStatusUrl -TimeoutSec 5
        }
        $result.steps.bot = [ordered]@{
            processId = if (Get-BotProcess) { (Get-BotProcess).ProcessId } else { $null }
            ports = [ordered]@{
                modHttpPort = [bool](Get-ListeningPort -Port $cell.modHttpPort)
            }
            modStatus = Invoke-JsonRequest -Uri $ModStatusUrl -TimeoutSec 5
        }
    }
    'ensure' {
        Ensure-Server
        if ($result.errors.Count -eq 0) {
            Ensure-BotProcess
        }
        if ($result.errors.Count -eq 0) {
            Ensure-BotConnected
        }
    }
    'smoke' {
        Ensure-Server
        if ($result.errors.Count -eq 0) {
            Ensure-BotProcess
        }
        if ($result.errors.Count -eq 0) {
            Ensure-BotConnected
        }
        if ($result.errors.Count -eq 0) {
            $screen = Invoke-ModAction -Action 'query_screen_state' -TimeoutSec 20
            $player = Invoke-ModAction -Action 'query_player_state' -TimeoutSec 20
            $relay = Invoke-PluginAction -Action 'query_player_state' -TimeoutSec 30
            $screenshot = Invoke-ModAction -Action 'screenshot' -Params @{
                testId = 'smoke'
                prefix = 'world'
            } -TimeoutSec 30

            $result.steps.relay = $relay
            $result.steps.test = [ordered]@{
                directScreen = $screen
                directPlayer = $player
                relayPlayer = $relay
                screenshot = $screenshot
            }

            foreach ($entry in @($screen, $player, $relay, $screenshot)) {
                if (-not $entry.ok -or -not $entry.json -or $entry.json.status -ne 'success') {
                    Add-Error "Smoke check failed for $($cell.id)."
                    break
                }
            }
        }
    }
    'stop' {
        $stopped = Stop-CellProcesses
        $result.steps.server.stop = $stopped
        if (-not ($stopped.freedPorts.serverPort -and $stopped.freedPorts.pluginHttpPort -and $stopped.freedPorts.modHttpPort)) {
            Add-Error "Not all ports were freed for $($cell.id)."
        }
    }
}

$result.ok = ($result.errors.Count -eq 0)
$result.finishedAt = (Get-Date).ToString('s')
[pscustomobject]$result | ConvertTo-Json -Depth 20

if (-not $result.ok) {
    exit 1
}
