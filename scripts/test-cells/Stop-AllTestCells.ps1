param(
    [string]$ConfigPath = '',
    [string[]]$CellIds = @(),
    [switch]$IncludeDisabled
)

$ErrorActionPreference = 'Stop'
$OutputEncoding = [Console]::OutputEncoding = [Text.UTF8Encoding]::new($false)
if ([string]::IsNullOrWhiteSpace($ConfigPath)) {
    $ConfigPath = Join-Path $PSScriptRoot 'cells.json'
}

. (Join-Path $PSScriptRoot 'TestCellCommon.ps1')

$config = Load-TestCellConfig -ConfigPath $ConfigPath
$invokePath = Join-Path $PSScriptRoot 'Invoke-TestCell.ps1'

$targets = if ($CellIds.Count -gt 0) {
    @($CellIds | ForEach-Object { Get-TestCell -Config $config -CellId $_ })
} else {
    @($config.cells | Where-Object { $IncludeDisabled -or $_.enabled })
}

$results = New-Object System.Collections.Generic.List[object]
$failed = $false

foreach ($cell in $targets) {
    try {
        $raw = & $invokePath -Mode stop -CellId $cell.id -ConfigPath $config.path
        $parsed = $raw | ConvertFrom-Json
        $results.Add([pscustomobject]@{
                cellId = $cell.id
                ok = [bool]$parsed.ok
                stop = $parsed.steps.server.stop
            }) | Out-Null

        if (-not $parsed.ok) {
            $failed = $true
        }
    } catch {
        $failed = $true
        $results.Add([pscustomobject]@{
                cellId = $cell.id
                ok = $false
                error = $_.Exception.Message
            }) | Out-Null
    }
}

[pscustomobject]@{
    ok = (-not $failed)
    configPath = $config.path
    stoppedCells = $results.ToArray()
} | ConvertTo-Json -Depth 12

if ($failed) {
    exit 1
}
