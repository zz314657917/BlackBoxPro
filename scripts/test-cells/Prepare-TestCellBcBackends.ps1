param(
    [ValidateSet('prepare', 'restore', 'status')]
    [string]$Mode = 'status',
    [string]$CellConfigPath = '',
    [string]$BotCellId = 'cell-02',
    [string[]]$BackendCellIds = @('cell-02', 'cell-03'),
    [string]$Owner = ''
)

$ErrorActionPreference = 'Stop'
$OutputEncoding = [Console]::OutputEncoding = [Text.UTF8Encoding]::new($false)

if ([string]::IsNullOrWhiteSpace($CellConfigPath)) {
    $CellConfigPath = Join-Path $PSScriptRoot 'cells.json'
}

. (Join-Path $PSScriptRoot 'TestCellCommon.ps1')
. (Join-Path $PSScriptRoot 'TestCellBcCommon.ps1')

if ([string]::IsNullOrWhiteSpace($Owner)) {
    if ($Mode -eq 'prepare') {
        $Owner = 'bc-backend-prep-' + (Get-Date -Format 'yyyyMMdd-HHmmss') + "-$PID"
    } else {
        throw 'Owner is required for status/restore.'
    }
}

$config = Load-TestCellConfig -ConfigPath $CellConfigPath
$normalizedBackendIds = @($BackendCellIds)
if ($normalizedBackendIds.Count -eq 1 -and $normalizedBackendIds[0] -match ',') {
    $normalizedBackendIds = @($normalizedBackendIds[0].Split(',') | ForEach-Object { $_.Trim() } | Where-Object { -not [string]::IsNullOrWhiteSpace($_) })
}
$backendIds = Get-TestCellBcUniqueCellIds -CellIds $normalizedBackendIds
$leasedCellIds = Get-TestCellBcUniqueCellIds -CellIds (@($BotCellId) + @($backendIds))
$statePath = Get-TestCellBcPrepStatePath -Owner $Owner -CellConfigPath $config.path

$result = [ordered]@{
    mode = $Mode
    owner = $Owner
    ok = $false
    prepared = $false
    startedAt = (Get-Date).ToString('s')
    cellConfigPath = $config.path
    botCellId = $BotCellId
    backendCellIds = @($backendIds)
    leasedCellIds = @($leasedCellIds)
    stateFilePath = $statePath
    stateFileExists = (Test-Path -LiteralPath $statePath)
    steps = [ordered]@{
        leases = @()
        snapshots = @()
        stopped = @()
        restored = @()
        fileStatus = @()
    }
    errors = New-Object System.Collections.Generic.List[string]
}

function Add-Error {
    param([string]$Message)
    $result.errors.Add($Message) | Out-Null
}

function Get-CommandLinePathPattern {
    param([string]$Path)

    return Get-TestCellCommandLinePathPattern -Path $Path
}

function Stop-PreparedBackendProcesses {
    param([object]$Cell)

    $stopped = [ordered]@{
        cellId = $Cell.id
        cmdPids = @()
        javaPids = @()
    }

    $cmdProcesses = @(Get-CimInstance Win32_Process |
        Where-Object {
            $_.Name -eq 'cmd.exe' -and
            $_.CommandLine -match (Get-CommandLinePathPattern -Path $Cell.serverDir)
        })
    $javaIds = New-Object 'System.Collections.Generic.HashSet[int]'
    $cmdDescendants = @(Get-TestCellDescendantProcesses -RootProcessIds @($cmdProcesses | Select-Object -ExpandProperty ProcessId))
    foreach ($proc in $cmdDescendants) {
        if ($proc.Name -eq 'java.exe') {
            $javaIds.Add([int]$proc.ProcessId) | Out-Null
            if ($stopped.javaPids -notcontains $proc.ProcessId) {
                $stopped.javaPids += $proc.ProcessId
            }
        }
    }
    $cmdTree = Stop-TestCellProcessTree -RootProcesses $cmdProcesses
    $stopped.cmdPids = @($stopped.cmdPids + @($cmdTree.rootPids) | Select-Object -Unique)

    foreach ($port in @($Cell.serverPort, $Cell.pluginHttpPort)) {
        $listener = Get-NetTCPConnection -State Listen -ErrorAction SilentlyContinue |
            Where-Object { $_.LocalPort -eq $port } |
            Select-Object -First 1
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
        if ($stopped.javaPids -notcontains $javaId) {
            $stopped.javaPids += $javaId
        }
    }

    return [pscustomobject]$stopped
}

function Get-PreparedBackendSnapshot {
    param(
        [object]$BackendCell,
        [object]$BotCell
    )

    $spigotPath = Join-Path $BackendCell.serverDir 'spigot.yml'
    $pluginConfigPath = Join-Path $BackendCell.serverDir 'plugins/BlackBoxPro/config.yml'
    if (-not (Test-Path -LiteralPath $spigotPath)) {
        throw "spigot.yml not found for $($BackendCell.id): $spigotPath"
    }
    if (-not (Test-Path -LiteralPath $pluginConfigPath)) {
        throw "BlackBoxPro config not found for $($BackendCell.id): $pluginConfigPath"
    }

    return [pscustomobject]@{
        id = $BackendCell.id
        serverDir = $BackendCell.serverDir
        spigotPath = $spigotPath
        spigotContent = [System.IO.File]::ReadAllText($spigotPath, [System.Text.UTF8Encoding]::new($false))
        pluginConfigPath = $pluginConfigPath
        pluginConfigContent = [System.IO.File]::ReadAllText($pluginConfigPath, [System.Text.UTF8Encoding]::new($false))
        pluginHttpPort = [int]$BackendCell.pluginHttpPort
        sharedBotModHttpPort = [int]$BotCell.modHttpPort
    }
}

function Apply-PreparedBackendSnapshot {
    param([object]$Snapshot)

    $newSpigot = Set-TestCellBcSpigotBungeecordContent -Content $Snapshot.spigotContent -Enabled $true
    [System.IO.File]::WriteAllText($Snapshot.spigotPath, $newSpigot, [System.Text.UTF8Encoding]::new($false))

    $newPluginConfig = Set-TestCellBcPluginConfigContent -Content $Snapshot.pluginConfigContent -PluginHttpPort ([int]$Snapshot.pluginHttpPort) -SharedBotModHttpPort ([int]$Snapshot.sharedBotModHttpPort)
    [System.IO.File]::WriteAllText($Snapshot.pluginConfigPath, $newPluginConfig, [System.Text.UTF8Encoding]::new($false))
}

function Restore-PreparedBackendSnapshot {
    param([object]$Snapshot)

    [System.IO.File]::WriteAllText($Snapshot.spigotPath, [string]$Snapshot.spigotContent, [System.Text.UTF8Encoding]::new($false))
    [System.IO.File]::WriteAllText($Snapshot.pluginConfigPath, [string]$Snapshot.pluginConfigContent, [System.Text.UTF8Encoding]::new($false))
}

function Convert-StateBackendsToSnapshots {
    param([object[]]$Backends)

    return @($Backends | ForEach-Object {
        [pscustomobject]@{
            id = $_.id
            serverDir = $_.serverDir
            spigotPath = $_.spigotPath
            spigotContent = $_.spigotContent
            pluginConfigPath = $_.pluginConfigPath
            pluginConfigContent = $_.pluginConfigContent
            pluginHttpPort = [int]$_.pluginHttpPort
            sharedBotModHttpPort = [int]$_.sharedBotModHttpPort
        }
    })
}

try {
    $botCell = Get-TestCell -Config $config -CellId $BotCellId
    $backendCells = @($backendIds | ForEach-Object { Get-TestCell -Config $config -CellId $_ })

    if ($Mode -eq 'status') {
        $state = Read-TestCellBcPrepState -Owner $Owner -CellConfigPath $config.path
        if ($null -eq $state) {
            $result.stateFileExists = $false
            $result.prepared = $false
            $result.ok = $true
        } else {
            $result.stateFileExists = $true
            $result.botCellId = $state.botCellId
            $result.backendCellIds = @($state.backendCellIds)
            $result.leasedCellIds = @($state.leasedCellIds)

            foreach ($cellId in @($state.leasedCellIds)) {
                $lease = Read-TestCellLease -CellId $cellId -ConfigPath $config.path
                $result.steps.leases += [ordered]@{
                    cellId = $cellId
                    present = ($null -ne $lease)
                    leaseOwner = if ($null -ne $lease) { $lease.owner } else { $null }
                    ownerMatches = ($null -ne $lease -and $lease.owner -eq $Owner)
                }
            }

            foreach ($snapshot in (Convert-StateBackendsToSnapshots -Backends $state.backends)) {
                $currentSpigot = if (Test-Path -LiteralPath $snapshot.spigotPath) { [System.IO.File]::ReadAllText($snapshot.spigotPath, [System.Text.UTF8Encoding]::new($false)) } else { $null }
                $currentPlugin = if (Test-Path -LiteralPath $snapshot.pluginConfigPath) { [System.IO.File]::ReadAllText($snapshot.pluginConfigPath, [System.Text.UTF8Encoding]::new($false)) } else { $null }
                $result.steps.fileStatus += [ordered]@{
                    cellId = $snapshot.id
                    spigotMatchesSnapshot = ($currentSpigot -eq $snapshot.spigotContent)
                    pluginConfigMatchesSnapshot = ($currentPlugin -eq $snapshot.pluginConfigContent)
                    spigotExists = ($null -ne $currentSpigot)
                    pluginConfigExists = ($null -ne $currentPlugin)
                }
            }

            $result.prepared = (@($result.steps.leases | Where-Object { -not $_.ownerMatches }).Count -eq 0)
            $result.ok = $true
        }
    }
    elseif ($Mode -eq 'prepare') {
        $acquiredLeases = @()
        $stateWritten = $false
        $snapshots = @()

        try {
            $acquiredLeases = @(Acquire-TestCellBcLeases -Config $config -CellIds $leasedCellIds -Owner $Owner)
            $result.steps.leases = @($acquiredLeases | ForEach-Object {
                [ordered]@{
                    cellId = $_.cellId
                    acquired = $true
                }
            })

            $snapshots = @($backendCells | ForEach-Object { Get-PreparedBackendSnapshot -BackendCell $_ -BotCell $botCell })
            $result.steps.snapshots = @($snapshots | ForEach-Object {
                [ordered]@{
                    cellId = $_.id
                    spigotPath = $_.spigotPath
                    pluginConfigPath = $_.pluginConfigPath
                }
            })

            $state = [pscustomobject]@{
                owner = $Owner
                cellConfigPath = $config.path
                botCellId = $botCell.id
                backendCellIds = @($backendCells | ForEach-Object { $_.id })
                leasedCellIds = @($leasedCellIds)
                preparedAt = (Get-Date).ToString('o')
                backends = @($snapshots)
            }
            $stateFile = Write-TestCellBcPrepState -Owner $Owner -State $state -CellConfigPath $config.path
            $result.stateFilePath = $stateFile
            $result.stateFileExists = $true
            $stateWritten = $true

            foreach ($backendCell in $backendCells) {
                $result.steps.stopped += Stop-PreparedBackendProcesses -Cell $backendCell
            }
            foreach ($snapshot in $snapshots) {
                Apply-PreparedBackendSnapshot -Snapshot $snapshot
                $result.steps.restored += [ordered]@{
                    cellId = $snapshot.id
                    prepared = $true
                }
            }

            $result.prepared = $true
            $result.ok = $true
        } catch {
            foreach ($snapshot in @($snapshots)) {
                if (Test-Path -LiteralPath $snapshot.spigotPath -and Test-Path -LiteralPath $snapshot.pluginConfigPath) {
                    try {
                        Restore-PreparedBackendSnapshot -Snapshot $snapshot
                    } catch {
                    }
                }
            }
            if ($stateWritten) {
                Remove-TestCellBcPrepState -Owner $Owner -CellConfigPath $config.path
                $result.stateFileExists = $false
            }
            if ($acquiredLeases.Count -gt 0) {
                try {
                    $null = Release-TestCellBcLeases -Config $config -CellIds @($acquiredLeases | ForEach-Object { $_.cellId }) -Owner $Owner
                } catch {
                }
            }
            throw
        }
    }
    else {
        $state = Read-TestCellBcPrepState -Owner $Owner -CellConfigPath $config.path
        if ($null -eq $state) {
            throw "State file not found for owner $Owner."
        }

        $snapshots = @(Convert-StateBackendsToSnapshots -Backends $state.backends)
        foreach ($snapshot in $snapshots) {
            $backendCell = Get-TestCell -Config $config -CellId $snapshot.id
            $result.steps.stopped += Stop-PreparedBackendProcesses -Cell $backendCell
            Restore-PreparedBackendSnapshot -Snapshot $snapshot
            $result.steps.restored += [ordered]@{
                cellId = $snapshot.id
                restored = $true
            }
        }

        $releaseResults = @(Release-TestCellBcLeases -Config $config -CellIds @($state.leasedCellIds) -Owner $Owner)
        $result.steps.leases = @($releaseResults | ForEach-Object {
            [ordered]@{
                cellId = $_.cell.id
                released = $_.released
                reason = $_.reason
            }
        })
        Remove-TestCellBcPrepState -Owner $Owner -CellConfigPath $config.path
        $result.stateFileExists = $false
        $result.prepared = $false
        $result.ok = $true
    }
}
catch {
    Add-Error $_.Exception.Message
    $result.ok = $false
}
finally {
    $result.finishedAt = (Get-Date).ToString('s')
    $result | ConvertTo-Json -Depth 20
    if (-not $result.ok) {
        exit 1
    }
}
