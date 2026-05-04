# Task Contract: bbp-sprint-002-mod1122-smoke-evidence

## Task ID

`bbp-sprint-002-mod1122-smoke-evidence`

## Role

Generator

## Goal

Produce real runtime evidence that the Forge 1.12.2 business-mod test pool `cell-20..22` can run a concrete business mod jar through BlackBoxPro startup and smoke validation with cleanup.

Use CloudStorage as the fixed business-mod target:

`F:/mcplugins/mod/CloudStorage/build/libs/cloudstorage-0.1.0-SNAPSHOT.jar`

## Success Criteria

- The target CloudStorage jar exists before runtime commands start.
- `Run-TestCellMod1122Regression.ps1 -Scope startup -AcquireCell` succeeds with JSON `ok=true`.
- `Run-TestCellMod1122Regression.ps1 -Scope smoke -AcquireCell` succeeds with JSON `ok=true`.
- Both runs use `scripts/test-cells/cells-mod1122.json`.
- Both runs automatically stop and release their acquired cell; do not use `-KeepCell`.
- The worker report records the acquired cell id, lease owner, scope, sync result, invoke result, cleanup result, and any errors.
- No source code, Gradle files, test-cell scripts, or global `.codex` files are modified.

## Allowed Paths

- `docs/workflow/**`

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

- Repo: current checkout/worktree root. The original repo path is `F:/mcplugins/BlackBoxPro-dev-2.0`, but a worker should run repo-local commands from the worktree where this task was copied.
- Config: `scripts/test-cells/cells-mod1122.json`
- Mod jar: `F:/mcplugins/mod/CloudStorage/build/libs/cloudstorage-0.1.0-SNAPSHOT.jar`
- Lease owners:
  - startup: `bbp-sprint-002-cloudstorage-startup`
  - smoke: `bbp-sprint-002-cloudstorage-smoke`

## Acceptance Commands

Run from the current checkout/worktree root. Use relative paths for repo-local scripts and configs; only the fixed CloudStorage mod jar path is absolute.

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

## Constraints

- Do not pass `-KeepCell`.
- Do not pass `-ShowClient`.
- Do not modify the CloudStorage repository.
- Do not rebuild CloudStorage unless the fixed jar is missing; if missing, stop and report `BLOCKED`.
- Do not manually copy jars; use `Run-TestCellMod1122Regression.ps1`, which calls the sync script internally.
- Do not change any test-cell config or script.
- Do not claim a PASS unless command output contains JSON `ok=true` and cleanup has no recorded error.
- Do not run repo-local commands against the original `F:/mcplugins/BlackBoxPro-dev-2.0` checkout when executing inside a worker worktree.
- Do not modify `C:/Users/Administrator/.codex/**`; reading the worker-result template is allowed only if the wrapper prompt requests it.

## Output

Create `docs/workflow/worker-results/bbp-sprint-002-mod1122-smoke-evidence-result.md` with:

- first line `### DONE: bbp-sprint-002-mod1122-smoke-evidence`, `### FAILED: ...`, or `### BLOCKED: ...`
- changed files
- exact commands run
- startup result summary
- smoke result summary
- acquired cell ids and lease owners
- cleanup result
- residual risks
- confirmation that denied paths were not modified

## Stop Rules

- Stop if the target CloudStorage jar is missing.
- Stop if no ready `cell-20..22` lease can be acquired.
- Stop if startup fails; do not proceed to smoke.
- Stop if cleanup fails; report `FAIL` with the cell id and lease owner.
- Stop if any denied path changes.
- Stop if command output is not parseable enough to determine `ok=true`.
