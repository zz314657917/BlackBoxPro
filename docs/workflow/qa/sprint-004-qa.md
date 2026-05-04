### PASS: bbp-sprint-004-modern-input-sync

## Scope

Sprint 4 synced the six priority input actions into the modern `1.21.11` and `1.21.1` Fabric + NeoForge client lines.

Runtime smoke is not marked PASS for any `1.21.x` line in this report. No real same-version `1.21.11` / `1.21.1` `/status` endpoint was available during acceptance, so runtime evidence remains `BLOCKED` / unverified per the contract.

## Changed Files

Implementation:

- `mod/1.21.11/runtime/src/main/kotlin/com/blackboxpro/neoforge/action/client/ClickMouseAction.kt`
- `mod/1.21.11/runtime/src/main/kotlin/com/blackboxpro/neoforge/action/client/ClickScreenAtAction.kt`
- `mod/1.21.11/runtime/src/main/kotlin/com/blackboxpro/neoforge/action/client/KeyPressAction.kt`
- `mod/1.21.11/runtime/src/main/kotlin/com/blackboxpro/neoforge/action/client/MoveMouseAction.kt`
- `mod/1.21.11/runtime/src/main/kotlin/com/blackboxpro/neoforge/action/client/TypeTextAction.kt`
- `mod/1.21.11/runtime/src/main/kotlin/com/blackboxpro/neoforge/action/query/QueryCursorStateAction.kt`
- `mod/1.21.11/runtime/src/main/kotlin/com/blackboxpro/neoforge/util/ScreenKeyboardHelper.kt`
- `mod/1.21.11/runtime/src/main/kotlin/com/blackboxpro/neoforge/util/ScreenMouseHelper.kt`
- `mod/1.21.11/runtime/src/main/kotlin/com/blackboxpro/neoforge/dispatcher/ActionRegistry.kt`
- `mod/1.21.11/fabric/src/main/kotlin/com/blackboxpro/fabric/action/client/ClickMouseAction.kt`
- `mod/1.21.11/fabric/src/main/kotlin/com/blackboxpro/fabric/action/client/ClickScreenAtAction.kt`
- `mod/1.21.11/fabric/src/main/kotlin/com/blackboxpro/fabric/action/client/KeyPressAction.kt`
- `mod/1.21.11/fabric/src/main/kotlin/com/blackboxpro/fabric/action/client/MoveMouseAction.kt`
- `mod/1.21.11/fabric/src/main/kotlin/com/blackboxpro/fabric/action/client/TypeTextAction.kt`
- `mod/1.21.11/fabric/src/main/kotlin/com/blackboxpro/fabric/action/query/QueryCursorStateAction.kt`
- `mod/1.21.11/fabric/src/main/kotlin/com/blackboxpro/fabric/util/ScreenKeyboardHelper.kt`
- `mod/1.21.11/fabric/src/main/kotlin/com/blackboxpro/fabric/util/ScreenMouseHelper.kt`
- `mod/1.21.11/fabric/src/main/kotlin/com/blackboxpro/fabric/dispatcher/ActionRegistry.kt`
- `mod/1.21.1/runtime/src/main/kotlin/com/blackboxpro/neoforge/action/client/ClickMouseAction.kt`
- `mod/1.21.1/runtime/src/main/kotlin/com/blackboxpro/neoforge/action/client/ClickScreenAtAction.kt`
- `mod/1.21.1/runtime/src/main/kotlin/com/blackboxpro/neoforge/action/client/KeyPressAction.kt`
- `mod/1.21.1/runtime/src/main/kotlin/com/blackboxpro/neoforge/action/client/MoveMouseAction.kt`
- `mod/1.21.1/runtime/src/main/kotlin/com/blackboxpro/neoforge/action/client/TypeTextAction.kt`
- `mod/1.21.1/runtime/src/main/kotlin/com/blackboxpro/neoforge/action/query/QueryCursorStateAction.kt`
- `mod/1.21.1/runtime/src/main/kotlin/com/blackboxpro/neoforge/util/ScreenKeyboardHelper.kt`
- `mod/1.21.1/runtime/src/main/kotlin/com/blackboxpro/neoforge/util/ScreenMouseHelper.kt`
- `mod/1.21.1/runtime/src/main/kotlin/com/blackboxpro/neoforge/dispatcher/ActionRegistry.kt`
- `mod/1.21.1/fabric/src/main/kotlin/com/blackboxpro/fabric/action/client/ClickMouseAction.kt`
- `mod/1.21.1/fabric/src/main/kotlin/com/blackboxpro/fabric/action/client/ClickScreenAtAction.kt`
- `mod/1.21.1/fabric/src/main/kotlin/com/blackboxpro/fabric/action/client/KeyPressAction.kt`
- `mod/1.21.1/fabric/src/main/kotlin/com/blackboxpro/fabric/action/client/MoveMouseAction.kt`
- `mod/1.21.1/fabric/src/main/kotlin/com/blackboxpro/fabric/action/client/TypeTextAction.kt`
- `mod/1.21.1/fabric/src/main/kotlin/com/blackboxpro/fabric/action/query/QueryCursorStateAction.kt`
- `mod/1.21.1/fabric/src/main/kotlin/com/blackboxpro/fabric/util/ScreenKeyboardHelper.kt`
- `mod/1.21.1/fabric/src/main/kotlin/com/blackboxpro/fabric/util/ScreenMouseHelper.kt`
- `mod/1.21.1/fabric/src/main/kotlin/com/blackboxpro/fabric/dispatcher/ActionRegistry.kt`

Tests and workflow:

- `common/src/test/kotlin/com/blackboxpro/common/action/PriorityInputActionSupportTest.kt`
- `docs/workflow/spec.md`
- `docs/workflow/status.md`
- `docs/workflow/tasks/sprint-004.md`
- `docs/workflow/reviews/sprint-004-contract-review.md`
- `docs/workflow/qa/sprint-004-qa.md`
- `docs/workflow/main-log.md`
- `knowledge/tasks/current-task.md`
- `knowledge/tasks/timeline.md`

Cleanup note:

- `java_pid53476.hprof` was an untracked heap dump from a failed OOM build attempt. It was verified as a repo-root single file and removed during branch cleanup after Sprint 4 QA.

## Implementation Summary

- `1.21.11` NeoForge runtime: added all six action implementations, `ScreenMouseHelper`, `ScreenKeyboardHelper`, and registry entries.
- `1.21.11` Fabric: added Fabric-specific action/helper implementations and registry entries.
- `1.21.1` NeoForge runtime: added all six action implementations, helpers, and registry entries.
- `1.21.1` Fabric: added Fabric-specific action/helper implementations and registry entries.
- Common tests now verify stable `ActionCatalog` params, registration in priority/modern registries, and implementation file presence.
- `ActionCatalog`, plugin source, `1.20.1`, `1.12.2`, test-cell scripts, and Gradle files were not modified.

## Executed Checks

```powershell
rg -n "move_mouse|click_mouse|click_screen_at|query_cursor_state|key_press|type_text" "mod/1.21.11" "mod/1.21.1" "plugin/src/main/kotlin" "common/src/test/kotlin"
```

Result: PASS. The six action ids are present in modern implementation/registry paths and common tests. Plugin source was searched as a catalog/wrapper alignment check; no plugin source change was required.

```powershell
.\gradlew.bat -p common test --no-daemon
```

Result: PASS.

```powershell
$env:JAVA_TOOL_OPTIONS="-Xmx8g"
.\gradlew.bat common_build plugin_build mod2111_build mod1211_build --no-daemon
```

Result: PASS. This aggregate build required Java 21 and a temporary local proxy on `127.0.0.1:7890` because existing Gradle properties point dependency resolution at `localhost:7890`. The proxy was stopped afterward.

```powershell
git diff --name-only -- "mod/1.20.1" "mod/1.12.2" "common/src/main/kotlin/com/blackboxpro/common/action/ActionCatalog.kt" "plugin/src/main/kotlin" "scripts/test-cells"
```

Result: PASS. No denied-path diff output.

```powershell
git diff --check
```

Result: PASS for whitespace. Git reported Windows LF-to-CRLF warnings only.

```powershell
git status --short
```

Result: PASS with expected Sprint 4 source/docs changes. The earlier untracked `java_pid53476.hprof` residual artifact has been removed.

## Runtime Evidence

- `1.21.11` Fabric: `BLOCKED` / unverified. No real same-version `/status` endpoint was available.
- `1.21.11` NeoForge: `BLOCKED` / unverified. No real same-version `/status` endpoint was available.
- `1.21.1` Fabric: `BLOCKED` / unverified. No real same-version `/status` endpoint was available.
- `1.21.1` NeoForge: `BLOCKED` / unverified. No real same-version `/status` endpoint was available.

No `1.20.1` or `1.12.2` runtime evidence is substituted for this sprint.

## Findings

- No denied-path source changes were found.
- No `ActionCatalog` id or parameter change was made.
- No plugin wrapper or test-cell script change was required.
- Runtime behavior still needs real `1.21.x` endpoint smoke before claiming `1.21.x` runtime PASS.

## Recommendation

Sprint 4 can be marked `done` for implementation, static search, common tests, aggregate build, and denied-path compliance.

Next legal action: commit/merge the Sprint 4 branch, then open Sprint 5 contract for `cell-20/21/22` Forge 1.12.2 business-mod matrix evidence.
