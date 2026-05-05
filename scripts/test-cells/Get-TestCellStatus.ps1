param(
    [string]$ConfigPath = ''
)

$ErrorActionPreference = 'Stop'
$OutputEncoding = [Console]::OutputEncoding = [Text.UTF8Encoding]::new($false)
if ([string]::IsNullOrWhiteSpace($ConfigPath)) {
    $ConfigPath = Join-Path $PSScriptRoot 'cells.json'
}
. (Join-Path $PSScriptRoot 'TestCellCommon.ps1')

$config = Load-TestCellConfig -ConfigPath $ConfigPath
[pscustomobject]@{
    configPath = $config.path
    defaultCellId = $config.defaultCellId
    cells = @(Get-TestCellStatusEntries -Config $config)
} | ConvertTo-Json -Depth 10
