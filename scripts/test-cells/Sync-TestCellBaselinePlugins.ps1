param(
    [string]$ConfigPath = '',
    [string]$BaselineConfigPath = '',
    [string]$SourceCellId = '',
    [string[]]$TargetCellIds = @(),
    [switch]$DryRun
)

$ErrorActionPreference = 'Stop'
$OutputEncoding = [Console]::OutputEncoding = [Text.UTF8Encoding]::new($false)
if ([string]::IsNullOrWhiteSpace($ConfigPath)) {
    $ConfigPath = Join-Path $PSScriptRoot 'cells.json'
}

. (Join-Path $PSScriptRoot 'TestCellCommon.ps1')
. (Join-Path $PSScriptRoot 'TestCellBaselinePlugins.ps1')

function Test-IsManagedTestCell {
    param([object]$Cell)

    $leaf = Split-Path -Leaf $Cell.serverDir
    return $leaf -like 'server-cell-*'
}

function Remove-FileIfExists {
    param(
        [string]$Path,
        [bool]$DryRunMode
    )

    if (-not (Test-Path -LiteralPath $Path)) {
        return $false
    }
    if (-not $DryRunMode) {
        Remove-Item -LiteralPath $Path -Force
    }
    return $true
}

function Copy-FileIfNeeded {
    param(
        [string]$SourcePath,
        [string]$DestinationPath,
        [bool]$DryRunMode
    )

    if (-not $DryRunMode) {
        Copy-Item -LiteralPath $SourcePath -Destination $DestinationPath -Force
    }
}

function Get-FileSha256 {
    param([string]$Path)

    return (Get-FileHash -LiteralPath $Path -Algorithm SHA256).Hash
}

function Test-SameFile {
    param(
        [string]$SourcePath,
        [string]$DestinationPath
    )

    if (-not (Test-Path -LiteralPath $DestinationPath)) {
        return $false
    }

    $sourceInfo = Get-Item -LiteralPath $SourcePath
    $destinationInfo = Get-Item -LiteralPath $DestinationPath
    if ($sourceInfo.Length -ne $destinationInfo.Length) {
        return $false
    }

    return (Get-FileSha256 -Path $SourcePath) -eq (Get-FileSha256 -Path $DestinationPath)
}

function Sync-BaselineFilesToTarget {
    param(
        [System.IO.FileInfo[]]$SourceFiles,
        [string]$TargetDir,
        [object]$BaselineConfig,
        [scriptblock]$GetTargetBaselineFiles,
        [bool]$DryRunMode
    )

    $sourceFileNames = New-Object System.Collections.Generic.HashSet[string]([System.StringComparer]::OrdinalIgnoreCase)
    foreach ($sourceFile in $SourceFiles) {
        $sourceFileNames.Add($sourceFile.Name) | Out-Null
    }

    $removed = New-Object System.Collections.Generic.List[string]
    $copied = New-Object System.Collections.Generic.List[string]
    $updated = New-Object System.Collections.Generic.List[string]
    $skipped = New-Object System.Collections.Generic.List[string]

    foreach ($file in @(& $GetTargetBaselineFiles $TargetDir $BaselineConfig)) {
        if (-not $sourceFileNames.Contains($file.Name) -and (Remove-FileIfExists -Path $file.FullName -DryRunMode $DryRunMode)) {
            $removed.Add($file.Name) | Out-Null
        }
    }

    foreach ($sourceFile in $SourceFiles) {
        $destinationPath = Join-Path $TargetDir $sourceFile.Name
        if (-not (Test-Path -LiteralPath $destinationPath)) {
            Copy-FileIfNeeded -SourcePath $sourceFile.FullName -DestinationPath $destinationPath -DryRunMode $DryRunMode
            $copied.Add($sourceFile.Name) | Out-Null
        } elseif (Test-SameFile -SourcePath $sourceFile.FullName -DestinationPath $destinationPath) {
            $skipped.Add($sourceFile.Name) | Out-Null
        } else {
            Copy-FileIfNeeded -SourcePath $sourceFile.FullName -DestinationPath $destinationPath -DryRunMode $DryRunMode
            $updated.Add($sourceFile.Name) | Out-Null
        }
    }

    return [pscustomobject]@{
        removed = $removed.ToArray()
        copied = $copied.ToArray()
        updated = $updated.ToArray()
        skipped = $skipped.ToArray()
    }
}

$config = Load-TestCellConfig -ConfigPath $ConfigPath
$baseline = Load-TestCellBaselineConfig -BaselineConfigPath $BaselineConfigPath -CellConfigPath $config.path

if ([string]::IsNullOrWhiteSpace($SourceCellId)) {
    $SourceCellId = $baseline.sourceCellId
}
if ($TargetCellIds.Count -eq 0) {
    $TargetCellIds = @($baseline.targetCellIds)
}

$sourceCell = Get-TestCell -Config $config -CellId $SourceCellId
if (-not (Test-IsManagedTestCell -Cell $sourceCell)) {
    throw "Source cell is not a managed test-cell: $SourceCellId"
}

$sourcePluginsDir = Join-Path $sourceCell.serverDir 'plugins'
if (-not (Test-Path -LiteralPath $sourcePluginsDir)) {
    throw "Source plugins dir not found: $sourcePluginsDir. The repo may still be using sample paths in $($config.path); update the local test-cell config first."
}
$sourceModsDir = Join-Path $sourceCell.botVersionDir 'mods'

$sourceBaselinePlugins = @(Get-TestCellBaselinePluginFiles -PluginsDir $sourcePluginsDir -BaselineConfig $baseline)
if ($sourceBaselinePlugins.Count -eq 0) {
    throw "No baseline plugins matched in source plugins dir: $sourcePluginsDir for baseline $($baseline.id)"
}

$sourcePluginNames = New-Object System.Collections.Generic.HashSet[string]([System.StringComparer]::OrdinalIgnoreCase)
foreach ($sourceFile in $sourceBaselinePlugins) {
    $sourcePluginNames.Add($sourceFile.Name) | Out-Null
}

$sourceBaselineBotMods = @()
if ((Get-TestCellBaselineBotModPatterns -BaselineConfig $baseline).Count -gt 0) {
    if (-not (Test-Path -LiteralPath $sourceModsDir)) {
        throw "Source bot mods dir not found: $sourceModsDir"
    }
    $sourceBaselineBotMods = @(Get-TestCellBaselineBotModFiles -ModsDir $sourceModsDir -BaselineConfig $baseline)
    if ($sourceBaselineBotMods.Count -eq 0) {
        throw "No baseline bot mods matched in source mods dir: $sourceModsDir for baseline $($baseline.id)"
    }
}

$summaries = New-Object System.Collections.Generic.List[object]

foreach ($targetId in $TargetCellIds) {
    $targetCell = Get-TestCell -Config $config -CellId $targetId
    if (-not (Test-IsManagedTestCell -Cell $targetCell)) {
        throw "Target cell is not a managed test-cell: $targetId"
    }
    if ($targetCell.id -eq $sourceCell.id) {
        continue
    }

    $pluginsDir = Join-Path $targetCell.serverDir 'plugins'
    $modsDir = Join-Path $targetCell.botVersionDir 'mods'
    $disabledDir = Join-Path $pluginsDir '_disabled-by-codex'
    $removedFromPlugins = New-Object System.Collections.Generic.List[string]
    $removedFromDisabled = New-Object System.Collections.Generic.List[string]
    $copied = New-Object System.Collections.Generic.List[string]
    $updated = New-Object System.Collections.Generic.List[string]
    $skipped = New-Object System.Collections.Generic.List[string]
    $botMods = [pscustomobject]@{
        modsDir = $modsDir
        exists = $false
        copied = @()
        updated = @()
        skipped = @()
        removed = @()
    }

    if (-not (Test-Path -LiteralPath $pluginsDir)) {
        $summaries.Add([pscustomobject]@{
                cellId = $targetCell.id
                pluginsDir = $pluginsDir
                exists = $false
                copied = @()
                updated = @()
                skipped = @()
                removedFromPlugins = @()
                removedFromDisabled = @()
            }) | Out-Null
        continue
    }

    foreach ($file in @(Get-TestCellBaselinePluginFiles -PluginsDir $pluginsDir -BaselineConfig $baseline)) {
        if (-not $sourcePluginNames.Contains($file.Name) -and (Remove-FileIfExists -Path $file.FullName -DryRunMode $DryRun.IsPresent)) {
            $removedFromPlugins.Add($file.Name) | Out-Null
        }
    }

    if (Test-Path -LiteralPath $disabledDir) {
        foreach ($file in @(Get-TestCellBaselinePluginFiles -PluginsDir $disabledDir -BaselineConfig $baseline)) {
            if (Remove-FileIfExists -Path $file.FullName -DryRunMode $DryRun.IsPresent) {
                $removedFromDisabled.Add($file.Name) | Out-Null
            }
        }
    }

    $pluginSync = Sync-BaselineFilesToTarget -SourceFiles $sourceBaselinePlugins -TargetDir $pluginsDir -BaselineConfig $baseline -GetTargetBaselineFiles {
        param($TargetDir, $BaselineConfig)
        Get-TestCellBaselinePluginFiles -PluginsDir $TargetDir -BaselineConfig $BaselineConfig
    } -DryRunMode $DryRun.IsPresent

    foreach ($name in @($pluginSync.copied)) { $copied.Add($name) | Out-Null }
    foreach ($name in @($pluginSync.updated)) { $updated.Add($name) | Out-Null }
    foreach ($name in @($pluginSync.skipped)) { $skipped.Add($name) | Out-Null }

    if ($sourceBaselineBotMods.Count -gt 0) {
        if (-not (Test-Path -LiteralPath $modsDir)) {
            if (-not $DryRun.IsPresent) {
                New-Item -ItemType Directory -Path $modsDir -Force | Out-Null
            }
        }

        $botModSync = Sync-BaselineFilesToTarget -SourceFiles $sourceBaselineBotMods -TargetDir $modsDir -BaselineConfig $baseline -GetTargetBaselineFiles {
            param($TargetDir, $BaselineConfig)
            Get-TestCellBaselineBotModFiles -ModsDir $TargetDir -BaselineConfig $BaselineConfig
        } -DryRunMode $DryRun.IsPresent

        $botMods = [pscustomobject]@{
            modsDir = $modsDir
            exists = $true
            copied = @($botModSync.copied)
            updated = @($botModSync.updated)
            skipped = @($botModSync.skipped)
            removed = @($botModSync.removed)
        }
    }

    $summaries.Add([pscustomobject]@{
            cellId = $targetCell.id
            pluginsDir = $pluginsDir
            exists = $true
            copied = $copied.ToArray()
            updated = $updated.ToArray()
            skipped = $skipped.ToArray()
            removedFromPlugins = $removedFromPlugins.ToArray()
            removedFromDisabled = $removedFromDisabled.ToArray()
            botMods = $botMods
        }) | Out-Null
}

[pscustomobject]@{
    ok = $true
    dryRun = $DryRun.IsPresent
    configPath = $config.path
    baselineConfigPath = $baseline.path
    baselineId = $baseline.id
    minecraftVersion = $baseline.minecraftVersion
    baselineSourceCellId = $sourceCell.id
    baselineSourcePluginsDir = $sourcePluginsDir
    baselineSourceModsDir = $sourceModsDir
    baselinePatterns = (Get-TestCellBaselinePluginPatterns -BaselineConfig $baseline)
    baselineBotModPatterns = (Get-TestCellBaselineBotModPatterns -BaselineConfig $baseline)
    baselinePluginNames = @($sourceBaselinePlugins | ForEach-Object { $_.Name })
    baselineBotModNames = @($sourceBaselineBotMods | ForEach-Object { $_.Name })
    targets = $summaries.ToArray()
} | ConvertTo-Json -Depth 12
