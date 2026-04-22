param(
    [string]$ConfigPath = '',
    [string[]]$CellIds = @(),
    [switch]$IncludeDisabled
)

$ErrorActionPreference = 'Stop'
$OutputEncoding = [Console]::OutputEncoding = [Text.UTF8Encoding]::new($false)
if ([string]::IsNullOrWhiteSpace($ConfigPath)) {
    $ConfigPath = Join-Path $PSScriptRoot 'cells-1201.json'
}
. (Join-Path $PSScriptRoot 'TestCellCommon.ps1')

$repoRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)

function Get-LatestArtifact {
    param(
        [string]$Directory,
        [string]$Filter,
        [string[]]$ExcludeNamePatterns = @()
    )

    $artifact = Get-ChildItem -Path $Directory -Filter $Filter -File -ErrorAction SilentlyContinue |
        Where-Object {
            $name = $_.Name
            -not ($ExcludeNamePatterns | Where-Object { $name -like $_ } | Select-Object -First 1)
        } |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1
    if ($null -eq $artifact) {
        throw "Artifact not found: $Directory/$Filter"
    }
    return $artifact
}

$config = Load-TestCellConfig -ConfigPath $ConfigPath
$targets = if ($CellIds.Count -gt 0) {
    @($CellIds | ForEach-Object { Get-TestCell -Config $config -CellId $_ })
} else {
    @($config.cells | Where-Object { $IncludeDisabled -or $_.enabled })
}

$pluginArtifact = Get-LatestArtifact -Directory (Join-Path $repoRoot 'plugin/build/libs') -Filter 'BlackBoxPro-Plugin-*.jar'
$clientArtifact = Get-LatestArtifact `
    -Directory (Join-Path $repoRoot 'build/libs') `
    -Filter 'BlackBoxPro-forge-1.20.1-*.jar' `
    -ExcludeNamePatterns @('*-dev-run.jar')

$synced = New-Object System.Collections.Generic.List[object]

foreach ($cell in $targets) {
    $pluginsDir = Join-Path $cell.serverDir 'plugins'
    $modsDir = Join-Path $cell.botVersionDir 'mods'

    if (-not (Test-Path -LiteralPath $pluginsDir)) {
        throw "Plugins dir not found for $($cell.id): $pluginsDir"
    }
    if (-not (Test-Path -LiteralPath $modsDir)) {
        New-Item -ItemType Directory -Path $modsDir -Force | Out-Null
    }

    Get-ChildItem -Path $pluginsDir -Filter 'BlackBoxPro-Plugin-*.jar' -File -ErrorAction SilentlyContinue |
        Remove-Item -Force
    Copy-Item -LiteralPath $pluginArtifact.FullName -Destination (Join-Path $pluginsDir $pluginArtifact.Name) -Force

    Get-ChildItem -Path $modsDir -File -ErrorAction SilentlyContinue | Remove-Item -Force
    Copy-Item -LiteralPath $clientArtifact.FullName -Destination (Join-Path $modsDir $clientArtifact.Name) -Force

    $synced.Add([pscustomobject]@{
            cellId = $cell.id
            pluginJar = Join-Path $pluginsDir $pluginArtifact.Name
            modJar = Join-Path $modsDir $clientArtifact.Name
        }) | Out-Null
}

[pscustomobject]@{
    configPath = $config.path
    pluginArtifact = $pluginArtifact.FullName
    clientArtifact = $clientArtifact.FullName
    synced = $synced
} | ConvertTo-Json -Depth 10
