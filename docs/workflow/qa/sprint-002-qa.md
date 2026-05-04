### PASS: bbp-sprint-002-mod1122-smoke-evidence

## Summary

Sprint 2 was completed by Codex/Evaluator direct execution after two DeepSeek worker attempts exceeded budget before producing a valid worker report.

The fixed CloudStorage Forge 1.12.2 jar was validated on the managed business-mod pool using `cell-20`. Both `startup` and `smoke` runs reported `ok=true`, and both runs completed automatic stop and lease release.

## Commands Run

```powershell
Test-Path "F:/mcplugins/mod/CloudStorage/build/libs/cloudstorage-0.1.0-SNAPSHOT.jar"
```

```powershell
powershell -ExecutionPolicy Bypass -File "scripts/test-cells/Run-TestCellMod1122Regression.ps1" -Scope startup -ModJar "F:/mcplugins/mod/CloudStorage/build/libs/cloudstorage-0.1.0-SNAPSHOT.jar" -AcquireCell -LeaseOwner "bbp-sprint-002-cloudstorage-startup"
```

```powershell
powershell -ExecutionPolicy Bypass -File "scripts/test-cells/Run-TestCellMod1122Regression.ps1" -Scope smoke -ModJar "F:/mcplugins/mod/CloudStorage/build/libs/cloudstorage-0.1.0-SNAPSHOT.jar" -AcquireCell -LeaseOwner "bbp-sprint-002-cloudstorage-smoke"
```

```powershell
git diff --check
git status --short
```

## Evidence

- Mod jar exists: `True`.
- Startup:
  - `scope`: `startup`
  - `cellId`: `cell-20`
  - `leaseOwner`: `bbp-sprint-002-cloudstorage-startup`
  - `ok`: `true`
  - `cleanupRequested`: `true`
  - release: `ok=true`, `released=true`
  - stop cleanup freed ports: `serverPort=true`, `pluginHttpPort=true`, `modHttpPort=true`
- Smoke:
  - `scope`: `smoke`
  - `cellId`: `cell-20`
  - `leaseOwner`: `bbp-sprint-002-cloudstorage-smoke`
  - `ok`: `true`
  - test status: `success`
  - message: `Test 'smoke' completed`
  - totals: `passed=22`, `failed=0`, `skipped=0`, `total=22`, `totalMs=8081`
  - screenshot: `G:/MC/game/BlackBoxProTestCells/cell-20/.minecraft/versions/bot/screenshots/blackboxpro/zzzderk/integration_smoke_1777814618/001_catalog_screenshot.png`, `854x480`, `31762` bytes, file exists
  - release: `ok=true`, `released=true`
  - stop cleanup freed ports: `serverPort=true`, `pluginHttpPort=true`, `modHttpPort=true`
- Final cleanup:
  - listeners on `25720`, `38200`, `38201`: `0`
  - matching `cmd/java/javaw` test-cell processes: `0`
- `git diff --check`: no whitespace errors; only Windows line-ending warnings for existing dirty files.

## Worker Attempts

- Attempt 1: `C:/Users/Administrator/.codex/scripts/Invoke-PgeWorker.ps1` with `BudgetUsd=0.10` failed with `error_max_budget_usd`, total cost `0.102509`, before writing the expected report.
- Attempt 2: retried with `BudgetUsd=0.20` and `PermissionMode=bypassPermissions`; failed with `error_max_budget_usd`, total cost `0.241496`, before writing the expected report.
- No further worker retries were run. Codex executed the approved acceptance commands directly to avoid more budget burn.

## Findings

- No blocking findings.
- Sprint 2 success criteria are satisfied for `cell-20` in the `cell-20..22` business-mod pool.
- The run used `scripts/test-cells/cells-mod1122.json` through `Run-TestCellMod1122Regression.ps1`.
- No source code, Gradle files, test-cell scripts, CloudStorage repository files, or global `.codex` files were modified by this QA step.

## Residual Risks

- This Sprint proves the business-mod path on one acquired pool cell (`cell-20`), not a full matrix across `cell-20`, `cell-21`, and `cell-22`.
- DeepSeek worker dispatch is not yet cost-stable for runtime tasks; future worker contracts should be shorter or use a higher budget only when necessary.
