# Test Worker Contract: bbp-sprint-006-germ-real-physical-click-qa

## Task ID

`bbp-sprint-006-germ-real-physical-click-qa`

## Role

QA Generator / test-only worker. Codex remains the final Evaluator.

## Goal

Run Sprint 6 static, build, diff, and runtime checks for the current checkout. Do not implement fixes. Do not modify source code.

## Success Criteria

- Static search confirms Germ click/query boundaries are present.
- `common test` passes under Java 21.
- `common_build plugin_build forge1122_build` passes under Java 21.
- Denied-path diff is empty for `ActionCatalog`, modern modules, plugin Germ server action, test-cell scripts, and root Gradle files.
- `git diff --check` has no whitespace errors.
- Runtime smoke is attempted only on a real Forge `1.12.2` Germ page.
- Runtime `PASS` is not allowed unless `click_germ_component` returns client click evidence and a before/after business side effect is recorded.
- If the real Germ page or business side-effect observer is unavailable, report `### BLOCKED`, not PASS.

## Allowed Paths

- `docs/workflow/worker-results/**`
- `docs/workflow/qa/**`

## Denied Paths

- `common/**`
- `mod/**`
- `plugin/**`
- `scripts/test-cells/**`
- `build.gradle.kts`
- `settings.gradle.kts`
- `gradle.properties`
- `C:/Users/Administrator/.codex/**`
- any external plugin repository

## Acceptance Commands

Run from the current checkout root.

```powershell
$env:JAVA_HOME=(Resolve-Path "../.local-tools/temurin21/jdk-21.0.10+7").Path
$env:Path="$env:JAVA_HOME/bin;$env:Path"
rg -n "click_germ_component|query_germ_hit_test|query_germ_screen|germ_gui_part_dos" "mod/1.12.2" "plugin/src/main/kotlin" "common/src/test/kotlin" "knowledge" "docs/workflow"
.\gradlew.bat -p common test --no-daemon
.\gradlew.bat common_build plugin_build forge1122_build --no-daemon
git diff --name-only -- "common/src/main/kotlin/com/blackboxpro/common/action/ActionCatalog.kt" "common/src/main/kotlin" "mod/1.20.1" "mod/1.21.1" "mod/1.21.11" "plugin/src/main/kotlin/com/blackboxpro/plugin/http/ServerGermActions.kt" "scripts/test-cells" "build.gradle.kts" "settings.gradle.kts" "gradle.properties"
git diff --check
git status --short
```

Runtime, only if a real `cell-01` Germ page is available:

```powershell
$owner = "bbp-sprint-006-test-worker"
powershell -ExecutionPolicy Bypass -File "scripts/test-cells/Acquire-TestCell.ps1" -CellId cell-01 -Owner $owner -ReadyOnly
powershell -ExecutionPolicy Bypass -File "scripts/test-cells/Invoke-TestCell.ps1" -Mode ensure -CellId cell-01
Invoke-RestMethod -Uri "http://127.0.0.1:38080/status" -Method GET
Invoke-RestMethod -Uri "http://127.0.0.1:38081/status" -Method GET
```

Then use the Sprint 6 contract runtime flow from `docs/workflow/tasks/sprint-006.md`: relay check, open a real Germ page, `query_germ_screen`, `query_germ_hit_test`, optional `germ_gui_part_dos execute=false`, `click_germ_component`, before/after business-state evidence, cleanup.

Cleanup after any runtime attempt:

```powershell
powershell -ExecutionPolicy Bypass -File "scripts/test-cells/Invoke-TestCell.ps1" -Mode stop -CellId cell-01
powershell -ExecutionPolicy Bypass -File "scripts/test-cells/Release-TestCell.ps1" -CellId cell-01 -Owner "bbp-sprint-006-test-worker"
Get-NetTCPConnection -State Listen -ErrorAction SilentlyContinue | Where-Object { $_.LocalPort -in 25570,38080,38081 } | Select-Object LocalAddress,LocalPort,OwningProcess
Get-CimInstance Win32_Process | Where-Object { $_.Name -in "cmd.exe","java.exe","javaw.exe" -and ($_.CommandLine -like "*BlackBoxProTestCells*cell-01*" -or $_.CommandLine -like "*server-cell-01*") } | Select-Object ProcessId,Name,CommandLine
```

## Output

Write `docs/workflow/worker-results/bbp-sprint-006-germ-real-physical-click-qa-result.md` with:

- first line `### DONE: bbp-sprint-006-germ-real-physical-click-qa`, `### FAILED: ...`, or `### BLOCKED: ...`
- changed files
- exact commands run
- static/build results
- denied-path diff result
- runtime evidence or blocker
- cleanup evidence
- residual risks

## Constraints

- Do not modify source code, Gradle files, test-cell scripts, configs, external plugin repositories, or global `.codex`.
- Do not run `germ_gui_part_dos execute=true`.
- Do not declare runtime PASS without real `click_germ_component` evidence and before/after business side-effect evidence.
- Do not use screenshots alone as business side-effect evidence.
- Treat unavailable real Germ page or unavailable business observer as `BLOCKED`.
- Keep runtime attempts sequential and always cleanup.

## Stop Rules

- Stop if any denied path changes.
- Stop if a build or static command fails.
- Stop if acquiring or opening a real Germ page would require changing test-cell scripts, configs, source, or external plugin files.
- Stop if the only possible runtime side effect path is `germ_gui_part_dos execute=true`.
- Always cleanup after runtime attempts.
