param(
    [string]$ConfigPath = '',
    [string]$BaselineConfigPath = '',
    [string[]]$CellIds = @(),
    [switch]$IncludeDisabled,
    [switch]$IncludeMainServer
)

$ErrorActionPreference = 'Stop'
$OutputEncoding = [Console]::OutputEncoding = [Text.UTF8Encoding]::new($false)
if ([string]::IsNullOrWhiteSpace($ConfigPath)) {
    $ConfigPath = Join-Path $PSScriptRoot 'cells.json'
}

. (Join-Path $PSScriptRoot 'TestCellCommon.ps1')
. (Join-Path $PSScriptRoot 'TestCellBaselinePlugins.ps1')

function Move-PluginIfNeeded {
    param(
        [string]$SourcePath,
        [string]$DestinationDir
    )

    $name = Split-Path -Leaf $SourcePath
    $destinationPath = Join-Path $DestinationDir $name
    if (Test-Path -LiteralPath $destinationPath) {
        Remove-Item -LiteralPath $destinationPath -Force
    }
    Move-Item -LiteralPath $SourcePath -Destination $destinationPath -Force
}

function Test-IsManagedTestCell {
    param([object]$Cell)

    $leaf = Split-Path -Leaf $Cell.serverDir
    return $leaf -like 'server-cell-*'
}

$config = Load-TestCellConfig -ConfigPath $ConfigPath
$baseline = Load-TestCellBaselineConfig -BaselineConfigPath $BaselineConfigPath -CellConfigPath $config.path
$targets = if ($CellIds.Count -gt 0) {
    @($CellIds | ForEach-Object { Get-TestCell -Config $config -CellId $_ })
} else {
    @($config.cells | Where-Object {
            ($IncludeDisabled -or $_.enabled) -and
            ($IncludeMainServer -or (Test-IsManagedTestCell -Cell $_))
        })
}

if (-not $IncludeMainServer) {
    $unmanaged = @($targets | Where-Object { -not (Test-IsManagedTestCell -Cell $_) })
    if ($unmanaged.Count -gt 0) {
        $ids = ($unmanaged | ForEach-Object { $_.id }) -join ', '
        throw "Refusing to touch non-test-cell plugin directories without -IncludeMainServer: $ids"
    }
}

$summaries = New-Object System.Collections.Generic.List[object]

foreach ($cell in $targets) {
    $pluginsDir = Join-Path $cell.serverDir 'plugins'
    if (-not (Test-Path -LiteralPath $pluginsDir)) {
        $summaries.Add([pscustomobject]@{
                cellId = $cell.id
                pluginsDir = $pluginsDir
                exists = $false
                kept = @()
                movedToDisabled = @()
                restored = @()
            }) | Out-Null
        continue
    }

    $disabledDir = Join-Path $pluginsDir '_disabled-by-codex'
    if (-not (Test-Path -LiteralPath $disabledDir)) {
        New-Item -ItemType Directory -Path $disabledDir | Out-Null
    }

    $restored = New-Object System.Collections.Generic.List[string]
    Get-ChildItem -LiteralPath $disabledDir -File -Filter '*.jar' -ErrorAction SilentlyContinue |
        Where-Object { Test-IsTestCellBaselinePlugin -Name $_.Name -BaselineConfig $baseline } |
        ForEach-Object {
            Move-PluginIfNeeded -SourcePath $_.FullName -DestinationDir $pluginsDir
            $restored.Add($_.Name) | Out-Null
        }

    $kept = New-Object System.Collections.Generic.List[string]
    $moved = New-Object System.Collections.Generic.List[string]
    Get-ChildItem -LiteralPath $pluginsDir -File -Filter '*.jar' |
        ForEach-Object {
            if (Test-IsTestCellBaselinePlugin -Name $_.Name -BaselineConfig $baseline) {
                $kept.Add($_.Name) | Out-Null
            } else {
                Move-PluginIfNeeded -SourcePath $_.FullName -DestinationDir $disabledDir
                $moved.Add($_.Name) | Out-Null
            }
        }

    $summaries.Add([pscustomobject]@{
            cellId = $cell.id
            pluginsDir = $pluginsDir
            exists = $true
            kept = $kept.ToArray()
            movedToDisabled = $moved.ToArray()
            restored = $restored.ToArray()
        }) | Out-Null
}

[pscustomobject]@{
    ok = $true
    configPath = $config.path
    baselineConfigPath = $baseline.path
    baselineId = $baseline.id
    allowedPatterns = (Get-TestCellBaselinePluginPatterns -BaselineConfig $baseline)
    cells = $summaries.ToArray()
} | ConvertTo-Json -Depth 12
