param(
    [Parameter(Mandatory = $true)]
    [string]$CellId,
    [string]$Owner = "owner-$PID",
    [string]$ConfigPath = '',
    [switch]$Force
)

$ErrorActionPreference = 'Stop'
$OutputEncoding = [Console]::OutputEncoding = [Text.UTF8Encoding]::new($false)
if ([string]::IsNullOrWhiteSpace($ConfigPath)) {
    $ConfigPath = Join-Path $PSScriptRoot 'cells.json'
}
. (Join-Path $PSScriptRoot 'TestCellCommon.ps1')

$config = Load-TestCellConfig -ConfigPath $ConfigPath
$release = Release-TestCellLease -Config $config -CellId $CellId -Owner $Owner -Force:$Force
$release | ConvertTo-Json -Depth 10
