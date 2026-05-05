### PASS: bbp-sprint-003-priority-input-support

# Sprint 003 QA: Priority Input Support

## Scope

Sprint 3 was reprioritized from `1.21.x` modern input sync to Forge `1.20.1` plus Forge `1.12.2` priority support.

This QA covers build/static evidence plus real runtime smoke for Forge `1.12.2` and Forge `1.20.1`.

## Changed Files

Implementation:

- `build.gradle.kts`
- `mod/1.20.1/src/main/kotlin/com/blackboxpro/forge/dispatcher/ActionRegistry.kt`
- `mod/1.20.1/src/main/kotlin/com/blackboxpro/forge/action/client/MoveMouseAction.kt`
- `mod/1.20.1/src/main/kotlin/com/blackboxpro/forge/action/client/ClickMouseAction.kt`
- `mod/1.20.1/src/main/kotlin/com/blackboxpro/forge/action/client/ClickScreenAtAction.kt`
- `mod/1.20.1/src/main/kotlin/com/blackboxpro/forge/action/client/KeyPressAction.kt`
- `mod/1.20.1/src/main/kotlin/com/blackboxpro/forge/action/client/TypeTextAction.kt`
- `mod/1.20.1/src/main/kotlin/com/blackboxpro/forge/action/query/QueryCursorStateAction.kt`
- `mod/1.20.1/src/main/kotlin/com/blackboxpro/forge/util/ScreenMouseHelper.kt`
- `mod/1.20.1/src/main/kotlin/com/blackboxpro/forge/util/ScreenKeyboardHelper.kt`
- `common/src/test/kotlin/com/blackboxpro/common/action/PriorityInputActionSupportTest.kt`

Workflow and handoff:

- `docs/workflow/status.md`
- `docs/workflow/spec.md`
- `docs/workflow/tasks/sprint-003.md`
- `docs/workflow/reviews/sprint-003-contract-review.md`
- `docs/workflow/main-log.md`
- `docs/workflow/qa/sprint-003-qa.md`
- `knowledge/tasks/current-task.md`
- `knowledge/tasks/timeline.md`

Existing dirty file not owned by this sprint:

- `scripts/test-cells/Invoke-TestCell.ps1`

## Implementation Summary

- Forge `1.20.1` now registers all six target input actions.
- Forge `1.20.1` has client-local mouse helpers using the Minecraft window and current screen APIs, not Windows global mouse injection.
- Forge `1.20.1` has client-local keyboard helpers for current-screen typing and key-binding press/release behavior.
- Forge `1.20.1` preserves the `BACK` keyboard alias by normalizing it to `BACKSPACE`, matching the existing 1.12.2 compatibility expectation.
- Forge `1.12.2` already had the target actions and was not modified in this sprint.
- `ActionCatalog` was not modified.
- Root `forge1201_build` now lets the child `mod/1.20.1` Gradle wrapper use the user Gradle cache, avoiding the incompatible root Gradle 9 child invocation path.
- Root `forge1201_build` now finalizes `collectJars`, so `Sync-TestCell1201Artifacts.ps1` does not pick a stale root `build/libs` jar after a standalone 1.20.1 build.

## Executed Checks

Static action search:

```powershell
rg -n "move_mouse|click_mouse|click_screen_at|query_cursor_state|key_press|type_text" "mod/1.20.1" "mod/1.12.2" "plugin/src/main/kotlin" "common/src/test/kotlin"
```

Result: PASS. The target actions appear in `1.20.1`, `1.12.2`, plugin wrappers/catalog, and common tests.

Common tests:

```powershell
.\gradlew.bat -p common test --no-daemon
```

Initial result: BLOCKED by shell environment because `JAVA_HOME` was `C:/Program Files/Java/jdk1.8.0_481`; root Gradle 9 requires Java 17+.

Rerun with local Java 21:

```powershell
$env:JAVA_HOME=(Resolve-Path "../.local-tools/temurin21/jdk-21.0.10+7").ProviderPath
$env:PATH="$env:JAVA_HOME/bin;$env:PATH"
.\gradlew.bat -p common test --no-daemon
```

Result: PASS. `BUILD SUCCESSFUL in 11s`.

Priority build matrix:

```powershell
$env:JAVA_HOME=(Resolve-Path "../.local-tools/temurin21/jdk-21.0.10+7").ProviderPath
$env:PATH="$env:JAVA_HOME/bin;$env:PATH"
.\gradlew.bat common_build plugin_build forge1201_build forge1122_build --no-daemon
```

Result: PASS. Exit code 0. Output contained Java deprecation / unchecked warnings only. After the final `forge1201_build`, root `build/libs/BlackBoxPro-forge-1.20.1-2.2.4.jar` and `mod/1.20.1/build/libs/BlackBoxPro-forge-1.20.1-2.2.4.jar` had matching size and timestamp.

Whitespace and status:

```powershell
git diff --check
git status --short
```

Result: PASS. `git diff --check` exit code 0 with Windows LF-to-CRLF warnings only. `git status --short` shows expected sprint files plus pre-existing dirty `scripts/test-cells/Invoke-TestCell.ps1`.

Denied-path self-check:

```powershell
git diff --name-only -- "mod/1.21.11" "mod/1.21.1" "mod/1.12.2" "common/src/main/kotlin/com/blackboxpro/common/action/ActionCatalog.kt" "plugin/src/main/kotlin"
```

Result: PASS. No output.

Runtime smoke:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File "scripts/test-cells/Invoke-TestCell.ps1" -Mode ensure -CellId cell-01
powershell -NoProfile -ExecutionPolicy Bypass -File "scripts/test-cells/Invoke-TestCell1201.ps1" -Mode ensure -CellId cell-06
```

Forge `1.12.2` runtime:

- Cell: `cell-01`
- Plugin `/status`: `version=2.2.4`, `ready=true`, `httpPort=38080`
- Mod `/status`: `platform=forge`, `httpPort=38081`, `actions=114`, `ready=true`
- Relay: plugin `/execute` `query_player_state` for `zzzderk` returned `status=success`
- Input smoke:
  - `open_inventory`: success
  - `query_cursor_state`: success, `screenClass=GuiInventory`, `screenType=player_inventory`, `scaledWidth=428`, `scaledHeight=241`
  - `move_mouse x=120 y=80`: success, cursor reported `mouseX=120`, `mouseY=80`
  - `click_screen_at x=120 y=80 button=0 clickCount=1`: success
  - `key_press key=A`: success, `mode=screen_key_typed`, `typedCount=1`
  - `key_press key=T`: success, opened `GuiChat`
  - `type_text text="bbp runtime 1122"`: success on `GuiChat`, `typedCount=16`
  - `key_press key=ENTER`: success
- Cleanup: `Invoke-TestCell.ps1 -Mode stop -CellId cell-01` freed `25570/38080/38081`; `Release-TestCell.ps1` returned `released=true`; follow-up checks found no listeners and no matching `cmd/java/javaw` processes.

Forge `1.20.1` runtime:

- Cell: `cell-06`
- Sync: `Sync-TestCell1201Artifacts.ps1 -CellIds cell-06` copied `BlackBoxPro-Plugin-2.2.4.jar` and `BlackBoxPro-forge-1.20.1-2.2.4.jar`
- Plugin `/status`: `version=2.2.4`, `ready=true`, `httpPort=38130`
- Mod `/status`: `platform=forge`, `version=2.2.4`, `httpPort=38131`, `actions=122`, `ready=true`
- Relay: plugin `/execute` `query_player_state` for `bot_player` returned `status=success`
- Input smoke:
  - `open_inventory`: success
  - `query_cursor_state`: success, `screenClass=InventoryScreen`, `screenType=player_inventory`, `title=Crafting`, `slotCount=46`
  - `move_mouse x=120 y=80`: success, cursor reported `mouseX=120`, `mouseY=80`
  - `click_screen_at x=120 y=80 button=0 clickCount=1`: success
  - `key_press key=A`: success, `mode=screen_key_typed`, `typedCount=1`
  - `key_press key=T`: success from no-screen state, `mode=key_binding`, opened `ChatScreen`
  - `type_text text="bbp runtime 1201"`: success on `ChatScreen`, `typedCount=16`
  - `key_press key=ENTER`: success, chat screen closed
- Cleanup: `Invoke-TestCell1201.ps1 -Mode stop -CellId cell-06` freed `25615/38130/38131`; `Release-TestCell.ps1 -ConfigPath cells-1201.json` returned `released=true`; follow-up checks found no listeners and no matching `cmd/java/javaw` processes.

Runtime fixes discovered during QA:

- The first 1.20.1 runtime attempt loaded an old root `build/libs` jar and returned `Unknown action` for the new input actions. Running `collectJars` fixed the immediate smoke, and `forge1201_build` now finalizes `collectJars` to prevent recurrence.
- The first 1.20.1 implementation failed with `Failed to resolve window metrics`; `ScreenMouseHelper` now reads Mojang `Window` APIs directly.
- Hidden 1.20.1 chat smoke required local test-cell option `pauseOnLostFocus:false` in `G:/MC/game/BlackBoxProTestCells/cell-06/.minecraft/versions/1.20.1-Forge_47.3.0/options.txt`. This is a local runtime setting, not a repo file.

## Findings

- No blocking build/static findings.
- No blocking runtime findings for Forge `1.12.2` or Forge `1.20.1`.
- `scripts/test-cells/Invoke-TestCell.ps1` is dirty in the working tree but was not modified by this sprint and is excluded from the Sprint 3 compliance decision.
- `1.21.11` / `1.21.1` remain out of scope for this sprint and must not be claimed as supported by this QA.

## Recommendation

Build/static QA and priority-version runtime smoke pass. Sprint 3 can stay `done`.

Next legal action: start a new sprint for deferred `1.21.x` input sync, or separately plan multi-bot orchestration.
