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
    $botResourcePackPatterns = if ($null -ne $json.botResourcePackPatterns) {
        @($json.botResourcePackPatterns)
    } else {
        @()
    }
    $botDefaultResourcePacks = if ($null -ne $json.botDefaultResourcePacks) {
        @($json.botDefaultResourcePacks)
    } else {
        @()
    }
    return [pscustomobject]@{
        path = (Resolve-Path -LiteralPath $BaselineConfigPath).ProviderPath
        id = $json.id
        minecraftVersion = $json.minecraftVersion
        defaultCellConfig = $json.defaultCellConfig
        sourceCellId = $json.sourceCellId
        targetCellIds = @($json.targetCellIds)
        pluginPatterns = @($json.pluginPatterns)
        botModPatterns = @($json.botModPatterns)
        botResourcePackPatterns = $botResourcePackPatterns
        botDefaultResourcePacks = $botDefaultResourcePacks
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

function Get-TestCellBaselineBotResourcePackPatterns {
    param([object]$BaselineConfig)
    return @($BaselineConfig.botResourcePackPatterns)
}

function Get-TestCellBaselineBotDefaultResourcePacks {
    param([object]$BaselineConfig)
    return @($BaselineConfig.botDefaultResourcePacks)
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

function Test-IsTestCellBaselineBotResourcePack {
    param(
        [string]$Name,
        [object]$BaselineConfig
    )

    foreach ($pattern in (Get-TestCellBaselineBotResourcePackPatterns -BaselineConfig $BaselineConfig)) {
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

function Get-TestCellBaselineBotResourcePackFiles {
    param(
        [string]$ResourcePacksDir,
        [object]$BaselineConfig
    )

    if (-not (Test-Path -LiteralPath $ResourcePacksDir)) {
        return @()
    }

    return @(Get-ChildItem -LiteralPath $ResourcePacksDir -File -ErrorAction SilentlyContinue |
        Where-Object { Test-IsTestCellBaselineBotResourcePack -Name $_.Name -BaselineConfig $BaselineConfig })
}

function ConvertFrom-TestCellMinecraftOptionsListLiteral {
    param([string]$Value)

    if ([string]::IsNullOrWhiteSpace($Value) -or $Value.Trim() -eq '[]') {
        return @()
    }

    try {
        $parsed = $Value | ConvertFrom-Json -ErrorAction Stop
        if ($null -eq $parsed) {
            return @()
        }
        return @($parsed | ForEach-Object { [string]$_ })
    } catch {
        throw "Failed to parse Minecraft options list: $Value"
    }
}

function ConvertTo-TestCellMinecraftOptionsListLiteral {
    param([string[]]$Values)

    $escapedValues = New-Object System.Collections.Generic.List[string]
    foreach ($value in @($Values)) {
        $escaped = ([string]$value).Replace('\', '\\').Replace('"', '\"')
        $escapedValues.Add('"' + $escaped + '"') | Out-Null
    }
    return '[' + ($escapedValues.ToArray() -join ',') + ']'
}

function Get-TestCellOptionsLineValue {
    param(
        [string[]]$Lines,
        [string]$Key
    )

    foreach ($line in @($Lines)) {
        if ($line.StartsWith("${Key}:")) {
            return $line.Substring($Key.Length + 1)
        }
    }
    return $null
}

function Set-TestCellOptionsListValue {
    param(
        [string[]]$Lines,
        [string]$Key,
        [string[]]$Values
    )

    $literal = ConvertTo-TestCellMinecraftOptionsListLiteral -Values $Values
    $found = $false
    $changed = $false
    $newLines = New-Object System.Collections.Generic.List[string]

    foreach ($line in @($Lines)) {
        if ($line.StartsWith("${Key}:")) {
            $found = $true
            $nextLine = "${Key}:$literal"
            if ($line -ne $nextLine) {
                $changed = $true
            }
            $newLines.Add($nextLine) | Out-Null
        } else {
            $newLines.Add($line) | Out-Null
        }
    }

    if (-not $found) {
        $newLines.Add("${Key}:$literal") | Out-Null
        $changed = $true
    }

    return [pscustomobject]@{
        lines = $newLines.ToArray()
        changed = $changed
    }
}

function Set-TestCellBotDefaultResourcePacks {
    param(
        [string]$BotVersionDir,
        [string[]]$ResourcePackNames,
        [switch]$DryRun
    )

    $optionsPath = Join-Path $BotVersionDir 'options.txt'
    $desiredPackIds = New-Object System.Collections.Generic.List[string]
    foreach ($name in @($ResourcePackNames)) {
        if ([string]::IsNullOrWhiteSpace($name)) {
            continue
        }
        if ($name -like 'file/*') {
            $desiredPackIds.Add($name) | Out-Null
        } else {
            $desiredPackIds.Add("file/$name") | Out-Null
        }
    }

    if ($desiredPackIds.Count -eq 0) {
        return [pscustomobject]@{
            optionsPath = $optionsPath
            exists = (Test-Path -LiteralPath $optionsPath)
            changed = $false
            desired = @()
            before = @()
            after = @()
        }
    }

    if (-not (Test-Path -LiteralPath $optionsPath)) {
        return [pscustomobject]@{
            optionsPath = $optionsPath
            exists = $false
            changed = $false
            desired = $desiredPackIds.ToArray()
            before = @()
            after = @()
        }
    }

    $lines = [System.IO.File]::ReadAllLines($optionsPath, [System.Text.Encoding]::UTF8)
    $currentValue = Get-TestCellOptionsLineValue -Lines $lines -Key 'resourcePacks'
    $currentPacks = if ($null -eq $currentValue) {
        @()
    } else {
        @(ConvertFrom-TestCellMinecraftOptionsListLiteral -Value $currentValue)
    }

    $seen = New-Object System.Collections.Generic.HashSet[string]([System.StringComparer]::OrdinalIgnoreCase)
    $nextPacks = New-Object System.Collections.Generic.List[string]
    foreach ($pack in @($currentPacks)) {
        if ([string]::IsNullOrWhiteSpace($pack)) {
            continue
        }

        $normalizedPack = $pack
        foreach ($name in @($ResourcePackNames)) {
            if ($pack -ieq $name) {
                $normalizedPack = "file/$name"
                break
            }
        }

        if ($seen.Add($normalizedPack)) {
            $nextPacks.Add($normalizedPack) | Out-Null
        }
    }
    foreach ($pack in @($desiredPackIds.ToArray())) {
        if ($seen.Add($pack)) {
            $nextPacks.Add($pack) | Out-Null
        }
    }

    $update = Set-TestCellOptionsListValue -Lines $lines -Key 'resourcePacks' -Values $nextPacks.ToArray()
    if ($update.changed -and -not $DryRun.IsPresent) {
        $utf8NoBom = New-Object System.Text.UTF8Encoding($false)
        [System.IO.File]::WriteAllLines($optionsPath, [string[]]$update.lines, $utf8NoBom)
    }

    return [pscustomobject]@{
        optionsPath = $optionsPath
        exists = $true
        changed = $update.changed
        desired = $desiredPackIds.ToArray()
        before = @($currentPacks)
        after = @($nextPacks.ToArray())
    }
}
