param(
    [string]$CellId,
    [string]$Owner = "owner-$PID",
    [int]$LeaseMinutes = 120,
    [int]$WaitSeconds = 0,
    [string]$ConfigPath = '',
    [switch]$ReadyOnly
)

$ErrorActionPreference = 'Stop'
$OutputEncoding = [Console]::OutputEncoding = [Text.UTF8Encoding]::new($false)
if ([string]::IsNullOrWhiteSpace($ConfigPath)) {
    $ConfigPath = Join-Path $PSScriptRoot 'cells.json'
}
. (Join-Path $PSScriptRoot 'TestCellCommon.ps1')

$config = Load-TestCellConfig -ConfigPath $ConfigPath
$lease = Acquire-TestCellLease -Config $config -Owner $Owner -CellId $CellId -LeaseMinutes $LeaseMinutes -WaitSeconds $WaitSeconds -ReadyOnly:$ReadyOnly
$lease | ConvertTo-Json -Depth 10
