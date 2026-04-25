param(
    [string]$ConfigPath = '',
    # cell-01 is the current baseline source for provisioning managed 1.12.2 test-cells unless overridden.
    [string]$SourceCellId = 'cell-01',
    [string[]]$TargetCellIds = @('cell-02', 'cell-03', 'cell-04', 'cell-05'),
    [switch]$UseJunctionAssetsLibraries,
    [switch]$Force
)

$ErrorActionPreference = 'Stop'
$OutputEncoding = [Console]::OutputEncoding = [Text.UTF8Encoding]::new($false)
if ([string]::IsNullOrWhiteSpace($ConfigPath)) {
    $ConfigPath = Join-Path $PSScriptRoot 'cells.json'
}
. (Join-Path $PSScriptRoot 'TestCellCommon.ps1')

function New-Junction {
    param(
        [string]$LinkPath,
        [string]$TargetPath
    )

    if (Test-Path -LiteralPath $LinkPath) {
        Remove-Item -LiteralPath $LinkPath -Recurse -Force
    }

    $parent = Split-Path -Parent $LinkPath
    if (-not (Test-Path -LiteralPath $parent)) {
        New-Item -ItemType Directory -Path $parent | Out-Null
    }

    cmd /c "mklink /J `"$LinkPath`" `"$TargetPath`"" | Out-Null
}

function Minimize-BotMods {
    param([object]$Cell)

    $modsDir = Join-Path $Cell.botVersionDir 'mods'
    if (-not (Test-Path -LiteralPath $modsDir)) {
        return
    }

    $disabledDir = Join-Path $modsDir '_disabled-by-codex'
    if (-not (Test-Path -LiteralPath $disabledDir)) {
        New-Item -ItemType Directory -Path $disabledDir | Out-Null
    }

    Get-ChildItem -LiteralPath $modsDir -File |
        Where-Object { $_.Name -notlike 'BlackBoxPro-forge-1.12.2-*.jar' -and $_.Name -notlike 'GermMod*.jar' } |
        ForEach-Object {
            Move-Item -LiteralPath $_.FullName -Destination (Join-Path $disabledDir $_.Name) -Force
        }
}

function Update-ServerConfig {
    param([object]$Cell)

    $configPath = Join-Path $Cell.serverDir 'plugins/BlackBoxPro/config.yml'
    if (-not (Test-Path -LiteralPath $configPath)) {
        return
    }

    $pluginConfig = @(
        '# BlackBoxPro plugin config'
        'debug: false'
        'response-timeout-ms: 10000'
        "http-port: $($Cell.pluginHttpPort)"
        'test-mode: dual'
        "mod-http-address: `"http://localhost:$($Cell.modHttpPort)`""
        ''
    )
    [System.IO.File]::WriteAllLines($configPath, $pluginConfig, [System.Text.UTF8Encoding]::new($false))

    $serverPropertiesPath = Join-Path $Cell.serverDir 'server.properties'
    if (Test-Path -LiteralPath $serverPropertiesPath) {
        $serverContent = Get-Content -Raw -LiteralPath $serverPropertiesPath
        $serverContent = [regex]::Replace($serverContent, '(?m)^server-port=\d+\s*$', "server-port=$($Cell.serverPort)")
        $serverContent = [regex]::Replace($serverContent, '(?m)^motd=.*$', "motd=BlackBoxPro $($Cell.id)")
        $serverContent = [regex]::Replace($serverContent, '(?m)^difficulty=.*$', 'difficulty=0')
        $serverContent = [regex]::Replace($serverContent, '(?m)^spawn-monsters=.*$', 'spawn-monsters=false')
        [System.IO.File]::WriteAllText($serverPropertiesPath, $serverContent, [System.Text.UTF8Encoding]::new($false))
    }
}

function Minimize-ServerPlugins {
    param(
        [object]$Config,
        [object]$Cell
    )

    $scriptPath = Join-Path $PSScriptRoot 'Minimize-TestCellServerPlugins.ps1'
    if (-not (Test-Path -LiteralPath $scriptPath)) {
        throw "Plugin minimizer script not found: $scriptPath"
    }

    $null = & $scriptPath -ConfigPath $Config.path -CellIds @($Cell.id) -IncludeDisabled
}

function Save-Config {
    param([object]$ConfigObject)

    $payload = [ordered]@{
        version = $ConfigObject.version
        defaultCellId = $ConfigObject.defaultCellId
        defaults = ConvertTo-FlatHashtable -InputObject $ConfigObject.defaults
        cells = @($ConfigObject.cells | ForEach-Object { ConvertTo-FlatHashtable -InputObject $_ })
    } | ConvertTo-Json -Depth 20

    [System.IO.File]::WriteAllText($ConfigObject.path, $payload, [System.Text.UTF8Encoding]::new($false))
}

$config = Load-TestCellConfig -ConfigPath $ConfigPath
$sourceCell = Get-TestCell -Config $config -CellId $SourceCellId

if (-not (Test-TestCellPathsReady -Cell $sourceCell)) {
    throw "Source cell is not ready: $SourceCellId"
}

$provisioned = @()

foreach ($targetId in $TargetCellIds) {
    $targetCell = Get-TestCell -Config $config -CellId $targetId

    if ((Test-Path -LiteralPath $targetCell.serverDir) -and -not $Force) {
        throw "Target server dir already exists: $($targetCell.serverDir). Use -Force to overwrite."
    }
    if ((Test-Path -LiteralPath $targetCell.botWorkspaceRoot) -and -not $Force) {
        throw "Target bot workspace already exists: $($targetCell.botWorkspaceRoot). Use -Force to overwrite."
    }

    if (Test-Path -LiteralPath $targetCell.serverDir) {
        Remove-Item -LiteralPath $targetCell.serverDir -Recurse -Force
    }
    if (Test-Path -LiteralPath $targetCell.botWorkspaceRoot) {
        Remove-Item -LiteralPath $targetCell.botWorkspaceRoot -Recurse -Force
    }

    Copy-Item -LiteralPath $sourceCell.serverDir -Destination $targetCell.serverDir -Recurse
    Update-ServerConfig -Cell $targetCell
    Minimize-ServerPlugins -Config $config -Cell $targetCell

    $targetMinecraftRoot = Join-Path $targetCell.botWorkspaceRoot '.minecraft'
    New-Item -ItemType Directory -Path $targetMinecraftRoot -Force | Out-Null
    New-Item -ItemType Directory -Path (Split-Path -Parent $targetCell.botVersionDir) -Force | Out-Null

    Copy-Item -LiteralPath $sourceCell.botVersionDir -Destination $targetCell.botVersionDir -Recurse
    Minimize-BotMods -Cell $targetCell

    if ($UseJunctionAssetsLibraries) {
        New-Junction -LinkPath $targetCell.assetsDir -TargetPath $sourceCell.assetsDir
        New-Junction -LinkPath $targetCell.librariesDir -TargetPath $sourceCell.librariesDir
    } else {
        Copy-Item -LiteralPath $sourceCell.assetsDir -Destination $targetCell.assetsDir -Recurse
        Copy-Item -LiteralPath $sourceCell.librariesDir -Destination $targetCell.librariesDir -Recurse
    }

    $targetCell.enabled = $true
    $provisioned += [pscustomobject]@{
        id = $targetCell.id
        serverDir = $targetCell.serverDir
        botWorkspaceRoot = $targetCell.botWorkspaceRoot
        pluginHttpPort = $targetCell.pluginHttpPort
        modHttpPort = $targetCell.modHttpPort
    }
}

Save-Config -ConfigObject $config

[pscustomobject]@{
    sourceCellId = $sourceCell.id
    baselineSourceCellId = $sourceCell.id
    baselineSourceServerDir = $sourceCell.serverDir
    baselineSourcePluginsDir = (Join-Path $sourceCell.serverDir 'plugins')
    baselineSourceNote = 'cell-01 is the current baseline source for managed 1.12.2 test-cells unless SourceCellId is overridden.'
    provisioned = $provisioned
    configPath = $config.path
    usedJunctionAssetsLibraries = $UseJunctionAssetsLibraries.IsPresent
} | ConvertTo-Json -Depth 10
