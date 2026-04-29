param(
    [string]$ConfigPath = '',
    [string]$SourceConfigPath = '',
    [string]$SourceCellId = 'cell-01',
    [string[]]$TargetCellIds = @('cell-21', 'cell-22'),
    [string]$AllowedServerRoot = 'F:/minecraft/test-cells',
    [string]$AllowedClientRoot = 'G:/MC/game/BlackBoxProTestCells',
    [switch]$Force
)

$ErrorActionPreference = 'Stop'
$OutputEncoding = [Console]::OutputEncoding = [Text.UTF8Encoding]::new($false)
if ([string]::IsNullOrWhiteSpace($ConfigPath)) {
    $ConfigPath = Join-Path $PSScriptRoot 'cells-mod1122.json'
}
if ([string]::IsNullOrWhiteSpace($SourceConfigPath)) {
    $SourceConfigPath = Join-Path $PSScriptRoot 'cells.json'
}
. (Join-Path $PSScriptRoot 'TestCellCommon.ps1')

function Get-NormalizedFullPath {
    param([string]$Path)
    return [System.IO.Path]::GetFullPath($Path).TrimEnd([System.IO.Path]::DirectorySeparatorChar, [System.IO.Path]::AltDirectorySeparatorChar)
}

function Assert-PathUnder {
    param(
        [string]$Path,
        [string]$AllowedRoot,
        [string]$Label
    )

    $fullPath = Get-NormalizedFullPath -Path $Path
    $fullRoot = Get-NormalizedFullPath -Path $AllowedRoot
    if (-not ($fullPath.Equals($fullRoot, [System.StringComparison]::OrdinalIgnoreCase) -or $fullPath.StartsWith("$fullRoot$([System.IO.Path]::DirectorySeparatorChar)", [System.StringComparison]::OrdinalIgnoreCase) -or $fullPath.StartsWith("$fullRoot$([System.IO.Path]::AltDirectorySeparatorChar)", [System.StringComparison]::OrdinalIgnoreCase))) {
        throw "$Label must stay under $fullRoot, got: $fullPath"
    }
}

function Remove-IfExists {
    param([string]$Path)
    if (Test-Path -LiteralPath $Path) {
        Remove-Item -LiteralPath $Path -Recurse -Force
    }
}

function New-Junction {
    param(
        [string]$LinkPath,
        [string]$TargetPath
    )

    if (-not (Test-Path -LiteralPath $TargetPath)) {
        throw "Junction target not found: $TargetPath"
    }

    Remove-IfExists -Path $LinkPath
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
        $serverContent = Set-ConfigLine -Content $serverContent -Pattern '(?m)^difficulty=.*$' -Replacement 'difficulty=0'
        $serverContent = Set-ConfigLine -Content $serverContent -Pattern '(?m)^spawn-monsters=.*$' -Replacement 'spawn-monsters=false'
        $serverContent = Set-ConfigLine -Content $serverContent -Pattern '(?m)^level-type=.*$' -Replacement 'level-type=FLAT'
        $serverContent = Set-ConfigLine -Content $serverContent -Pattern '(?m)^generator-settings=.*$' -Replacement 'generator-settings='
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

    $eulaPath = Join-Path $Cell.serverDir 'eula.txt'
    [System.IO.File]::WriteAllText($eulaPath, "eula=true`r`n", [System.Text.ASCIIEncoding]::new())
}

function Clear-RuntimeNoise {
    param([object]$Cell)

    foreach ($relative in @('logs', 'crash-reports')) {
        $path = Join-Path $Cell.serverDir $relative
        if (Test-Path -LiteralPath $path) {
            Get-ChildItem -LiteralPath $path -Force -ErrorAction SilentlyContinue | Remove-Item -Recurse -Force
        }
    }

    foreach ($relative in @('logs', 'crash-reports', 'screenshots')) {
        $path = Join-Path $Cell.botVersionDir $relative
        if (Test-Path -LiteralPath $path) {
            Get-ChildItem -LiteralPath $path -Force -ErrorAction SilentlyContinue | Remove-Item -Recurse -Force
        }
    }
}

$sourceConfig = Load-TestCellConfig -ConfigPath $SourceConfigPath
$targetConfig = Load-TestCellConfig -ConfigPath $ConfigPath
$sourceCell = Get-TestCell -Config $sourceConfig -CellId $SourceCellId

if (-not (Test-Path -LiteralPath $sourceCell.serverDir)) {
    throw "Source server dir not found: $($sourceCell.serverDir)"
}
if (-not (Test-Path -LiteralPath $sourceCell.botVersionDir)) {
    throw "Source client version dir not found: $($sourceCell.botVersionDir)"
}
if (-not (Test-Path -LiteralPath $sourceCell.assetsDir)) {
    throw "Source assets dir not found: $($sourceCell.assetsDir)"
}
if (-not (Test-Path -LiteralPath $sourceCell.librariesDir)) {
    throw "Source libraries dir not found: $($sourceCell.librariesDir)"
}

$provisioned = New-Object System.Collections.Generic.List[object]

foreach ($targetId in $TargetCellIds) {
    $cell = Get-TestCell -Config $targetConfig -CellId $targetId
    Assert-PathUnder -Path $cell.serverDir -AllowedRoot $AllowedServerRoot -Label "$($cell.id) serverDir"
    Assert-PathUnder -Path $cell.botWorkspaceRoot -AllowedRoot $AllowedClientRoot -Label "$($cell.id) botWorkspaceRoot"
    Assert-PathUnder -Path $cell.botVersionDir -AllowedRoot $cell.botWorkspaceRoot -Label "$($cell.id) botVersionDir"
    Assert-PathUnder -Path $cell.assetsDir -AllowedRoot $cell.botWorkspaceRoot -Label "$($cell.id) assetsDir"
    Assert-PathUnder -Path $cell.librariesDir -AllowedRoot $cell.botWorkspaceRoot -Label "$($cell.id) librariesDir"

    if ((Test-Path -LiteralPath $cell.serverDir) -and -not $Force) {
        throw "Target server dir already exists: $($cell.serverDir). Use -Force to overwrite."
    }
    if ((Test-Path -LiteralPath $cell.botWorkspaceRoot) -and -not $Force) {
        throw "Target client workspace already exists: $($cell.botWorkspaceRoot). Use -Force to overwrite."
    }

    Remove-IfExists -Path $cell.serverDir
    Remove-IfExists -Path $cell.botWorkspaceRoot

    Copy-Item -LiteralPath $sourceCell.serverDir -Destination $cell.serverDir -Recurse
    Update-ServerFiles -Cell $cell

    $minecraftRoot = Join-Path $cell.botWorkspaceRoot '.minecraft'
    $versionsRoot = Join-Path $minecraftRoot 'versions'
    New-Item -ItemType Directory -Path $versionsRoot -Force | Out-Null
    Copy-Item -LiteralPath $sourceCell.botVersionDir -Destination $cell.botVersionDir -Recurse
    New-Junction -LinkPath $cell.assetsDir -TargetPath $sourceCell.assetsDir
    New-Junction -LinkPath $cell.librariesDir -TargetPath $sourceCell.librariesDir
    Clear-RuntimeNoise -Cell $cell

    $provisioned.Add([pscustomobject]@{
            id = $cell.id
            sourceCellId = $sourceCell.id
            serverDir = $cell.serverDir
            botWorkspaceRoot = $cell.botWorkspaceRoot
            botVersionDir = $cell.botVersionDir
            serverPort = $cell.serverPort
            pluginHttpPort = $cell.pluginHttpPort
            modHttpPort = $cell.modHttpPort
        }) | Out-Null
}

[pscustomobject]@{
    configPath = $targetConfig.path
    sourceConfigPath = $sourceConfig.path
    sourceCellId = $sourceCell.id
    provisioned = $provisioned
} | ConvertTo-Json -Depth 10
