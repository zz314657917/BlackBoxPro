param(
    [ValidateSet('smoke', 'full')]
    [string]$Scope = 'smoke',
    [string]$CellId = '',
    [string]$ConfigPath = '',
    [switch]$AcquireCell,
    [int]$WaitSeconds = 60,
    [int]$LeaseMinutes = 120,
    [string]$LeaseOwner = '',
    [switch]$KeepCell,
    [switch]$ShowClient
)

$ErrorActionPreference = 'Stop'
$OutputEncoding = [Console]::OutputEncoding = [Text.UTF8Encoding]::new($false)

if ([string]::IsNullOrWhiteSpace($ConfigPath)) {
    $ConfigPath = Join-Path $PSScriptRoot 'cells.json'
}
if ([string]::IsNullOrWhiteSpace($LeaseOwner)) {
    $LeaseOwner = "bbp-regression-$Scope-$PID"
}

. (Join-Path $PSScriptRoot 'TestCellCommon.ps1')

$invokeScriptPath = Join-Path $PSScriptRoot 'Invoke-TestCell.ps1'
$config = Load-TestCellConfig -ConfigPath $ConfigPath
$acquiredLease = $null

if ($AcquireCell -or [string]::IsNullOrWhiteSpace($CellId)) {
    $acquiredLease = Acquire-TestCellLease -Config $config -Owner $LeaseOwner -CellId $CellId -LeaseMinutes $LeaseMinutes -WaitSeconds $WaitSeconds -ReadyOnly
    $cell = $acquiredLease.cell
} else {
    $cell = Get-TestCell -Config $config -CellId $CellId
}

$pluginExecuteUrl = "http://127.0.0.1:$($cell.pluginHttpPort)/execute"

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
        ensure = $null
        test = $null
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

function Invoke-JsonRequest {
    param(
        [string]$Uri,
        [object]$Body,
        [int]$TimeoutSec = 1800
    )

    try {
        $jsonBody = if ($Body -is [string]) { $Body } else { $Body | ConvertTo-Json -Compress -Depth 20 }
        $content = Invoke-WebRequest -Uri $Uri -Method Post -UseBasicParsing -TimeoutSec $TimeoutSec -ContentType 'application/json; charset=utf-8' -Body $jsonBody |
            Select-Object -ExpandProperty Content
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

function Invoke-RunTest {
    $request = [ordered]@{
        id = "cell-$Scope-$($cell.id)-$([DateTimeOffset]::UtcNow.ToUnixTimeSeconds())"
        action = 'run_test'
        params = [ordered]@{
            player = $cell.botPlayer
            scope = $Scope
        }
    }

    return Invoke-JsonRequest -Uri $pluginExecuteUrl -Body $request -TimeoutSec 1800
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
    $ensure = Invoke-JsonScript -ScriptPath $invokeScriptPath -Arguments @{
        Mode = 'ensure'
        CellId = $cell.id
        ConfigPath = $ConfigPath
        ShowClient = [bool]$ShowClient
    }
    $result.steps.ensure = $ensure
    if (-not $ensure.ok) {
        throw "Ensure failed for $($cell.id)."
    }

    $test = Invoke-RunTest
    $result.steps.test = $test
    if (-not $test.ok) {
        throw "run_test request failed for $($cell.id): $($test.error)"
    }
    if (-not $test.json -or $test.json.status -ne 'success') {
        throw "run_test did not return success JSON for $($cell.id)."
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
