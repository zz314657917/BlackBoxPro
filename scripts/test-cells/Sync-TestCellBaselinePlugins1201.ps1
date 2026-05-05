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
    $ConfigPath = Join-Path $PSScriptRoot 'cells-1201.json'
}
if ([string]::IsNullOrWhiteSpace($BaselineConfigPath)) {
    $BaselineConfigPath = Join-Path $PSScriptRoot 'baselines/baseline-1201.json'
}

$args = @(
    '-ExecutionPolicy', 'Bypass',
    '-File', (Join-Path $PSScriptRoot 'Sync-TestCellBaselinePlugins.ps1'),
    '-ConfigPath', $ConfigPath,
    '-BaselineConfigPath', $BaselineConfigPath
)
if (-not [string]::IsNullOrWhiteSpace($SourceCellId)) {
    $args += @('-SourceCellId', $SourceCellId)
}
if ($TargetCellIds.Count -gt 0) {
    $args += @('-TargetCellIds', $TargetCellIds)
}
if ($DryRun) {
    $args += '-DryRun'
}

powershell @args
