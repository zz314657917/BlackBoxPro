### PASS: bbp-sprint-005-mod1122-matrix-evidence

## Verdict

Sprint 5 runtime matrix passed. `cell-20`, `cell-21`, and `cell-22` all completed startup and smoke with cleanup clean. `cell-21` was repaired by reprovisioning from the `cell-20` baseline and clearing a stale smoke lease before the successful reruns.

## Commands Run

```powershell
Test-Path "F:/mcplugins/mod/CloudStorage/build/libs/cloudstorage-0.1.0-SNAPSHOT.jar"
powershell -ExecutionPolicy Bypass -File "scripts/test-cells/Run-TestCellMod1122Regression.ps1" -Scope startup -CellId cell-20 -AcquireCell -LeaseOwner "bbp-sprint-005-cell-20-startup" -ModJar "F:/mcplugins/mod/CloudStorage/build/libs/cloudstorage-0.1.0-SNAPSHOT.jar"
powershell -ExecutionPolicy Bypass -File "scripts/test-cells/Run-TestCellMod1122Regression.ps1" -Scope smoke -CellId cell-20 -AcquireCell -LeaseOwner "bbp-sprint-005-cell-20-smoke" -ModJar "F:/mcplugins/mod/CloudStorage/build/libs/cloudstorage-0.1.0-SNAPSHOT.jar"
powershell -ExecutionPolicy Bypass -File "scripts/test-cells/Run-TestCellMod1122Regression.ps1" -Scope startup -CellId cell-21 -AcquireCell -LeaseOwner "bbp-sprint-005-cell-21-startup" -ModJar "F:/mcplugins/mod/CloudStorage/build/libs/cloudstorage-0.1.0-SNAPSHOT.jar"
Get-NetTCPConnection -State Listen -ErrorAction SilentlyContinue | Where-Object { $_.LocalPort -in 25720,38200,38201,25721,38210,38211,25722,38220,38221 } | Select-Object LocalAddress,LocalPort,OwningProcess
Get-CimInstance Win32_Process | Where-Object { $_.Name -in "cmd.exe","java.exe","javaw.exe" -and ($_.CommandLine -like "*BlackBoxProTestCells*cell-2*" -or $_.CommandLine -like "*server-cell-mod1122-2*") } | Select-Object ProcessId,Name,CommandLine
powershell -NoProfile -ExecutionPolicy Bypass -File "scripts/test-cells/Provision-TestCellMod1122.ps1" -ConfigPath "scripts/test-cells/cells-mod1122.json" -SourceConfigPath "scripts/test-cells/cells-mod1122.json" -SourceCellId "cell-20" -TargetCellIds "cell-21" -Force
powershell -NoProfile -ExecutionPolicy Bypass -File "scripts/test-cells/Release-TestCell.ps1" -ConfigPath "scripts/test-cells/cells-mod1122.json" -CellId cell-21 -Owner "bbp-sprint-005-cell-21-smoke-retest2"
powershell -ExecutionPolicy Bypass -File "scripts/test-cells/Run-TestCellMod1122Regression.ps1" -Scope smoke -CellId cell-21 -AcquireCell -LeaseOwner "bbp-sprint-005-cell-21-smoke-retry3" -ModJar "F:/mcplugins/mod/CloudStorage/build/libs/cloudstorage-0.1.0-SNAPSHOT.jar"
powershell -ExecutionPolicy Bypass -File "scripts/test-cells/Run-TestCellMod1122Regression.ps1" -Scope startup -CellId cell-22 -AcquireCell -LeaseOwner "bbp-sprint-005-cell-22-startup-retry2" -ModJar "F:/mcplugins/mod/CloudStorage/build/libs/cloudstorage-0.1.0-SNAPSHOT.jar"
powershell -ExecutionPolicy Bypass -File "scripts/test-cells/Run-TestCellMod1122Regression.ps1" -Scope smoke -CellId cell-22 -AcquireCell -LeaseOwner "bbp-sprint-005-cell-22-smoke-retry2" -ModJar "F:/mcplugins/mod/CloudStorage/build/libs/cloudstorage-0.1.0-SNAPSHOT.jar"
Get-NetTCPConnection -State Listen -ErrorAction SilentlyContinue | Where-Object { $_.LocalPort -in 25720,38200,38201,25721,38210,38211,25722,38220,38221 } | Select-Object LocalAddress,LocalPort,OwningProcess
Get-CimInstance Win32_Process | Where-Object { $_.Name -in "cmd.exe","java.exe","javaw.exe" -and ($_.CommandLine -like "*BlackBoxProTestCells*cell-2*" -or $_.CommandLine -like "*server-cell-mod1122-2*") } | Select-Object ProcessId,Name,CommandLine
git diff --name-only -- "common" "mod" "plugin" "scripts/test-cells" "build.gradle.kts" "settings.gradle.kts" "gradle.properties"
if (Test-Path "F:/mcplugins/mod/CloudStorage/.git") { git -C "F:/mcplugins/mod/CloudStorage" diff --name-only }
git diff --check
git status --short
```

## Runtime Evidence

| Cell | Scope | Lease Owner | Result | Evidence |
| --- | --- | --- | --- | --- |
| `cell-20` | `startup` | `bbp-sprint-005-cell-20-startup` | PASS | Top-level `ok=true`; plugin `/status` ready on `38200`; mod `/status` ready on `38201`, platform `forge`, actions `109`; relay `query_player_state` success; stop `ok=true`; release `released=true`. |
| `cell-20` | `smoke` | `bbp-sprint-005-cell-20-smoke` | PASS | Top-level `ok=true`; plugin `/status` ready on `38200`; mod `/status` ready on `38201`, platform `forge`, actions `109`; relay `query_player_state` success; `run_test smoke passed=22 failed=0 skipped=0 total=22 totalMs=8084`; screenshot `G:/MC/game/BlackBoxProTestCells/cell-20/.minecraft/versions/bot/screenshots/blackboxpro/zzzderk/integration_smoke_1777897504/001_catalog_screenshot.png`, `854x480`, `31731` bytes; stop `ok=true`; release `released=true`. |
| `cell-21` | `startup` | `bbp-sprint-005-cell-21-startup` | PASS | Top-level `ok=true`; after reprovision the server plugin `/status` was ready on `38210`; mod `/status` was ready on `38211`, platform `forge`, actions `109`; relay `query_player_state` success; stop `ok=true`; release `released=true`. |
| `cell-21` | `smoke` | `bbp-sprint-005-cell-21-smoke-retry3` | PASS | Top-level `ok=true`; plugin `/status` ready on `38210`; mod `/status` ready on `38211`, platform `forge`, actions `109`; relay `query_player_state` success; `run_test smoke passed=22 failed=0 skipped=0 total=22 totalMs=8070`; screenshot `G:/MC/game/BlackBoxProTestCells/cell-21/.minecraft/versions/bot/screenshots/blackboxpro/zzzderk/integration_smoke_1777900827/001_catalog_screenshot.png`, `854x480`, `31773` bytes; stop `ok=true`; release `released=true`. |
| `cell-22` | `startup` | `bbp-sprint-005-cell-22-startup-retry2` | PASS | Top-level `ok=true`; plugin `/status` ready on `38220`; mod `/status` ready on `38221`, platform `forge`, actions `109`; relay `query_player_state` success; stop `ok=true`; release `released=true`. |
| `cell-22` | `smoke` | `bbp-sprint-005-cell-22-smoke-retry2` | PASS | Top-level `ok=true`; plugin `/status` ready on `38220`; mod `/status` ready on `38221`, platform `forge`, actions `109`; relay `query_player_state` success; `run_test smoke passed=22 failed=0 skipped=0 total=22 totalMs=8136`; screenshot `G:/MC/game/BlackBoxProTestCells/cell-22/.minecraft/versions/bot/screenshots/blackboxpro/zzzderk/integration_smoke_1777901180/001_catalog_screenshot.png`, `854x480`, `46635` bytes; stop `ok=true`; release `released=true`. |

## Cleanup Evidence

- `cell-20`, `cell-21`, and `cell-22` stop/release all returned `ok=true` / `released=true`.
- Final port cleanup check for `25720`, `38200`, `38201`, `25721`, `38210`, `38211`, `25722`, `38220`, `38221`: no output.
- Final process cleanup check for mod1122 `cmd.exe`, `java.exe`, `javaw.exe`: no output.
- Final denied-path diff checks and `git diff --check` were clean.

## Denied Path Compliance

- `common/**`, `mod/**`, `plugin/**`, `scripts/test-cells/**`, root Gradle files: no diff.
- `F:/mcplugins/mod/CloudStorage/**`: no diff.
- Only test-cell environment data and workflow docs were touched.

## Residual Risks

- None in repository source.
- The only observed risk was test-cell environment drift on `cell-21`; it was repaired and retested successfully.

## Next Legal Action

Draft Sprint 6 contract for Germ real physical click enhancement. Keep multi-bot scenario orchestration separate.
