# Task Contract: bbp-sprint-005-mod1122-matrix-evidence

## Task ID

`bbp-sprint-005-mod1122-matrix-evidence`

## Role

QA Generator / test-only worker. Codex remains the final Evaluator.

## Goal

Produce real runtime matrix evidence that all three Forge 1.12.2 business-mod cells can run a concrete business mod jar through BlackBoxPro startup and smoke validation with cleanup:

- `cell-20`
- `cell-21`
- `cell-22`

Use CloudStorage as the fixed business-mod target:

`F:/mcplugins/mod/CloudStorage/build/libs/cloudstorage-0.1.0-SNAPSHOT.jar`

This sprint closes the Sprint 2 evidence gap. Sprint 2 proved the path on `cell-20`; Sprint 5 must prove the same contract on `cell-20`, `cell-21`, and `cell-22`.

## Success Criteria

- The target CloudStorage jar exists before runtime commands start.
- Each of `cell-20`, `cell-21`, and `cell-22` completes `startup` with JSON `ok=true`.
- Each of `cell-20`, `cell-21`, and `cell-22` completes `smoke` with JSON `ok=true`.
- Every run uses `scripts/test-cells/cells-mod1122.json`.
- Every run uses `Run-TestCellMod1122Regression.ps1` with explicit `-CellId <cell>` and `-AcquireCell`.
- No run passes `-KeepCell` or `-ShowClient`.
- Every run automatically stops and releases its acquired lease.
- Final cleanup confirms no listening ports remain for:
  - `cell-20`: `25720`, `38200`, `38201`
  - `cell-21`: `25721`, `38210`, `38211`
  - `cell-22`: `25722`, `38220`, `38221`
- Final cleanup confirms no matching test-cell `cmd/java/javaw` process remains for the mod1122 cell paths.
- The report records per-cell scope, lease owner, `ok`, `run_test` totals for smoke, screenshot path if present, stop/release evidence, and any errors.
- No source code, Gradle files, test-cell scripts, CloudStorage repository files, or global `.codex` files are modified.

## Allowed Paths

For a test-only worker:

- `docs/workflow/worker-results/**`
- `docs/workflow/qa/**`

For Codex/Evaluator after review:

- `docs/workflow/**`
- `knowledge/tasks/current-task.md`
- `knowledge/tasks/timeline.md`

## Denied Paths

- `common/**`
- `mod/**`
- `plugin/**`
- `scripts/test-cells/**`
- `build.gradle.kts`
- `settings.gradle.kts`
- `gradle.properties`
- `F:/mcplugins/mod/CloudStorage/**`
- `C:/Users/Administrator/.codex/**`

## Runtime Inputs

- Repo: current checkout/worktree root. Run repo-local commands from the checkout where this contract is executed.
- Config: `scripts/test-cells/cells-mod1122.json`
- Mod jar: `F:/mcplugins/mod/CloudStorage/build/libs/cloudstorage-0.1.0-SNAPSHOT.jar`
- Lease owners:
  - `bbp-sprint-005-cell-20-startup`
  - `bbp-sprint-005-cell-20-smoke`
  - `bbp-sprint-005-cell-21-startup`
  - `bbp-sprint-005-cell-21-smoke`
  - `bbp-sprint-005-cell-22-startup`
  - `bbp-sprint-005-cell-22-smoke`

## Acceptance Commands

Run from the current checkout/worktree root. Use relative paths for repo-local scripts and configs; only the fixed CloudStorage mod jar path is absolute.

```powershell
Test-Path "F:/mcplugins/mod/CloudStorage/build/libs/cloudstorage-0.1.0-SNAPSHOT.jar"
```

```powershell
powershell -ExecutionPolicy Bypass -File "scripts/test-cells/Run-TestCellMod1122Regression.ps1" -Scope startup -CellId cell-20 -AcquireCell -LeaseOwner "bbp-sprint-005-cell-20-startup" -ModJar "F:/mcplugins/mod/CloudStorage/build/libs/cloudstorage-0.1.0-SNAPSHOT.jar"
```

```powershell
powershell -ExecutionPolicy Bypass -File "scripts/test-cells/Run-TestCellMod1122Regression.ps1" -Scope smoke -CellId cell-20 -AcquireCell -LeaseOwner "bbp-sprint-005-cell-20-smoke" -ModJar "F:/mcplugins/mod/CloudStorage/build/libs/cloudstorage-0.1.0-SNAPSHOT.jar"
```

```powershell
powershell -ExecutionPolicy Bypass -File "scripts/test-cells/Run-TestCellMod1122Regression.ps1" -Scope startup -CellId cell-21 -AcquireCell -LeaseOwner "bbp-sprint-005-cell-21-startup" -ModJar "F:/mcplugins/mod/CloudStorage/build/libs/cloudstorage-0.1.0-SNAPSHOT.jar"
```

```powershell
powershell -ExecutionPolicy Bypass -File "scripts/test-cells/Run-TestCellMod1122Regression.ps1" -Scope smoke -CellId cell-21 -AcquireCell -LeaseOwner "bbp-sprint-005-cell-21-smoke" -ModJar "F:/mcplugins/mod/CloudStorage/build/libs/cloudstorage-0.1.0-SNAPSHOT.jar"
```

```powershell
powershell -ExecutionPolicy Bypass -File "scripts/test-cells/Run-TestCellMod1122Regression.ps1" -Scope startup -CellId cell-22 -AcquireCell -LeaseOwner "bbp-sprint-005-cell-22-startup" -ModJar "F:/mcplugins/mod/CloudStorage/build/libs/cloudstorage-0.1.0-SNAPSHOT.jar"
```

```powershell
powershell -ExecutionPolicy Bypass -File "scripts/test-cells/Run-TestCellMod1122Regression.ps1" -Scope smoke -CellId cell-22 -AcquireCell -LeaseOwner "bbp-sprint-005-cell-22-smoke" -ModJar "F:/mcplugins/mod/CloudStorage/build/libs/cloudstorage-0.1.0-SNAPSHOT.jar"
```

```powershell
Get-NetTCPConnection -State Listen -ErrorAction SilentlyContinue | Where-Object { $_.LocalPort -in 25720,38200,38201,25721,38210,38211,25722,38220,38221 } | Select-Object LocalAddress,LocalPort,OwningProcess
```

Expected result for the port check: no output.

```powershell
Get-CimInstance Win32_Process | Where-Object { $_.Name -in "cmd.exe","java.exe","javaw.exe" -and ($_.CommandLine -like "*BlackBoxProTestCells*cell-2*" -or $_.CommandLine -like "*server-cell-mod1122-2*") } | Select-Object ProcessId,Name,CommandLine
```

Expected result for the process check: no matching mod1122 test-cell process.

```powershell
git diff --name-only -- "common" "mod" "plugin" "scripts/test-cells" "build.gradle.kts" "settings.gradle.kts" "gradle.properties"
```

Expected result for the denied-path diff command: no output.

```powershell
if (Test-Path "F:/mcplugins/mod/CloudStorage/.git") { git -C "F:/mcplugins/mod/CloudStorage" diff --name-only }
```

Expected result for the CloudStorage source diff command: no output.

```powershell
git diff --check
git status --short
```

## Constraints

- Do not pass `-KeepCell`.
- Do not pass `-ShowClient`.
- Do not run the three cells in parallel unless explicitly approved; default to sequential execution to reduce memory pressure.
- Do not modify the CloudStorage repository.
- Do not rebuild CloudStorage unless the fixed jar is missing; if missing, stop and report `BLOCKED`.
- Do not manually copy jars; use `Run-TestCellMod1122Regression.ps1`, which calls the sync script internally.
- Do not change any test-cell config or script.
- Do not claim a per-cell PASS unless command output contains JSON `ok=true` and cleanup has no recorded error.
- Do not claim matrix PASS unless all three cells pass both startup and smoke and final cleanup checks are clean.
- Do not treat `cell-20` evidence alone as satisfying Sprint 5.
- Do not modify `C:/Users/Administrator/.codex/**`; reading templates is allowed only if the wrapper prompt requests it.

## Output

Create `docs/workflow/worker-results/bbp-sprint-005-mod1122-matrix-evidence-result.md` with:

- first line `### DONE: bbp-sprint-005-mod1122-matrix-evidence`, `### FAILED: ...`, or `### BLOCKED: ...`
- changed files
- exact commands run
- per-cell startup result summary
- per-cell smoke result summary
- per-cell acquired lease owner
- per-cell cleanup result
- final port/process cleanup result
- denied-path compliance
- residual risks

Codex/Evaluator must create the final QA report at `docs/workflow/qa/sprint-005-qa.md` with first line `### PASS: ...`, `### FAIL: ...`, or `### BLOCKED: ...`.

## Stop Rules

- Stop if the target CloudStorage jar is missing.
- Stop if any requested `cell-20`, `cell-21`, or `cell-22` lease cannot be acquired.
- Stop if a cell startup fails; report which cell failed and do not run that cell's smoke.
- Continue to cleanup even after a runtime failure.
- Stop and report `FAIL` if cleanup fails for any cell.
- Stop if any denied path changes.
- Stop if command output is not parseable enough to determine per-run `ok=true`.
- Stop if running the full matrix would require modifying scripts, configs, source code, or CloudStorage.
