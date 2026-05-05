# Task Contract: bbp-sprint-004-modern-input-sync

## Task ID

`bbp-sprint-004-modern-input-sync`

## Role

Generator for implementation. A test-only worker may run build/static/runtime checks and write a worker result. Codex remains the final Evaluator.

## Goal

Sync the six priority input actions from the validated Forge `1.20.1` / Forge `1.12.2` baseline into the modern client lines:

- `1.21.11` Fabric
- `1.21.11` NeoForge
- `1.21.1` Fabric
- `1.21.1` NeoForge

Target actions:

- `move_mouse`
- `click_mouse`
- `click_screen_at`
- `query_cursor_state`
- `key_press`
- `type_text`

Prefer shared runtime implementation where the existing `runtime` modules make that reasonable. Use loader-specific code only for Fabric/NeoForge API differences. Do not change action ids, catalog parameters, or transport behavior.

## Success Criteria

- `1.21.11` registers all six target actions for both Fabric and NeoForge runtime paths.
- `1.21.1` registers all six target actions for both Fabric and NeoForge runtime paths.
- Each target action has real implementation, not an always-success stub.
- `ActionCatalog` ids and parameter lists remain unchanged.
- Plugin wrappers, parameter metadata, and `run_test` catalog remain aligned with `ActionCatalog`; plugin source should not need changes unless an actual mismatch is found.
- `1.20.1` and `1.12.2` are preserved as known-good baselines and are not modified.
- Common test coverage verifies modern-version registration/files and stable catalog params.
- Build/static acceptance commands pass.
- Runtime PASS for a modern client line requires a real same-version `/status` endpoint and same-version smoke evidence. Do not substitute `1.20.1` or `1.12.2` runtime evidence for `1.21.x`.

## Allowed Paths

- `mod/1.21.11/runtime/src/main/kotlin/**`
- `mod/1.21.11/fabric/src/main/kotlin/**`
- `mod/1.21.11/neoforge/src/main/kotlin/**`
- `mod/1.21.1/runtime/src/main/kotlin/**`
- `mod/1.21.1/fabric/src/main/kotlin/**`
- `mod/1.21.1/neoforge/src/main/kotlin/**`
- `common/src/test/kotlin/com/blackboxpro/common/action/**`
- `docs/workflow/**`
- `knowledge/tasks/current-task.md`
- `knowledge/tasks/timeline.md`

For a test-only worker, write only under `docs/workflow/worker-results/**` or `docs/workflow/qa/**`.

## Denied Paths

- `common/src/main/kotlin/com/blackboxpro/common/action/ActionCatalog.kt`
- `common/src/main/kotlin/**`
- `mod/1.20.1/**`
- `mod/1.12.2/**`
- `plugin/src/main/kotlin/**`
- `scripts/test-cells/**`
- `build.gradle.kts`
- `settings.gradle.kts`
- `gradle.properties`
- `C:/Users/Administrator/.codex/**`

## Constraints

- Keep this sprint scoped to modern input action parity.
- Do not add `/actions`; current HTTP endpoints remain `/execute` and `/status`.
- Do not add multi-bot orchestration, scenario DSL, identity management, or mineflayer-style high-level behavior.
- Do not change `ActionCatalog` ids or parameter order.
- Do not change test-cell scripts or configs.
- Do not use global OS input injection.
- Do not describe `click_screen_at` or `click_mouse` as proof that all Germ pages are physically clickable.
- Do not claim runtime support without real same-version endpoint evidence.

## Acceptance Commands

Run from the repo root.

```powershell
rg -n "move_mouse|click_mouse|click_screen_at|query_cursor_state|key_press|type_text" "mod/1.21.11" "mod/1.21.1" "plugin/src/main/kotlin" "common/src/test/kotlin"
```

```powershell
.\gradlew.bat -p common test --no-daemon
```

```powershell
.\gradlew.bat common_build plugin_build mod2111_build mod1211_build --no-daemon
```

```powershell
git diff --name-only -- "mod/1.20.1" "mod/1.12.2" "common/src/main/kotlin/com/blackboxpro/common/action/ActionCatalog.kt" "plugin/src/main/kotlin" "scripts/test-cells"
```

Expected result for the denied-path diff command: no output.

```powershell
git diff --check
git status --short
```

Runtime smoke is required only when real `1.21.11` / `1.21.1` endpoints are available. Record exact version, loader, endpoint, action count, smoke actions, and cleanup evidence. If no endpoint exists, report runtime as `BLOCKED` or unverified for that version/loader.

## Output

Create an Evaluator QA report at `docs/workflow/qa/sprint-004-qa.md` with:

- first line `### PASS: bbp-sprint-004-modern-input-sync`, `### FAIL: ...`, or `### BLOCKED: ...`
- changed files
- implementation summary by version and loader
- exact commands run
- build/static result summary
- runtime endpoint and smoke evidence, or explicit runtime blocked/unverified reason per version/loader
- denied-path compliance
- residual risks
- next legal action

If a test-only worker is used, its report must live under `docs/workflow/worker-results/**` or `docs/workflow/qa/**`, and Codex must still write the final QA report.

## Stop Rules

- Stop if the repo is not a git repository.
- Stop if satisfying the task requires changing `ActionCatalog` ids or parameter order.
- Stop if satisfying the task requires editing `mod/1.20.1/**`, `mod/1.12.2/**`, test-cell scripts, plugin source, or global `.codex` files.
- Stop if mouse/keyboard handling cannot be implemented without global OS input injection.
- Stop if the modern runtime/module split makes the task ambiguous; return to Planner with the exact conflicting files and proposed split.
- Stop if builds fail for reasons unrelated to the task and the failure cannot be isolated without expanding scope.
- Stop and report runtime `BLOCKED` for any version/loader without a real same-version `/status` endpoint.
