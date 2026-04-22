param(
    [string]$ConfigPath = '',
    [string]$SourceServerDir = 'G:/MC/A1.20.1/test1',
    [string]$SourceVersionDir = '',
    [string[]]$TargetCellIds = @('cell-06', 'cell-07', 'cell-08'),
    [switch]$Force
)

$ErrorActionPreference = 'Stop'
$OutputEncoding = [Console]::OutputEncoding = [Text.UTF8Encoding]::new($false)
if ([string]::IsNullOrWhiteSpace($ConfigPath)) {
    $ConfigPath = Join-Path $PSScriptRoot 'cells-1201.json'
}
. (Join-Path $PSScriptRoot 'TestCellCommon.ps1')

function New-Junction {
    param(
        [string]$LinkPath,
        [string]$TargetPath
    )

    if (-not (Test-Path -LiteralPath $TargetPath)) {
        throw "Junction target not found: $TargetPath"
    }

    if (Test-Path -LiteralPath $LinkPath) {
        Remove-Item -LiteralPath $LinkPath -Recurse -Force
    }

    $parent = Split-Path -Parent $LinkPath
    if (-not (Test-Path -LiteralPath $parent)) {
        New-Item -ItemType Directory -Path $parent -Force | Out-Null
    }

    cmd /c "mklink /J `"$LinkPath`" `"$TargetPath`"" | Out-Null
}

function Set-ConfigLine {
    param(
        [string]$Content,
        [string]$Pattern,
        [string]$Replacement
    )

    if ($Content -match $Pattern) {
        return [regex]::Replace($Content, $Pattern, $Replacement)
    }

    if (-not $Content.EndsWith("`n")) {
        $Content += "`r`n"
    }
    return $Content + $Replacement + "`r`n"
}

function Update-ServerFiles {
    param([object]$Cell)

    $serverPropertiesPath = Join-Path $Cell.serverDir 'server.properties'
    if (Test-Path -LiteralPath $serverPropertiesPath) {
        $serverContent = Get-Content -Raw -LiteralPath $serverPropertiesPath
        $serverContent = Set-ConfigLine -Content $serverContent -Pattern '(?m)^server-port=.*$' -Replacement "server-port=$($Cell.serverPort)"
        $serverContent = Set-ConfigLine -Content $serverContent -Pattern '(?m)^online-mode=.*$' -Replacement 'online-mode=false'
        $serverContent = Set-ConfigLine -Content $serverContent -Pattern '(?m)^enforce-secure-profile=.*$' -Replacement 'enforce-secure-profile=false'
        [System.IO.File]::WriteAllText($serverPropertiesPath, $serverContent, [System.Text.UTF8Encoding]::new($false))
    }

    $pluginConfigPath = Join-Path $Cell.serverDir 'plugins/BlackBoxPro/config.yml'
    if (Test-Path -LiteralPath $pluginConfigPath) {
        $pluginConfig = @(
            '# BlackBoxPro plugin config'
            'debug: false'
            'response-timeout-ms: 10000'
            "http-port: $($Cell.pluginHttpPort)"
            'test-mode: dual'
            "mod-http-address: `"http://localhost:$($Cell.modHttpPort)`""
            ''
        )
        [System.IO.File]::WriteAllLines($pluginConfigPath, $pluginConfig, [System.Text.UTF8Encoding]::new($false))
    }
}

function Remove-IfExists {
    param([string]$Path)
    if (Test-Path -LiteralPath $Path) {
        Remove-Item -LiteralPath $Path -Recurse -Force
    }
}

function Resolve-SourceVersionDir {
    param([string]$ConfiguredPath)

    if (-not [string]::IsNullOrWhiteSpace($ConfiguredPath)) {
        return $ConfiguredPath
    }

    $gameRoot = 'G:/MC/game'
    if (-not (Test-Path -LiteralPath $gameRoot)) {
        throw "Game root not found: $gameRoot"
    }

    $candidate = Get-ChildItem -LiteralPath $gameRoot -Directory -ErrorAction SilentlyContinue |
        ForEach-Object { Join-Path $_.FullName '.minecraft/versions/1.20.1-Forge_47.3.0' } |
        Where-Object { Test-Path -LiteralPath $_ } |
        Select-Object -First 1

    if ($candidate) {
        return $candidate
    }

    throw 'Source client version dir not found under G:/MC/game/*/.minecraft/versions/1.20.1-Forge_47.3.0'
}

function Minimize-ClientVersion {
    param([object]$Cell)

    $modsDir = Join-Path $Cell.botVersionDir 'mods'
    if (-not (Test-Path -LiteralPath $modsDir)) {
        New-Item -ItemType Directory -Path $modsDir -Force | Out-Null
    }
    Get-ChildItem -LiteralPath $modsDir -File -ErrorAction SilentlyContinue | Remove-Item -Force

    foreach ($sidecar in @('journeymap', 'patchouli_books', 'ldlib', 'local', 'tlm_custom_pack')) {
        Remove-IfExists -Path (Join-Path $Cell.botVersionDir $sidecar)
    }
}

$config = Load-TestCellConfig -ConfigPath $ConfigPath
$SourceVersionDir = Resolve-SourceVersionDir -ConfiguredPath $SourceVersionDir

if (-not (Test-Path -LiteralPath $SourceServerDir)) {
    throw "Source server dir not found: $SourceServerDir"
}
if (-not (Test-Path -LiteralPath $SourceVersionDir)) {
    throw "Source client version dir not found: $SourceVersionDir"
}

$sourceVersionsRoot = Split-Path -Parent $SourceVersionDir
$sourceMinecraftRoot = Split-Path -Parent $sourceVersionsRoot
$assetsSource = Join-Path $sourceMinecraftRoot 'assets'
$librariesSource = Join-Path $sourceMinecraftRoot 'libraries'
if (-not (Test-Path -LiteralPath $assetsSource)) {
    throw "Assets source not found: $assetsSource"
}
if (-not (Test-Path -LiteralPath $librariesSource)) {
    throw "Libraries source not found: $librariesSource"
}

$provisioned = New-Object System.Collections.Generic.List[object]

foreach ($targetId in $TargetCellIds) {
    $cell = Get-TestCell -Config $config -CellId $targetId

    if ((Test-Path -LiteralPath $cell.serverDir) -and -not $Force) {
        throw "Target server dir already exists: $($cell.serverDir). Use -Force to overwrite."
    }
    if ((Test-Path -LiteralPath $cell.botWorkspaceRoot) -and -not $Force) {
        throw "Target client workspace already exists: $($cell.botWorkspaceRoot). Use -Force to overwrite."
    }

    Remove-IfExists -Path $cell.serverDir
    Remove-IfExists -Path $cell.botWorkspaceRoot

    Copy-Item -LiteralPath $SourceServerDir -Destination $cell.serverDir -Recurse
    Update-ServerFiles -Cell $cell

    $minecraftRoot = Join-Path $cell.botWorkspaceRoot '.minecraft'
    $versionsRoot = Join-Path $minecraftRoot 'versions'
    New-Item -ItemType Directory -Path $versionsRoot -Force | Out-Null
    Copy-Item -LiteralPath $SourceVersionDir -Destination $cell.botVersionDir -Recurse
    Minimize-ClientVersion -Cell $cell

    New-Junction -LinkPath $cell.assetsDir -TargetPath $assetsSource
    New-Junction -LinkPath $cell.librariesDir -TargetPath $librariesSource

    $provisioned.Add([pscustomobject]@{
            id = $cell.id
            serverDir = $cell.serverDir
            botWorkspaceRoot = $cell.botWorkspaceRoot
            botVersionDir = $cell.botVersionDir
            serverPort = $cell.serverPort
            pluginHttpPort = $cell.pluginHttpPort
            modHttpPort = $cell.modHttpPort
        }) | Out-Null
}

[pscustomobject]@{
    configPath = $config.path
    sourceServerDir = $SourceServerDir
    sourceVersionDir = $SourceVersionDir
    provisioned = $provisioned
} | ConvertTo-Json -Depth 10
