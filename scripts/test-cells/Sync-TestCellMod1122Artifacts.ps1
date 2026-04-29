param(
    [Parameter(Mandatory = $true)]
    [string[]]$ModJar,
    [string]$ConfigPath = '',
    [string[]]$CellIds = @('cell-20', 'cell-21', 'cell-22'),
    [string[]]$RemoveExistingPattern = @(),
    [switch]$IncludeDisabled
)

$ErrorActionPreference = 'Stop'
$OutputEncoding = [Console]::OutputEncoding = [Text.UTF8Encoding]::new($false)
if ([string]::IsNullOrWhiteSpace($ConfigPath)) {
    $ConfigPath = Join-Path $PSScriptRoot 'cells-mod1122.json'
}
. (Join-Path $PSScriptRoot 'TestCellCommon.ps1')

function Resolve-ModJar {
    param([string]$Path)
    if (-not (Test-Path -LiteralPath $Path)) {
        throw "Mod jar not found: $Path"
    }
    return (Resolve-Path -LiteralPath $Path).ProviderPath
}

function Get-InferredRemovePattern {
    param([string]$JarPath)

    $name = [System.IO.Path]::GetFileNameWithoutExtension($JarPath)
    if ($name -match '^(.+?)-\d') {
        return "$($matches[1])-*.jar"
    }
    if ($name -match '^(.+?)-') {
        return "$($matches[1])-*.jar"
    }
    return "$name.jar"
}

$config = Load-TestCellConfig -ConfigPath $ConfigPath
$targets = if ($CellIds.Count -gt 0) {
    @($CellIds | ForEach-Object { Get-TestCell -Config $config -CellId $_ })
} else {
    @($config.cells | Where-Object { $IncludeDisabled -or $_.enabled })
}

$resolvedJars = @($ModJar | ForEach-Object { Resolve-ModJar -Path $_ })
$patterns = if ($RemoveExistingPattern.Count -gt 0) {
    $RemoveExistingPattern
} else {
    @($resolvedJars | ForEach-Object { Get-InferredRemovePattern -JarPath $_ } | Select-Object -Unique)
}

$synced = New-Object System.Collections.Generic.List[object]

foreach ($cell in $targets) {
    $serverModsDir = Join-Path $cell.serverDir 'mods'
    $clientModsDir = Join-Path $cell.botVersionDir 'mods'

    if (-not (Test-Path -LiteralPath $serverModsDir)) {
        throw "Server mods dir not found for $($cell.id): $serverModsDir"
    }
    if (-not (Test-Path -LiteralPath $clientModsDir)) {
        New-Item -ItemType Directory -Path $clientModsDir -Force | Out-Null
    }

    foreach ($pattern in $patterns) {
        Get-ChildItem -Path $serverModsDir -Filter $pattern -File -ErrorAction SilentlyContinue | Remove-Item -Force
        Get-ChildItem -Path $clientModsDir -Filter $pattern -File -ErrorAction SilentlyContinue | Remove-Item -Force
    }

    $serverCopied = New-Object System.Collections.Generic.List[string]
    $clientCopied = New-Object System.Collections.Generic.List[string]
    foreach ($jar in $resolvedJars) {
        $name = Split-Path -Leaf $jar
        $serverTarget = Join-Path $serverModsDir $name
        $clientTarget = Join-Path $clientModsDir $name
        Copy-Item -LiteralPath $jar -Destination $serverTarget -Force
        Copy-Item -LiteralPath $jar -Destination $clientTarget -Force
        $serverCopied.Add($serverTarget) | Out-Null
        $clientCopied.Add($clientTarget) | Out-Null
    }

    $synced.Add([pscustomobject]@{
            cellId = $cell.id
            removeExistingPattern = $patterns
            serverModsDir = $serverModsDir
            clientModsDir = $clientModsDir
            serverJars = $serverCopied
            clientJars = $clientCopied
        }) | Out-Null
}

[pscustomobject]@{
    configPath = $config.path
    modJars = $resolvedJars
    synced = $synced
} | ConvertTo-Json -Depth 10
