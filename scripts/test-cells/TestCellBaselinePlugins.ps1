$Script:TestCellBaselineConfigDir = Join-Path $PSScriptRoot 'baselines'

function Get-DefaultTestCellBaselineConfigPath {
    param([string]$CellConfigPath = '')

    $configName = if ([string]::IsNullOrWhiteSpace($CellConfigPath)) {
        'cells.json'
    } else {
        Split-Path -Leaf $CellConfigPath
    }

    $baselineName = if ($configName -ieq 'cells-1201.json') {
        'baseline-1201.json'
    } else {
        'baseline-1122.json'
    }

    return Join-Path $Script:TestCellBaselineConfigDir $baselineName
}

function Load-TestCellBaselineConfig {
    param(
        [string]$BaselineConfigPath = '',
        [string]$CellConfigPath = ''
    )

    if ([string]::IsNullOrWhiteSpace($BaselineConfigPath)) {
        $BaselineConfigPath = Get-DefaultTestCellBaselineConfigPath -CellConfigPath $CellConfigPath
    }

    if (-not (Test-Path -LiteralPath $BaselineConfigPath)) {
        throw "Baseline config not found: $BaselineConfigPath"
    }

    $json = Get-Content -Raw -Encoding UTF8 -LiteralPath $BaselineConfigPath | ConvertFrom-Json
    return [pscustomobject]@{
        path = (Resolve-Path -LiteralPath $BaselineConfigPath).ProviderPath
        id = $json.id
        minecraftVersion = $json.minecraftVersion
        defaultCellConfig = $json.defaultCellConfig
        sourceCellId = $json.sourceCellId
        targetCellIds = @($json.targetCellIds)
        pluginPatterns = @($json.pluginPatterns)
        botModPatterns = @($json.botModPatterns)
    }
}

function Get-TestCellBaselinePluginPatterns {
    param([object]$BaselineConfig)
    return @($BaselineConfig.pluginPatterns)
}

function Get-TestCellBaselineBotModPatterns {
    param([object]$BaselineConfig)
    return @($BaselineConfig.botModPatterns)
}

function Test-IsTestCellBaselinePlugin {
    param(
        [string]$Name,
        [object]$BaselineConfig
    )

    foreach ($pattern in (Get-TestCellBaselinePluginPatterns -BaselineConfig $BaselineConfig)) {
        if ($Name -imatch $pattern) {
            return $true
        }
    }
    return $false
}

function Test-IsTestCellBaselineBotMod {
    param(
        [string]$Name,
        [object]$BaselineConfig
    )

    foreach ($pattern in (Get-TestCellBaselineBotModPatterns -BaselineConfig $BaselineConfig)) {
        if ($Name -imatch $pattern) {
            return $true
        }
    }
    return $false
}

function Get-TestCellBaselinePluginFiles {
    param(
        [string]$PluginsDir,
        [object]$BaselineConfig
    )

    if (-not (Test-Path -LiteralPath $PluginsDir)) {
        return @()
    }

    return @(Get-ChildItem -LiteralPath $PluginsDir -File -Filter '*.jar' -ErrorAction SilentlyContinue |
        Where-Object { Test-IsTestCellBaselinePlugin -Name $_.Name -BaselineConfig $BaselineConfig })
}

function Get-TestCellBaselineBotModFiles {
    param(
        [string]$ModsDir,
        [object]$BaselineConfig
    )

    if (-not (Test-Path -LiteralPath $ModsDir)) {
        return @()
    }

    return @(Get-ChildItem -LiteralPath $ModsDir -File -Filter '*.jar' -ErrorAction SilentlyContinue |
        Where-Object { Test-IsTestCellBaselineBotMod -Name $_.Name -BaselineConfig $BaselineConfig })
}
