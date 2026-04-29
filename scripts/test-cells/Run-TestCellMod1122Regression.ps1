param(
    [ValidateSet('startup', 'smoke')]
    [string]$Scope = 'startup',
    [string]$CellId = 'cell-20',
    [string]$ConfigPath = '',
    [string[]]$ModJar = @(),
    [string[]]$RemoveExistingPattern = @(),
    [switch]$AcquireCell,
    [int]$WaitSeconds = 60,
    [int]$LeaseMinutes = 120,
    [string]$LeaseOwner = '',
    [switch]$KeepCell,
    [switch]$ShowClient
)

$ErrorActionPreference = 'Stop'
$OutputEncoding = [Console]::OutputEncoding = [Text.UTF8Encoding]::new($false)
$explicitCellId = $PSBoundParameters.ContainsKey('CellId')

if ([string]::IsNullOrWhiteSpace($ConfigPath)) {
    $ConfigPath = Join-Path $PSScriptRoot 'cells-mod1122.json'
}
if ([string]::IsNullOrWhiteSpace($LeaseOwner)) {
    $LeaseOwner = "bbp-mod1122-$Scope-$PID"
}
if ($AcquireCell -and -not $explicitCellId) {
    $CellId = ''
}

. (Join-Path $PSScriptRoot 'TestCellCommon.ps1')

$invokeScriptPath = Join-Path $PSScriptRoot 'Invoke-TestCell.ps1'
$syncScriptPath = Join-Path $PSScriptRoot 'Sync-TestCellMod1122Artifacts.ps1'
$config = Load-TestCellConfig -ConfigPath $ConfigPath
$acquiredLease = $null

if ($AcquireCell -or [string]::IsNullOrWhiteSpace($CellId)) {
    $acquiredLease = Acquire-TestCellLease -Config $config -Owner $LeaseOwner -CellId $CellId -LeaseMinutes $LeaseMinutes -WaitSeconds $WaitSeconds -ReadyOnly
    $cell = $acquiredLease.cell
} else {
    $cell = Get-TestCell -Config $config -CellId $CellId
}

$result = [ordered]@{
    scope = $Scope
    cellId = $cell.id
    leaseOwner = $LeaseOwner
    acquiredCell = ($null -ne $acquiredLease)
    keepCell = $KeepCell
    cleanupRequested = (-not $KeepCell)
    ok = $false
    startedAt = (Get-Date).ToString('s')
    steps = [ordered]@{
        acquire = $null
        sync = $null
        invoke = $null
        cleanup = $null
    }
    errors = New-Object System.Collections.Generic.List[string]
}

if ($null -ne $acquiredLease) {
    $result.steps.acquire = [ordered]@{
        cell = $acquiredLease.cell
        lease = $acquiredLease.lease
    }
}

function Add-Error {
    param([string]$Message)
    $result.errors.Add($Message) | Out-Null
}

function Invoke-JsonScript {
    param(
        [string]$ScriptPath,
        [hashtable]$Arguments
    )

    try {
        $global:LASTEXITCODE = 0
        $output = & $ScriptPath @Arguments 2>&1
        $exitCode = if ($null -ne $LASTEXITCODE) { $LASTEXITCODE } else { 0 }
    } catch {
        $output = @($_)
        $exitCode = 1
    }

    $lines = @($output | ForEach-Object { [string]$_ })
    $text = [string]::Join([Environment]::NewLine, $lines)
    $json = $null
    if (-not [string]::IsNullOrWhiteSpace($text)) {
        try {
            $json = $text | ConvertFrom-Json -ErrorAction Stop
        } catch {
            $json = $null
        }
    }

    return [ordered]@{
        ok = ($exitCode -eq 0)
        exitCode = $exitCode
        output = $lines
        text = $text
        json = $json
    }
}

function Cleanup-Cell {
    $cleanup = [ordered]@{
        stop = $null
        release = $null
    }

    $cleanup.stop = Invoke-JsonScript -ScriptPath $invokeScriptPath -Arguments @{
        Mode = 'stop'
        CellId = $cell.id
        ConfigPath = $ConfigPath
    }

    if ($null -ne $acquiredLease) {
        try {
            $release = Release-TestCellLease -Config $config -CellId $cell.id -Owner $LeaseOwner
            $cleanup.release = [ordered]@{
                ok = $true
                json = $release
            }
        } catch {
            $cleanup.release = [ordered]@{
                ok = $false
                error = $_.Exception.Message
            }
        }
    }

    return $cleanup
}

try {
    if ($ModJar.Count -gt 0) {
        $syncArgs = @{
            ConfigPath = $ConfigPath
            CellIds = @($cell.id)
            ModJar = $ModJar
        }
        if ($RemoveExistingPattern.Count -gt 0) {
            $syncArgs.RemoveExistingPattern = $RemoveExistingPattern
        }
        $sync = Invoke-JsonScript -ScriptPath $syncScriptPath -Arguments $syncArgs
        $result.steps.sync = $sync
        if (-not $sync.ok) {
            throw "Artifact sync failed for $($cell.id)."
        }
    }

    $mode = if ($Scope -eq 'smoke') { 'smoke' } else { 'ensure' }
    $invoke = Invoke-JsonScript -ScriptPath $invokeScriptPath -Arguments @{
        Mode = $mode
        CellId = $cell.id
        ConfigPath = $ConfigPath
        ShowClient = [bool]$ShowClient
    }
    $result.steps.invoke = $invoke
    if (-not $invoke.ok) {
        throw "$mode failed for $($cell.id)."
    }

    $result.ok = $true
}
catch {
    Add-Error $_.Exception.Message
    $result.ok = $false
}
finally {
    if (-not $KeepCell) {
        $result.steps.cleanup = Cleanup-Cell
        if ($null -ne $result.steps.cleanup.stop -and -not $result.steps.cleanup.stop.ok) {
            Add-Error "Stop cleanup failed for $($cell.id)."
        }
        if ($null -ne $result.steps.cleanup.release -and -not $result.steps.cleanup.release.ok) {
            Add-Error "Release cleanup failed for $($cell.id)."
        }
    }

    $result.ok = ($result.errors.Count -eq 0)
    $result.finishedAt = (Get-Date).ToString('s')
    [pscustomobject]$result | ConvertTo-Json -Depth 20
    if (-not $result.ok) {
        exit 1
    }
}
