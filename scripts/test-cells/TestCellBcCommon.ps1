$ErrorActionPreference = 'Stop'

. (Join-Path $PSScriptRoot 'TestCellCommon.ps1')

function Get-DefaultTestCellBcConfigPath {
    return Join-Path $PSScriptRoot 'cells-bc.json'
}

function Resolve-TestCellBcPath {
    param(
        [string]$BaseDir,
        [string]$Path
    )

    if ([string]::IsNullOrWhiteSpace($Path)) {
        return $null
    }

    if ([System.IO.Path]::IsPathRooted($Path)) {
        return [System.IO.Path]::GetFullPath($Path)
    }

    return [System.IO.Path]::GetFullPath((Join-Path $BaseDir $Path))
}

function ConvertTo-TestCellBcBool {
    param(
        [object]$Value,
        [bool]$DefaultValue
    )

    if ($null -eq $Value) {
        return $DefaultValue
    }

    return [bool]$Value
}

function Load-TestCellBcConfig {
    param([string]$ConfigPath = (Get-DefaultTestCellBcConfigPath))

    if (-not (Test-Path -LiteralPath $ConfigPath)) {
        throw "BC config not found: $ConfigPath"
    }

    $resolvedPath = (Resolve-Path -LiteralPath $ConfigPath).ProviderPath
    $baseDir = Split-Path -Parent $resolvedPath
    $raw = Get-Content -Raw -Encoding UTF8 -LiteralPath $resolvedPath | ConvertFrom-Json

    $sourceConfigs = @()
    if ($raw.PSObject.Properties.Name -contains 'sourceConfigs' -and $null -ne $raw.sourceConfigs) {
        $sourceConfigs = @($raw.sourceConfigs)
    } else {
        $sourceConfigs = @('cells.json', 'cells-1201.json')
    }

    return [pscustomobject]@{
        path = $resolvedPath
        baseDir = $baseDir
        version = if ($raw.version) { [int]$raw.version } else { 1 }
        serverDir = Resolve-TestCellBcPath -BaseDir $baseDir -Path $raw.serverDir
        serverJar = if ([string]::IsNullOrWhiteSpace($raw.serverJar)) { 'Waterfall.jar' } else { [string]$raw.serverJar }
        javaPath = Resolve-TestCellBcPath -BaseDir $baseDir -Path $raw.javaPath
        minMemoryMb = if ($raw.minMemoryMb) { [int]$raw.minMemoryMb } else { 512 }
        maxMemoryMb = if ($raw.maxMemoryMb) { [int]$raw.maxMemoryMb } else { 1024 }
        listenHost = if ([string]::IsNullOrWhiteSpace($raw.listenHost)) { '127.0.0.1' } else { [string]$raw.listenHost }
        listenPort = if ($raw.listenPort) { [int]$raw.listenPort } else { 25645 }
        onlineMode = ConvertTo-TestCellBcBool -Value $raw.onlineMode -DefaultValue $false
        ipForward = ConvertTo-TestCellBcBool -Value $raw.ipForward -DefaultValue $true
        forgeSupport = ConvertTo-TestCellBcBool -Value $raw.forgeSupport -DefaultValue $false
        motd = if ([string]::IsNullOrWhiteSpace($raw.motd)) { '&6BlackBoxPro BC' } else { [string]$raw.motd }
        maxPlayers = if ($raw.maxPlayers) { [int]$raw.maxPlayers } else { 20 }
        defaultBackend = if ([string]::IsNullOrWhiteSpace($raw.defaultBackend)) { $null } else { [string]$raw.defaultBackend }
        sourceConfigs = @($sourceConfigs | ForEach-Object { Resolve-TestCellBcPath -BaseDir $baseDir -Path $_ })
    }
}

function Read-TestCellBcProperties {
    param([string]$Path)

    if (-not (Test-Path -LiteralPath $Path)) {
        return $null
    }

    $map = @{}
    foreach ($line in Get-Content -LiteralPath $Path -Encoding UTF8) {
        $trimmed = $line.Trim()
        if ([string]::IsNullOrWhiteSpace($trimmed)) {
            continue
        }
        if ($trimmed.StartsWith('#')) {
            continue
        }

        $separatorIndex = $trimmed.IndexOf('=')
        if ($separatorIndex -lt 0) {
            continue
        }

        $key = $trimmed.Substring(0, $separatorIndex).Trim()
        $value = $trimmed.Substring($separatorIndex + 1).Trim()
        $map[$key] = $value
    }

    return $map
}

function Resolve-TestCellBcBackendHost {
    param([string]$ServerIp)

    if ([string]::IsNullOrWhiteSpace($ServerIp)) {
        return '127.0.0.1'
    }

    switch ($ServerIp.Trim()) {
        '0.0.0.0' { return '127.0.0.1' }
        '::' { return '127.0.0.1' }
        '*' { return '127.0.0.1' }
        default { return $ServerIp.Trim() }
    }
}

function Get-TestCellBcBackends {
    param(
        [string[]]$SourceConfigPaths,
        [string]$DefaultBackendId,
        [string[]]$IncludeCellIds = @()
    )

    $discovered = New-Object System.Collections.Generic.List[object]
    $skipped = New-Object System.Collections.Generic.List[object]
    $seenIds = New-Object 'System.Collections.Generic.HashSet[string]'
    $includeSet = $null
    if ($IncludeCellIds -and @($IncludeCellIds).Count -gt 0) {
        $includeSet = New-Object 'System.Collections.Generic.HashSet[string]' ([System.StringComparer]::OrdinalIgnoreCase)
        foreach ($includeId in @($IncludeCellIds | Where-Object { -not [string]::IsNullOrWhiteSpace($_) })) {
            $includeSet.Add([string]$includeId) | Out-Null
        }
    }

    foreach ($sourceConfigPath in @($SourceConfigPaths | Where-Object { -not [string]::IsNullOrWhiteSpace($_) })) {
        if (-not (Test-Path -LiteralPath $sourceConfigPath)) {
            $skipped.Add([pscustomobject]@{
                sourceConfigPath = $sourceConfigPath
                id = $null
                reason = 'missing-source-config'
            }) | Out-Null
            continue
        }

        try {
            $config = Load-TestCellConfig -ConfigPath $sourceConfigPath
        } catch {
            $skipped.Add([pscustomobject]@{
                sourceConfigPath = $sourceConfigPath
                id = $null
                reason = 'invalid-source-config'
                detail = $_.Exception.Message
            }) | Out-Null
            continue
        }

        foreach ($cell in @($config.cells)) {
            if (-not $cell.enabled) {
                continue
            }
            if ($null -ne $includeSet -and -not $includeSet.Contains([string]$cell.id)) {
                continue
            }

            if (-not $seenIds.Add([string]$cell.id)) {
                $skipped.Add([pscustomobject]@{
                    sourceConfigPath = $sourceConfigPath
                    id = $cell.id
                    reason = 'duplicate-cell-id'
                }) | Out-Null
                continue
            }

            if ([string]::IsNullOrWhiteSpace($cell.serverDir) -or -not $cell.serverPort) {
                $skipped.Add([pscustomobject]@{
                    sourceConfigPath = $sourceConfigPath
                    id = $cell.id
                    reason = 'invalid-cell-config'
                }) | Out-Null
                continue
            }

            $serverPropertiesPath = Join-Path $cell.serverDir 'server.properties'
            if (-not (Test-Path -LiteralPath $serverPropertiesPath)) {
                $skipped.Add([pscustomobject]@{
                    sourceConfigPath = $sourceConfigPath
                    id = $cell.id
                    reason = 'missing-server-properties'
                    serverDir = $cell.serverDir
                }) | Out-Null
                continue
            }

            $properties = Read-TestCellBcProperties -Path $serverPropertiesPath
            $backendHost = Resolve-TestCellBcBackendHost -ServerIp $properties['server-ip']
            $address = '{0}:{1}' -f $backendHost, [int]$cell.serverPort

            $discovered.Add([pscustomobject]@{
                id = [string]$cell.id
                label = [string]$cell.label
                host = $backendHost
                port = [int]$cell.serverPort
                address = $address
                sourceConfigPath = $config.path
                serverDir = $cell.serverDir
                serverPropertiesPath = $serverPropertiesPath
            }) | Out-Null
        }
    }

    $resolvedDefault = $null
    if (-not [string]::IsNullOrWhiteSpace($DefaultBackendId)) {
        $explicit = @($discovered | Where-Object { $_.id -eq $DefaultBackendId } | Select-Object -First 1)
        if ($explicit.Count -gt 0) {
            $resolvedDefault = $explicit[0].id
        }
    }
    if ($null -eq $resolvedDefault -and $discovered.Count -gt 0) {
        $resolvedDefault = $discovered[0].id
    }

    return [pscustomobject]@{
        backends = @($discovered.ToArray())
        skipped = @($skipped.ToArray())
        defaultBackendId = $resolvedDefault
    }
}

function ConvertTo-TestCellBcYamlQuoted {
    param([string]$Value)

    if ($null -eq $Value) {
        return "''"
    }

    return "'" + ($Value -replace "'", "''") + "'"
}

function Set-TestCellBcSpigotBungeecordContent {
    param(
        [string]$Content,
        [bool]$Enabled
    )

    $replacement = 'bungeecord: ' + $Enabled.ToString().ToLowerInvariant()
    if ($Content -match '(?m)^(\s*)bungeecord:\s*(true|false)\s*$') {
        return [regex]::Replace($Content, '(?m)^(\s*)bungeecord:\s*(true|false)\s*$', ('$1' + $replacement))
    }

    if ($Content -match '(?m)^settings:\s*$') {
        return [regex]::Replace($Content, '(?m)^settings:\s*$', ('$0' + "`r`n  " + $replacement))
    }

    if (-not $Content.EndsWith("`n")) {
        $Content += "`r`n"
    }
    return $Content + 'settings:' + "`r`n  " + $replacement + "`r`n"
}

function Set-TestCellBcConfigLine {
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

function Set-TestCellBcPluginConfigContent {
    param(
        [string]$Content,
        [int]$PluginHttpPort,
        [int]$SharedBotModHttpPort
    )

    if ($null -eq $Content) {
        $Content = ''
    }

    $updated = $Content
    $updated = Set-TestCellBcConfigLine -Content $updated -Pattern '(?m)^debug:.*$' -Replacement 'debug: false'
    $updated = Set-TestCellBcConfigLine -Content $updated -Pattern '(?m)^response-timeout-ms:.*$' -Replacement 'response-timeout-ms: 10000'
    $updated = Set-TestCellBcConfigLine -Content $updated -Pattern '(?m)^http-port:.*$' -Replacement "http-port: $PluginHttpPort"
    $updated = Set-TestCellBcConfigLine -Content $updated -Pattern '(?m)^test-mode:.*$' -Replacement 'test-mode: dual'
    $updated = Set-TestCellBcConfigLine -Content $updated -Pattern '(?m)^mod-http-address:.*$' -Replacement "mod-http-address: `"http://localhost:$SharedBotModHttpPort`""
    return $updated
}

function Get-TestCellBcPrepStateRoot {
    param([string]$CellConfigPath = (Get-DefaultTestCellConfigPath))

    $root = Join-Path (Split-Path -Parent $CellConfigPath) 'locks/bc-backend-prep'
    if (-not (Test-Path -LiteralPath $root)) {
        New-Item -ItemType Directory -Path $root -Force | Out-Null
    }
    return (Resolve-Path -LiteralPath $root).ProviderPath
}

function ConvertTo-TestCellBcOwnerFileName {
    param([string]$Owner)

    if ([string]::IsNullOrWhiteSpace($Owner)) {
        throw 'Owner cannot be empty.'
    }

    return (($Owner -replace '[^A-Za-z0-9._-]', '_') + '.json')
}

function Get-TestCellBcPrepStatePath {
    param(
        [string]$Owner,
        [string]$CellConfigPath = (Get-DefaultTestCellConfigPath))

    return Join-Path (Get-TestCellBcPrepStateRoot -CellConfigPath $CellConfigPath) (ConvertTo-TestCellBcOwnerFileName -Owner $Owner)
}

function Write-TestCellBcPrepState {
    param(
        [string]$Owner,
        [object]$State,
        [string]$CellConfigPath = (Get-DefaultTestCellConfigPath))

    $path = Get-TestCellBcPrepStatePath -Owner $Owner -CellConfigPath $CellConfigPath
    [System.IO.File]::WriteAllText($path, ($State | ConvertTo-Json -Depth 20), [System.Text.UTF8Encoding]::new($false))
    return $path
}

function Read-TestCellBcPrepState {
    param(
        [string]$Owner,
        [string]$CellConfigPath = (Get-DefaultTestCellConfigPath))

    $path = Get-TestCellBcPrepStatePath -Owner $Owner -CellConfigPath $CellConfigPath
    if (-not (Test-Path -LiteralPath $path)) {
        return $null
    }

    return Get-Content -Raw -Encoding UTF8 -LiteralPath $path | ConvertFrom-Json
}

function Remove-TestCellBcPrepState {
    param(
        [string]$Owner,
        [string]$CellConfigPath = (Get-DefaultTestCellConfigPath))

    $path = Get-TestCellBcPrepStatePath -Owner $Owner -CellConfigPath $CellConfigPath
    if (Test-Path -LiteralPath $path) {
        Remove-Item -LiteralPath $path -Force
    }
}

function Get-TestCellBcUniqueCellIds {
    param([string[]]$CellIds)

    $seen = New-Object 'System.Collections.Generic.HashSet[string]' ([System.StringComparer]::OrdinalIgnoreCase)
    $ordered = New-Object System.Collections.Generic.List[string]
    foreach ($cellId in @($CellIds | Where-Object { -not [string]::IsNullOrWhiteSpace($_) })) {
        if ($seen.Add([string]$cellId)) {
            $ordered.Add([string]$cellId) | Out-Null
        }
    }
    return @($ordered.ToArray())
}

function Get-TestCellBcLeaseAvailability {
    param(
        [object]$Config,
        [string[]]$CellIds
    )

    $results = New-Object System.Collections.Generic.List[object]
    foreach ($cellId in (Get-TestCellBcUniqueCellIds -CellIds $CellIds)) {
        $cell = Get-TestCell -Config $Config -CellId $cellId
        $lease = Read-TestCellLease -CellId $cell.id -ConfigPath $Config.path
        if (Test-TestCellLeaseExpired -Lease $lease) {
            Remove-TestCellLock -CellId $cell.id -ConfigPath $Config.path
            $lease = $null
        }

        $results.Add([pscustomobject]@{
            cellId = $cell.id
            cell = $cell
            lease = $lease
            available = ($null -eq $lease)
        }) | Out-Null
    }

    return @($results.ToArray())
}

function Assert-TestCellBcLeasesAvailable {
    param(
        [object]$Config,
        [string[]]$CellIds
    )

    $availability = @(Get-TestCellBcLeaseAvailability -Config $Config -CellIds $CellIds)
    $blocked = @($availability | Where-Object { -not $_.available })
    if ($blocked.Count -gt 0) {
        $summary = @($blocked | ForEach-Object {
            '{0}(owner={1})' -f $_.cellId, $_.lease.owner
        }) -join ', '
        throw "Requested test cells already leased: $summary"
    }

    return $availability
}

function Acquire-TestCellBcLeases {
    param(
        [object]$Config,
        [string[]]$CellIds,
        [string]$Owner,
        [int]$LeaseMinutes = 180
    )

    $availability = @(Assert-TestCellBcLeasesAvailable -Config $Config -CellIds $CellIds)
    $acquired = New-Object System.Collections.Generic.List[object]
    $currentCellId = $null
    $currentLockCreated = $false

    try {
        foreach ($entry in $availability) {
            $currentCellId = $entry.cellId
            $currentLockCreated = $false

            $lockPath = Get-TestCellLockPath -CellId $entry.cell.id -ConfigPath $Config.path
            New-Item -ItemType Directory -Path $lockPath -ErrorAction Stop | Out-Null
            $currentLockCreated = $true

            $lease = [pscustomobject]@{
                cellId = $entry.cell.id
                owner = $Owner
                acquiredAt = [DateTimeOffset]::UtcNow.ToString('o')
                expiresAt = [DateTimeOffset]::UtcNow.AddMinutes($LeaseMinutes).ToString('o')
                processId = $PID
                host = $env:COMPUTERNAME
            }
            Write-TestCellLease -CellId $entry.cell.id -Lease $lease -ConfigPath $Config.path

            $acquired.Add([pscustomobject]@{
                cellId = $entry.cell.id
                cell = $entry.cell
                lockPath = $lockPath
                lease = $lease
            }) | Out-Null

            $currentCellId = $null
            $currentLockCreated = $false
        }
    }
    catch {
        if ($currentLockCreated -and -not [string]::IsNullOrWhiteSpace($currentCellId)) {
            try {
                Remove-TestCellLock -CellId $currentCellId -ConfigPath $Config.path
            } catch {
            }
        }
        foreach ($entry in @($acquired.ToArray())) {
            try {
                $null = Release-TestCellLease -Config $Config -CellId $entry.cellId -Owner $Owner
            } catch {
            }
        }
        throw
    }

    return @($acquired.ToArray())
}

function Release-TestCellBcLeases {
    param(
        [object]$Config,
        [string[]]$CellIds,
        [string]$Owner,
        [switch]$Force
    )

    $results = New-Object System.Collections.Generic.List[object]
    foreach ($cellId in (Get-TestCellBcUniqueCellIds -CellIds $CellIds)) {
        $release = Release-TestCellLease -Config $Config -CellId $cellId -Owner $Owner -Force:$Force
        $results.Add($release) | Out-Null
    }
    return @($results.ToArray())
}

function New-TestCellBcWaterfallConfigText {
    param(
        [hashtable]$Config,
        [object[]]$Backends,
        [string]$DefaultBackendId
    )

    $priorities = @()
    if (-not [string]::IsNullOrWhiteSpace($DefaultBackendId)) {
        $priorities += $DefaultBackendId
    }
    foreach ($backend in @($Backends)) {
        if ($priorities -notcontains $backend.id) {
            $priorities += $backend.id
        }
    }

    $lines = New-Object System.Collections.Generic.List[string]
    $lines.Add('groups: {}') | Out-Null
    $lines.Add('connection_throttle: 4000') | Out-Null
    $lines.Add('servers:') | Out-Null
    foreach ($backend in @($Backends)) {
        $lines.Add(('  {0}:' -f $backend.id)) | Out-Null
        $lines.Add(('    motd: {0}' -f (ConvertTo-TestCellBcYamlQuoted -Value $backend.id))) | Out-Null
        $lines.Add(('    address: {0}' -f $backend.address)) | Out-Null
        $lines.Add('    restricted: false') | Out-Null
    }
    $lines.Add('listeners:') | Out-Null
    $lines.Add(('- query_port: {0}' -f [int]$Config.listenPort)) | Out-Null
    $lines.Add(('  motd: {0}' -f (ConvertTo-TestCellBcYamlQuoted -Value $Config.motd))) | Out-Null
    $lines.Add('  tab_list: GLOBAL_PING') | Out-Null
    $lines.Add('  query_enabled: false') | Out-Null
    $lines.Add('  proxy_protocol: false') | Out-Null
    $lines.Add('  forced_hosts: {}') | Out-Null
    $lines.Add('  ping_passthrough: false') | Out-Null
    $lines.Add('  priorities:') | Out-Null
    foreach ($priority in $priorities) {
        $lines.Add(('    - {0}' -f $priority)) | Out-Null
    }
    $lines.Add('  bind_local_address: true') | Out-Null
    $lines.Add(('  host: {0}:{1}' -f $Config.listenHost, [int]$Config.listenPort)) | Out-Null
    $lines.Add(('  max_players: {0}' -f [int]$Config.maxPlayers)) | Out-Null
    $lines.Add('  tab_size: 60') | Out-Null
    $lines.Add('  force_default_server: true') | Out-Null
    $lines.Add(('online_mode: {0}' -f $Config.onlineMode.ToString().ToLowerInvariant())) | Out-Null
    $lines.Add(('ip_forward: {0}' -f $Config.ipForward.ToString().ToLowerInvariant())) | Out-Null
    $lines.Add(('forge_support: {0}' -f $Config.forgeSupport.ToString().ToLowerInvariant())) | Out-Null
    $lines.Add('network_compression_threshold: 256') | Out-Null
    $lines.Add('disabled_commands: []') | Out-Null
    $lines.Add('timeout: 30000') | Out-Null
    $lines.Add('player_limit: -1') | Out-Null
    $lines.Add('permissions:') | Out-Null
    $lines.Add('  default:') | Out-Null
    $lines.Add('    - bungeecord.command.server') | Out-Null
    $lines.Add('    - bungeecord.command.list') | Out-Null
    $lines.Add('  admin:') | Out-Null
    $lines.Add('    - bungeecord.command.alert') | Out-Null
    $lines.Add('    - bungeecord.command.end') | Out-Null
    $lines.Add('    - bungeecord.command.ip') | Out-Null
    $lines.Add('    - bungeecord.command.reload') | Out-Null
    $lines.Add('log_pings: false') | Out-Null
    $lines.Add('connection_throttle_limit: 3') | Out-Null
    $lines.Add('prevent_proxy_connections: false') | Out-Null
    $lines.Add('log_commands: false') | Out-Null

    return ($lines -join "`r`n") + "`r`n"
}
