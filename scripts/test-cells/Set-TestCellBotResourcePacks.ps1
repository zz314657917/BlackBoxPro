param(
    [string[]]$ConfigPath = @(),
    [string[]]$CellId = @(),
    [string]$ResourcePackName = 'Minecraft-Mod-Language-Modpack.zip',
    [string]$SourceResourcePackPath = '',
    [string[]]$ExtraBotVersionDir = @(),
    [switch]$DryRun
)

$ErrorActionPreference = 'Stop'
$OutputEncoding = [Console]::OutputEncoding = [Text.UTF8Encoding]::new($false)

. (Join-Path $PSScriptRoot 'TestCellCommon.ps1')
. (Join-Path $PSScriptRoot 'TestCellBaselinePlugins.ps1')

if ($ConfigPath.Count -eq 0) {
    $ConfigPath = @(
        (Join-Path $PSScriptRoot 'cells.json')
    )
    $mod1122ConfigPath = Join-Path $PSScriptRoot 'cells-mod1122.json'
    if (Test-Path -LiteralPath $mod1122ConfigPath) {
        $ConfigPath += $mod1122ConfigPath
    }
}

$defaultCell10BotVersionDir = 'G:/MC/game/BlackBoxProTestCells/cell-10/.minecraft/versions/bot'
if ($ExtraBotVersionDir.Count -eq 0 -and (Test-Path -LiteralPath $defaultCell10BotVersionDir)) {
    $ExtraBotVersionDir = @($defaultCell10BotVersionDir)
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

$targets = New-Object System.Collections.Generic.List[object]
$seenBotVersionDirs = New-Object System.Collections.Generic.HashSet[string]([System.StringComparer]::OrdinalIgnoreCase)

foreach ($path in @($ConfigPath)) {
    $config = Load-TestCellConfig -ConfigPath $path
    foreach ($cell in @($config.cells)) {
        if ($CellId.Count -gt 0 -and $CellId -notcontains $cell.id) {
            continue
        }
        if (-not $cell.enabled) {
            continue
        }
        $resolvedBotVersionDir = (Resolve-Path -LiteralPath $cell.botVersionDir -ErrorAction SilentlyContinue)
        $botVersionDir = if ($null -ne $resolvedBotVersionDir) {
            $resolvedBotVersionDir.ProviderPath
        } else {
            $cell.botVersionDir
        }
        if ($seenBotVersionDirs.Add($botVersionDir)) {
            $targets.Add([pscustomobject]@{
                    cellId = $cell.id
                    configPath = $config.path
                    botVersionDir = $botVersionDir
                }) | Out-Null
        }
    }
}

$extraIndex = 0
foreach ($dir in @($ExtraBotVersionDir)) {
    $extraIndex++
    $resolvedDir = (Resolve-Path -LiteralPath $dir -ErrorAction SilentlyContinue)
    $botVersionDir = if ($null -ne $resolvedDir) {
        $resolvedDir.ProviderPath
    } else {
        $dir
    }
    if ($seenBotVersionDirs.Add($botVersionDir)) {
        $targets.Add([pscustomobject]@{
                cellId = "extra-$extraIndex"
                configPath = $null
                botVersionDir = $botVersionDir
            }) | Out-Null
    }
}

if ($targets.Count -eq 0) {
    throw 'No target bot version dirs matched.'
}

if ([string]::IsNullOrWhiteSpace($SourceResourcePackPath)) {
    $sourceCandidates = New-Object System.Collections.Generic.List[string]
    $sourceCandidates.Add('G:/MC/game/AAA枫叶大陆服务器/.minecraft/versions/落叶四周目客户端/resourcepacks/' + $ResourcePackName) | Out-Null
    foreach ($target in @($targets.ToArray())) {
        $sourceCandidates.Add((Join-Path (Join-Path $target.botVersionDir 'resourcepacks') $ResourcePackName)) | Out-Null
    }

    foreach ($candidate in @($sourceCandidates.ToArray())) {
        if (Test-Path -LiteralPath $candidate) {
            $SourceResourcePackPath = $candidate
            break
        }
    }
}

if ([string]::IsNullOrWhiteSpace($SourceResourcePackPath) -or -not (Test-Path -LiteralPath $SourceResourcePackPath)) {
    throw "Source resource pack not found: $SourceResourcePackPath"
}

$sourceHash = Get-FileSha256 -Path $SourceResourcePackPath
$summaries = New-Object System.Collections.Generic.List[object]

foreach ($target in @($targets.ToArray())) {
    $resourcePacksDir = Join-Path $target.botVersionDir 'resourcepacks'
    $destinationPath = Join-Path $resourcePacksDir $ResourcePackName
    $copyAction = 'skipped'

    if (-not (Test-Path -LiteralPath $resourcePacksDir)) {
        $copyAction = 'copied'
        if (-not $DryRun.IsPresent) {
            New-Item -ItemType Directory -Path $resourcePacksDir -Force | Out-Null
        }
    }

    if (Test-Path -LiteralPath $destinationPath) {
        if (Test-SameFile -SourcePath $SourceResourcePackPath -DestinationPath $destinationPath) {
            if ($copyAction -ne 'copied') {
                $copyAction = 'skipped'
            }
        } else {
            $copyAction = 'updated'
            if (-not $DryRun.IsPresent) {
                Copy-Item -LiteralPath $SourceResourcePackPath -Destination $destinationPath -Force
            }
        }
    } else {
        $copyAction = 'copied'
        if (-not $DryRun.IsPresent) {
            Copy-Item -LiteralPath $SourceResourcePackPath -Destination $destinationPath -Force
        }
    }

    $options = Set-TestCellBotDefaultResourcePacks -BotVersionDir $target.botVersionDir -ResourcePackNames @($ResourcePackName) -DryRun:$DryRun
    $summaries.Add([pscustomobject]@{
            cellId = $target.cellId
            configPath = $target.configPath
            botVersionDir = $target.botVersionDir
            resourcePacksDir = $resourcePacksDir
            resourcePackPath = $destinationPath
            copyAction = $copyAction
            options = $options
        }) | Out-Null
}

[pscustomobject]@{
    ok = $true
    dryRun = $DryRun.IsPresent
    resourcePackName = $ResourcePackName
    sourceResourcePackPath = (Resolve-Path -LiteralPath $SourceResourcePackPath).ProviderPath
    sourceSha256 = $sourceHash
    targets = $summaries.ToArray()
} | ConvertTo-Json -Depth 12
