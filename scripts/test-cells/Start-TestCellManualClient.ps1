param(
    [Parameter(Mandatory = $true)]
    [string]$CellId,
    [string]$Owner = '',
    [int]$LeaseMinutes = 240,
    [switch]$DryRun,
    [switch]$NoAutoCleanup
)

$ErrorActionPreference = 'Stop'
$OutputEncoding = [Console]::OutputEncoding = [Text.UTF8Encoding]::new($false)

$ScriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$AcquireScript = Join-Path $ScriptRoot 'Acquire-TestCell.ps1'
$ReleaseScript = Join-Path $ScriptRoot 'Release-TestCell.ps1'

function Invoke-ChildPowerShell {
    param(
        [Parameter(Mandatory = $true)]
        [string]$ScriptPath,
        [string[]]$Arguments = @()
    )

    $commandArgs = @('-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', $ScriptPath) + $Arguments
    $output = & powershell.exe @commandArgs 2>&1
    $exitCode = $LASTEXITCODE
    $text = ($output | ForEach-Object { $_.ToString() }) -join [Environment]::NewLine

    return [pscustomobject]@{
        exitCode = $exitCode
        output = $text
    }
}

function New-IsolatedCell10ConfigPath {
    $runtimeDir = Join-Path $ScriptRoot 'locks/manual-client'
    if (-not (Test-Path -LiteralPath $runtimeDir)) {
        New-Item -ItemType Directory -Path $runtimeDir -Force | Out-Null
    }

    $configPath = Join-Path $runtimeDir 'cells-isolated.json'
    $config = [ordered]@{
        version = 1
        defaultCellId = 'cell-10'
        defaults = [ordered]@{
            enabled = $false
            serverJar = 'catserver.jar'
            serverJava = 'C:/Program Files/Java/jdk1.8.0_481/bin/java.exe'
            serverMinMemoryMb = 512
            serverMaxMemoryMb = 1536
            botJava = 'C:/Program Files/Java/jdk1.8.0_481/bin/javaw.exe'
            botMinMemoryMb = 512
            botMaxMemoryMb = 1024
            botPlayer = 'zzzderk'
            botVersionName = 'bot'
            visitorName = 'zzzderk'
            visitorUuid = '9527a0de-4acf-37c2-adcc-2bf5ef360c6b'
            ownerName = 'summer'
            ownerUuid = '520324a1-0030-3345-abb1-bbb9262f6798'
        }
        cells = @(
            [ordered]@{
                id = 'cell-10'
                label = 'isolated-cell-10'
                enabled = $true
                serverDir = 'F:/minecraft/test-cells/server-cell-10'
                playerDataDir = 'F:/minecraft/test-cells/server-cell-10/plugins/BlackBoxPro/playerdata'
                botWorkspaceRoot = 'G:/MC/game/BlackBoxProTestCells/cell-10'
                botVersionDir = 'G:/MC/game/BlackBoxProTestCells/cell-10/.minecraft/versions/bot'
                assetsDir = 'G:/MC/game/BlackBoxProTestCells/cell-10/.minecraft/assets'
                librariesDir = 'G:/MC/game/BlackBoxProTestCells/cell-10/.minecraft/libraries'
                serverPort = 25655
                pluginHttpPort = 38170
                modHttpPort = 38171
            }
        )
    }

    $json = $config | ConvertTo-Json -Depth 12
    [System.IO.File]::WriteAllText($configPath, $json, [System.Text.UTF8Encoding]::new($false))
    return $configPath
}

function Resolve-ManualCellProfile {
    param([string]$Id)

    if ($Id -match '^cell-0[1-5]$') {
        return [pscustomobject]@{
            configPath = Join-Path $ScriptRoot 'cells.json'
            invokeScriptPath = Join-Path $ScriptRoot 'Invoke-TestCell.ps1'
        }
    }

    if ($Id -match '^cell-0[6-8]$') {
        return [pscustomobject]@{
            configPath = Join-Path $ScriptRoot 'cells-1201.json'
            invokeScriptPath = Join-Path $ScriptRoot 'Invoke-TestCell1201.ps1'
        }
    }

    if ($Id -eq 'cell-10') {
        return [pscustomobject]@{
            configPath = New-IsolatedCell10ConfigPath
            invokeScriptPath = Join-Path $ScriptRoot 'Invoke-TestCell.ps1'
        }
    }

    if ($Id -match '^cell-2[0-2]$') {
        return [pscustomobject]@{
            configPath = Join-Path $ScriptRoot 'cells-mod1122.json'
            invokeScriptPath = Join-Path $ScriptRoot 'Invoke-TestCell.ps1'
        }
    }

    throw "Unsupported manual test cell: $Id"
}

function Invoke-RequiredScript {
    param(
        [string]$Description,
        [string]$ScriptPath,
        [string[]]$Arguments
    )

    Write-Host "==> $Description"
    $result = Invoke-ChildPowerShell -ScriptPath $ScriptPath -Arguments $Arguments
    if (-not [string]::IsNullOrWhiteSpace($result.output)) {
        Write-Host $result.output
    }

    if ($result.exitCode -ne 0) {
        throw "$Description failed with exit code $($result.exitCode)."
    }

    return $result
}

function Invoke-BestEffortScript {
    param(
        [string]$Description,
        [string]$ScriptPath,
        [string[]]$Arguments
    )

    Write-Host "==> $Description"
    $result = Invoke-ChildPowerShell -ScriptPath $ScriptPath -Arguments $Arguments
    if (-not [string]::IsNullOrWhiteSpace($result.output)) {
        Write-Host $result.output
    }
    if ($result.exitCode -ne 0) {
        Write-Warning "$Description failed with exit code $($result.exitCode)."
    }
    return $result
}

$profile = Resolve-ManualCellProfile -Id $CellId
if ([string]::IsNullOrWhiteSpace($Owner)) {
    $Owner = "manual-$CellId-$([DateTimeOffset]::UtcNow.ToUnixTimeSeconds())-$PID"
}

$summary = [ordered]@{
    cellId = $CellId
    owner = $Owner
    configPath = $profile.configPath
    invokeScriptPath = $profile.invokeScriptPath
    leaseMinutes = $LeaseMinutes
    dryRun = [bool]$DryRun
    noAutoCleanup = [bool]$NoAutoCleanup
}

if ($DryRun) {
    $summary | ConvertTo-Json -Depth 6
    return
}

$acquired = $false
$started = $false

try {
    Invoke-RequiredScript -Description "Acquire $CellId" -ScriptPath $AcquireScript -Arguments @(
        '-CellId', $CellId,
        '-Owner', $Owner,
        '-LeaseMinutes', "$LeaseMinutes",
        '-ConfigPath', $profile.configPath,
        '-ReadyOnly'
    ) | Out-Null
    $acquired = $true

    Invoke-RequiredScript -Description "Start $CellId server and visible client" -ScriptPath $profile.invokeScriptPath -Arguments @(
        '-Mode', 'ensure',
        '-CellId', $CellId,
        '-ConfigPath', $profile.configPath,
        '-ShowClient'
    ) | Out-Null
    $started = $true

    Write-Host ''
    Write-Host "Manual test is running for $CellId."
    Write-Host "Use the visible Minecraft client window for testing."

    if ($NoAutoCleanup) {
        Write-Host ''
        Write-Host 'NoAutoCleanup is set. Close later with:'
        Write-Host ('powershell -NoProfile -ExecutionPolicy Bypass -File "{0}" -Mode stop -CellId {1} -ConfigPath "{2}"' -f $profile.invokeScriptPath, $CellId, $profile.configPath)
        Write-Host ('powershell -NoProfile -ExecutionPolicy Bypass -File "{0}" -CellId {1} -Owner "{2}" -ConfigPath "{3}"' -f $ReleaseScript, $CellId, $Owner, $profile.configPath)
        return
    }

    Write-Host ''
    Read-Host 'Press Enter here after hand-testing; this will stop this cell and release the lease'
}
finally {
    if (-not $DryRun -and -not $NoAutoCleanup -and ($acquired -or $started)) {
        Invoke-BestEffortScript -Description "Stop $CellId server and client" -ScriptPath $profile.invokeScriptPath -Arguments @(
            '-Mode', 'stop',
            '-CellId', $CellId,
            '-ConfigPath', $profile.configPath
        ) | Out-Null
    }

    if (-not $DryRun -and -not $NoAutoCleanup -and $acquired) {
        Invoke-BestEffortScript -Description "Release $CellId" -ScriptPath $ReleaseScript -Arguments @(
            '-CellId', $CellId,
            '-Owner', $Owner,
            '-ConfigPath', $profile.configPath
        ) | Out-Null
    }
}
