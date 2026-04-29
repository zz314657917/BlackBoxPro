$ErrorActionPreference = 'Stop'

function Get-DefaultTestCellConfigPath {
    return Join-Path $PSScriptRoot 'cells.json'
}

function ConvertTo-FlatHashtable {
    param([object]$InputObject)

    $table = @{}
    if ($null -eq $InputObject) {
        return $table
    }

    foreach ($property in $InputObject.PSObject.Properties) {
        $table[$property.Name] = $property.Value
    }
    return $table
}

function New-MergedCellObject {
    param(
        [hashtable]$Defaults,
        [object]$Cell
    )

    $merged = @{}
    foreach ($entry in $Defaults.GetEnumerator()) {
        $merged[$entry.Key] = $entry.Value
    }
    foreach ($entry in (ConvertTo-FlatHashtable -InputObject $Cell).GetEnumerator()) {
        $merged[$entry.Key] = $entry.Value
    }
    return [pscustomobject]$merged
}

function Load-TestCellConfig {
    param([string]$ConfigPath = (Get-DefaultTestCellConfigPath))

    if (-not (Test-Path -LiteralPath $ConfigPath)) {
        throw "Cell config not found: $ConfigPath"
    }

    $json = Get-Content -Raw -Encoding UTF8 -LiteralPath $ConfigPath | ConvertFrom-Json
    $defaults = ConvertTo-FlatHashtable -InputObject $json.defaults
    $cells = @($json.cells | ForEach-Object { New-MergedCellObject -Defaults $defaults -Cell $_ })

    return [pscustomobject]@{
        path = (Resolve-Path -LiteralPath $ConfigPath).ProviderPath
        defaultCellId = $json.defaultCellId
        defaults = [pscustomobject]$defaults
        cells = $cells
        version = $json.version
    }
}

function Get-TestCell {
    param(
        [object]$Config,
        [string]$CellId
    )

    $resolvedId = if ([string]::IsNullOrWhiteSpace($CellId)) { $Config.defaultCellId } else { $CellId }
    $cell = @($Config.cells | Where-Object { $_.id -eq $resolvedId }) | Select-Object -First 1
    if ($null -eq $cell) {
        throw "Unknown test cell id: $resolvedId"
    }
    return $cell
}

function Get-TestCellLockRoot {
    param([string]$ConfigPath = (Get-DefaultTestCellConfigPath))

    $root = Join-Path (Split-Path -Parent $ConfigPath) 'locks'
    if (-not (Test-Path -LiteralPath $root)) {
        New-Item -ItemType Directory -Path $root | Out-Null
    }
    return (Resolve-Path -LiteralPath $root).ProviderPath
}

function Get-TestCellLockPath {
    param(
        [string]$CellId,
        [string]$ConfigPath = (Get-DefaultTestCellConfigPath))

    return Join-Path (Get-TestCellLockRoot -ConfigPath $ConfigPath) $CellId
}

function Get-TestCellLeasePath {
    param(
        [string]$CellId,
        [string]$ConfigPath = (Get-DefaultTestCellConfigPath))

    return Join-Path (Get-TestCellLockPath -CellId $CellId -ConfigPath $ConfigPath) 'lease.json'
}

function Read-TestCellLease {
    param(
        [string]$CellId,
        [string]$ConfigPath = (Get-DefaultTestCellConfigPath))

    $leasePath = Get-TestCellLeasePath -CellId $CellId -ConfigPath $ConfigPath
    if (-not (Test-Path -LiteralPath $leasePath)) {
        return $null
    }

    return Get-Content -Raw -Encoding UTF8 -LiteralPath $leasePath | ConvertFrom-Json
}

function Write-TestCellLease {
    param(
        [string]$CellId,
        [object]$Lease,
        [string]$ConfigPath = (Get-DefaultTestCellConfigPath))

    $lockPath = Get-TestCellLockPath -CellId $CellId -ConfigPath $ConfigPath
    if (-not (Test-Path -LiteralPath $lockPath)) {
        New-Item -ItemType Directory -Path $lockPath | Out-Null
    }

    $leaseJson = $Lease | ConvertTo-Json -Depth 10
    [System.IO.File]::WriteAllText((Get-TestCellLeasePath -CellId $CellId -ConfigPath $ConfigPath), $leaseJson, [System.Text.UTF8Encoding]::new($false))
}

function Remove-TestCellLock {
    param(
        [string]$CellId,
        [string]$ConfigPath = (Get-DefaultTestCellConfigPath))

    $lockPath = Get-TestCellLockPath -CellId $CellId -ConfigPath $ConfigPath
    if (Test-Path -LiteralPath $lockPath) {
        Remove-Item -LiteralPath $lockPath -Recurse -Force
    }
}

function Test-TestCellPathsReady {
    param([object]$Cell)

    return (
        (Test-Path -LiteralPath $Cell.serverDir) -and
        (Test-Path -LiteralPath $Cell.botWorkspaceRoot) -and
        (Test-Path -LiteralPath $Cell.botVersionDir)
    )
}

function Test-TestCellLeaseExpired {
    param([object]$Lease)

    if ($null -eq $Lease) {
        return $false
    }
    $expiresAt = [DateTimeOffset]::Parse($Lease.expiresAt)
    return $expiresAt -lt [DateTimeOffset]::UtcNow
}

function Acquire-TestCellLease {
    param(
        [object]$Config,
        [string]$Owner,
        [string]$CellId,
        [int]$LeaseMinutes = 120,
        [int]$WaitSeconds = 0,
        [switch]$ReadyOnly
    )

    if ([string]::IsNullOrWhiteSpace($Owner)) {
        $Owner = "owner-$PID"
    }

    $deadline = [DateTimeOffset]::UtcNow.AddSeconds($WaitSeconds)
    while ($true) {
        $candidates = if ([string]::IsNullOrWhiteSpace($CellId)) {
            @($Config.cells | Where-Object { $_.enabled })
        } else {
            @(Get-TestCell -Config $Config -CellId $CellId)
        }

        foreach ($cell in $candidates) {
            if (-not $cell.enabled) {
                continue
            }
            if ($ReadyOnly -and -not (Test-TestCellPathsReady -Cell $cell)) {
                continue
            }

            $lockPath = Get-TestCellLockPath -CellId $cell.id -ConfigPath $Config.path
            if (Test-Path -LiteralPath $lockPath) {
                $lease = Read-TestCellLease -CellId $cell.id -ConfigPath $Config.path
                if (Test-TestCellLeaseExpired -Lease $lease) {
                    Remove-TestCellLock -CellId $cell.id -ConfigPath $Config.path
                } else {
                    continue
                }
            }

            try {
                New-Item -ItemType Directory -Path $lockPath -ErrorAction Stop | Out-Null
                $lease = [pscustomobject]@{
                    cellId = $cell.id
                    owner = $Owner
                    acquiredAt = [DateTimeOffset]::UtcNow.ToString('o')
                    expiresAt = [DateTimeOffset]::UtcNow.AddMinutes($LeaseMinutes).ToString('o')
                    processId = $PID
                    host = $env:COMPUTERNAME
                }
                Write-TestCellLease -CellId $cell.id -Lease $lease -ConfigPath $Config.path
                return [pscustomobject]@{
                    cell = $cell
                    lease = $lease
                    lockPath = $lockPath
                }
            } catch {
                continue
            }
        }

        if ($WaitSeconds -le 0 -or [DateTimeOffset]::UtcNow -ge $deadline) {
            throw 'No available test cell could be acquired.'
        }

        Start-Sleep -Seconds 2
    }
}

function Release-TestCellLease {
    param(
        [object]$Config,
        [string]$CellId,
        [string]$Owner,
        [switch]$Force
    )

    $cell = Get-TestCell -Config $Config -CellId $CellId
    $lease = Read-TestCellLease -CellId $cell.id -ConfigPath $Config.path
    if ($null -eq $lease) {
        return [pscustomobject]@{
            released = $false
            reason = 'not-locked'
            cell = $cell
        }
    }

    if (-not $Force -and $lease.owner -ne $Owner) {
        throw "Lease owner mismatch for ${CellId}: current=$($lease.owner) requested=$Owner"
    }

    Remove-TestCellLock -CellId $cell.id -ConfigPath $Config.path
    return [pscustomobject]@{
        released = $true
        cell = $cell
        lease = $lease
    }
}

function Get-TestCellStatusEntries {
    param([object]$Config)

    return @($Config.cells | ForEach-Object {
        $lease = Read-TestCellLease -CellId $_.id -ConfigPath $Config.path
        [pscustomobject]@{
            id = $_.id
            label = $_.label
            enabled = $_.enabled
            ready = (Test-TestCellPathsReady -Cell $_)
            locked = ($null -ne $lease)
            leaseOwner = if ($null -ne $lease) { $lease.owner } else { $null }
            leaseExpiresAt = if ($null -ne $lease) { $lease.expiresAt } else { $null }
            serverDir = $_.serverDir
            botWorkspaceRoot = $_.botWorkspaceRoot
            serverPort = $_.serverPort
            pluginHttpPort = $_.pluginHttpPort
            modHttpPort = $_.modHttpPort
        }
    })
}
