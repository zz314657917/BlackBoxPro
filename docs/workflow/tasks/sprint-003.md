# Task Contract: bbp-sprint-003-priority-input-support

## Task ID

`bbp-sprint-003-priority-input-support`

## Role

Generator for the implementation; test-only worker may run verification and write a worker result. Codex remains the final Evaluator.

## Goal

Prioritize the currently useful client lines by bringing Forge `1.20.1` to parity with the existing Forge `1.12.2` input action baseline, while preserving the `1.12.2` implementation.

Target actions:

- `move_mouse`
- `click_mouse`
- `click_screen_at`
- `query_cursor_state`
- `key_press`
- `type_text`

`ActionCatalog` remains the action id and parameter truth source. The earlier `1.21.11` / `1.21.1` modern sync target is deferred and must not be implemented in this sprint.

## Success Criteria

- Forge `1.20.1` registers all six target actions.
- Forge `1.20.1` has real implementation/helper files for all six target actions; no always-success stubs.
- Forge `1.12.2` continues to register and provide implementation/helper files for all six target actions.
- `ActionCatalog` ids and parameter lists remain unchanged.
- `PriorityInputActionSupportTest` or equivalent common test coverage verifies the priority-version registration/files and stable catalog params.
- Existing plugin API wrappers and `run_test` catalog entries remain aligned with `ActionCatalog`; no plugin source change is required unless a mismatch is found.
- Root `forge1201_build` works from the repo root without relying on an incompatible Gradle 9 child invocation.
- Build/static acceptance commands pass.
- Runtime PASS for either priority version requires a real `/status` endpoint and smoke evidence for that same version. If no runtime endpoint is available, report runtime as `BLOCKED` or unverified instead of substituting another version.

## Allowed Paths

- `mod/1.20.1/src/main/kotlin/**`
- `common/src/test/kotlin/com/blackboxpro/common/action/**`
- `build.gradle.kts` only for the `forge1201_build` child Gradle cache/runtime fix needed by root acceptance
- `docs/workflow/**`
- `knowledge/tasks/current-task.md`
- `knowledge/tasks/timeline.md`

For a test-only worker, write only under `docs/workflow/worker-results/**` or `docs/workflow/qa/**`.

## Denied Paths

- `common/src/main/kotlin/com/blackboxpro/common/action/ActionCatalog.kt`
- `common/src/main/kotlin/**`
- `mod/1.21.11/**`
- `mod/1.21.1/**`
- `mod/1.12.2/**`
- `plugin/src/main/kotlin/**`
- `scripts/test-cells/**`
- `settings.gradle.kts`
- `gradle.properties`
- `C:/Users/Administrator/.codex/**`

## Constraints

- Keep this as a priority-version input action support sprint, not a transport rewrite.
- Do not add `/actions`; current HTTP endpoints remain `/execute` and `/status`.
- Do not add multi-bot orchestration, scenario DSL, or identity management.
- Do not change `ActionCatalog` ids or parameter order.
- Do not change test-cell scripts or configs.
- Do not launch Minecraft with `-ShowClient` unless a later Evaluator explicitly requests a manual session.
- Do not describe `click_screen_at` or `click_mouse` as proof that all Germ pages are physically clickable.
- Do not claim `1.21.x` support from this sprint.

## Acceptance Commands

Run from the repo root.

```powershell
rg -n "move_mouse|click_mouse|click_screen_at|query_cursor_state|key_press|type_text" "mod/1.20.1" "mod/1.12.2" "plugin/src/main/kotlin" "common/src/test/kotlin"
```

```powershell
.\gradlew.bat -p common test --no-daemon
```

```powershell
.\gradlew.bat common_build plugin_build forge1201_build forge1122_build --no-daemon
```

```powershell
git diff --check
git status --short
```

Runtime commands are optional for this sprint and required only if a real priority-version endpoint is available in the current environment. Record the exact endpoint, action count, smoke commands, and cleanup evidence if runtime smoke is run. If no endpoint exists, report runtime as `BLOCKED` or unverified.

## Output

Create an Evaluator QA report at `docs/workflow/qa/sprint-003-qa.md` with:

- first line `### PASS: bbp-sprint-003-priority-input-support`, `### FAIL: ...`, or `### BLOCKED: ...`
- changed files
- implementation summary by version
- exact commands run
- build/static result summary
- runtime endpoint and smoke evidence, or explicit runtime blocked/unverified reason
- denied-path compliance
- residual risks
- next legal action

If a test-only worker is used, its report must live under `docs/workflow/worker-results/**` or `docs/workflow/qa/**` and Codex must still write the final QA report.

## Stop Rules

- Stop if the repo is not a git repository.
- Stop if satisfying the task requires changing `ActionCatalog` ids or parameter order.
- Stop if satisfying the task requires editing `mod/1.21.11/**`, `mod/1.21.1/**`, `mod/1.12.2/**`, test-cell scripts, plugin source, or global `.codex` files.
- Stop if mouse/keyboard handling cannot be implemented without global OS input injection.
- Stop if builds fail for reasons unrelated to the task and the failure cannot be isolated without expanding scope.
- Stop and report runtime `BLOCKED` if no real priority-version runtime endpoint exists for `/status` and smoke verification.
