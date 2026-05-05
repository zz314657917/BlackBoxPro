### PASS: bbp-sprint-006-germ-real-physical-click

## Verdict

Sprint 6 now passes after the contract was amended to use a controlled fixed Germ test page.

The earlier Lmshop page remains diagnostic failure evidence: callable Germ hooks did not change PlayerPoints. The final accepted runtime target is the fixed page `blackboxpro_fixed_click` from `docs/workflow/fixtures/germ/blackboxpro-fixed-click.yml`.

Final accepted evidence: `click_germ_component` with `screenClickPolicy=always` returned `clickPath=component+screen`; the selected component invocation was recorded, screen fallback succeeded through the reflective screen path, and the deterministic chat marker changed from absent to present in the same time window: `beforeMarkerCount=0`, `afterMarkerCount=1`, `plain=<zzzderk> BBP_GERM_FIXED_CLICK_MARKER`.

## Changed Files Reviewed

- `mod/1.12.2/forge/src/main/kotlin/com/blackboxpro/forge/action/client/ClickGermComponentAction.kt`
- `mod/1.12.2/forge/src/main/kotlin/com/blackboxpro/forge/util/GermScreenProbeHelper.kt`
- `mod/1.12.2/forge/src/main/kotlin/com/blackboxpro/forge/util/ScreenMouseHelper.kt`
- `docs/workflow/fixtures/germ/blackboxpro-fixed-click.yml`
- `docs/workflow/tasks/sprint-006-test-worker.md`
- `docs/workflow/tasks/sprint-006.md`
- `docs/workflow/main-log.md`
- `docs/workflow/qa/sprint-006-qa.md`
- `docs/workflow/status.md`
- `knowledge/tasks/current-task.md`
- `knowledge/tasks/timeline.md`

## Static And Build Checks

```powershell
$javaHome = (Resolve-Path '../.local-tools/temurin21/jdk-21.0.10+7').Path
$env:JAVA_HOME = $javaHome
$env:Path = "$javaHome/bin;$env:Path"
rg -n "click_germ_component|query_germ_hit_test|query_germ_screen|germ_gui_part_dos" "mod/1.12.2" "plugin/src/main/kotlin" "common/src/test/kotlin" "knowledge" "docs/workflow"
git diff --name-only -- "common/src/main/kotlin/com/blackboxpro/common/action/ActionCatalog.kt" "common/src/main/kotlin" "mod/1.20.1" "mod/1.21.1" "mod/1.21.11" "plugin/src/main/kotlin/com/blackboxpro/plugin/http/ServerGermActions.kt" "scripts/test-cells" "build.gradle.kts" "settings.gradle.kts" "gradle.properties"
git diff --check
.\gradlew.bat -p common test --no-daemon
.\gradlew.bat common_build plugin_build forge1122_build --no-daemon
```

Results:

- Static search found the expected Germ click/query boundaries.
- Denied-path diff produced no output.
- `git diff --check` produced no whitespace errors.
- `common test` passed: `BUILD SUCCESSFUL`.
- `common_build plugin_build forge1122_build` passed with Java deprecation/unchecked warnings only; exit code `0`.

## Runtime Evidence

Runtime target:

- `cell-01`
- owner: `bbp-sprint-006-germ-real-click`
- plugin HTTP: `http://127.0.0.1:38080`
- mod HTTP: `http://127.0.0.1:38081`
- target player: `zzzderk`

The running cell was restarted after copying the fresh Forge 1.12.2 build artifact into the bot mods directory:

- source: `mod/1.12.2/build/libs/BlackBoxPro-forge-1.12.2-2.2.4.jar`
- runtime jar: `G:/MC/game/BlackBoxProTestCells/cell-01/.minecraft/versions/bot/mods/BlackBoxPro-forge-1.12.2-2.2.4.jar`
- final runtime jar size: `2538148`

Health checks:

- plugin `/status`: `ready=true`, `version=2.2.4`, `mode=dual`, `httpPort=38080`
- mod `/status`: `ready=true`, `platform=forge`, `actions=114`, `httpPort=38081`
- relay `query_player_state`: `status=success`

Real Germ page:

- opened with `chat_command`: `/gp open zzzderk 分类商城正式模板`
- `query_germ_screen`: `open=true`, `supported=true`, `probeMode=screen-class`, `screenClassName=com.germmc!.O000OOO0O0OO`, `componentCount=500`, `scaledWidth=428`, `scaledHeight=241`

Target hit-test:

- action: `query_germ_hit_test`
- coordinate: `x=138`, `y=84`
- `hitCount=4`
- first hit id: `root.ALLATORIxDEMO.else[0].class[12]`
- first hit class: `com.germmc!.OO0O0OO0OO0O`
- first hit bounds: `x=119.83999633789062`, `y=80.73500061035156`, `width=7.230000019073486`, `height=34.2400016784668`
- first hit candidate method hint: `ALLATORIxDEMO(float,float):void`

Semantic dry-run comparison:

- action: `germ_gui_part_dos`
- `execute=false`
- `guiName=分类商城正式模板`
- `partId=商品2点券`
- result: resolved a `playercmd<->lmshop buy 2 player_points <redacted-token>` command from `category-shop-full.yml`
- this was used only as semantic comparison, not as PASS evidence.

Final physical/client click attempt:

- action: `click_germ_component`
- coordinate: `x=138`, `y=84`
- `clickSucceeded=true`
- `clickPath=screen`
- selected target id: `root.ALLATORIxDEMO.else[0].class[12]`
- `componentClick.ok=false`
- component attempt: `no-invokable-germ-mouse-method`
- `fallbackScreenClick.ok=true`
- fallback invocations:
  - `mouseClicked:func_73864_a(int,int,int):void:ok=true`
  - `mouseReleased:func_146286_b(int,int,int):void:ok=true`

Business side-effect check:

- observer: `plugins/PlayerPoints/playerpoints.db`
- player: `zzzderk`
- before: `7988`
- after first current-jar click attempt: `7988`
- after alternate target-bounds-center click attempt: `7988`
- after final stricter-candidate click attempt: `7988`

Because no balance change, order effect, GUI state change, or other stable business side effect was observed, runtime acceptance failed.

## Fix Retest 2026-05-05 16:40 +08:00

Codex retested the Sprint 6 fix path directly after updating `GermScreenProbeHelper`:

- `component-shape` obfuscated numeric candidates are now invoked instead of being skipped.
- Component attempts now record `handled` separately from invocation `ok`.
- `componentClick.coordinate` now records screen coordinates plus relative coordinates under the selected bounds.

Verification after this fix:

- `git diff --check`: passed.
- denied-path diff: no output.
- `.\gradlew.bat -p common test --no-daemon`: `BUILD SUCCESSFUL`.
- `.\gradlew.bat common_build plugin_build forge1122_build --no-daemon`: exit code `0` with Java deprecation/unchecked warnings only.

Runtime retest on `cell-01`:

- real page opened with `/gp open zzzderk 分类商城正式模板`.
- observer: `F:/minecraft/test-cells/server-cell-01/plugins/PlayerPoints/playerpoints.db`.
- player: `zzzderk`, uuid `9527a0de-4acf-37c2-adcc-2bf5ef360c6b`.
- target `root.ALLATORIxDEMO.else[0].class[12]`, coordinate `x=138`, `y=84`.
- component attempt invoked `ALLATORIxDEMO(float,float):void`.
- result: `clickStatus=success`, `clickPath=component`, `componentClick.ok=true`, `fallbackScreenClick.ok=false`.
- PlayerPoints stayed `7988 -> 7988`.

Additional parent target check:

- target `root.ALLATORIxDEMO.else[0].class[58]`, coordinate `x=137.84`, `y=108.13`.
- component attempt invoked `ALLATORIxDEMO(float,float):void`.
- result: `clickStatus=success`, `clickPath=component`, `componentClick.ok=true`.
- PlayerPoints stayed `7988 -> 7988`.

Cleanup after retest:

- `Invoke-TestCell.ps1 -Mode stop -CellId cell-01`: `ok=true`.
- `Release-TestCell.ps1 -CellId cell-01 -Owner bbp-sprint-006-germ-real-click`: `released=true`.
- final listener check for `25570/38080/38081`: `0`.
- final matching `cmd/java/javaw` process check: `0`.

Retest verdict remains `FAIL`: the candidate Germ component method can be invoked, but there is still no observable business side effect. Sprint 6 must not move to `done`.

## Synthetic Screen Retest 2026-05-05 18:50 +08:00

Codex continued retesting the real Germ page with screen-level hooks that the root screen actually exposes:

- `click_screen_at` on the exact candidate center `136.95999717712402,84.3500006198883` stayed at PlayerPoints `7988 -> 7988`.
- `click_germ_component` with `syntheticScreenMethod=ALLATORIxDEMO()` returned `screenSyntheticClick.ok=true`, but PlayerPoints still stayed `7988 -> 7988`.
- `click_germ_component` with `syntheticScreenMethod=ALLATORIxDEMO(OOOO0O000OO0)` also returned `screenSyntheticClick.ok=true`, but PlayerPoints still stayed `7988 -> 7988`.
- The retest was stopped and released cleanly after the checks.

This confirms the current Germ physical-click / screen-synthetic chain still does not produce the required business side effect on the real shop page.

## Fixed Page Retest 2026-05-05 19:26 +08:00

The Sprint 6 contract was amended to use a controlled Germ page with a deterministic chat side effect:

- fixture source: `docs/workflow/fixtures/germ/blackboxpro-fixed-click.yml`
- transient runtime copy: `F:/minecraft/test-cells/server-cell-01/plugins/GermPlugin/gui/blackboxpro-fixed-click.yml`
- page id: `blackboxpro_fixed_click`
- side effect: `chat<->BBP_GERM_FIXED_CLICK_MARKER`

Code fix:

- `click_germ_component` now accepts optional `screenClickPolicy`.
- Default behavior remains `on_component_failure`.
- Runtime acceptance uses `screenClickPolicy=always` with `fallbackScreenClick=true` so a component reflection success does not short-circuit the real screen click path.

Static/build evidence after the fix:

- `.\gradlew.bat -p common test --no-daemon`: `BUILD SUCCESSFUL`.
- `.\gradlew.bat common_build plugin_build forge1122_build --no-daemon`: exit code `0`; Java deprecation/unchecked warnings only.
- `rg -n "click_germ_component|query_germ_hit_test|query_germ_screen|germ_gui_part_dos|screenClickPolicy|BBP_GERM_FIXED_CLICK_MARKER" ...`: expected hits found.
- denied-path diff: no output.
- `git diff --check`: no whitespace errors; CRLF warnings only.

Runtime target:

- `cell-01`
- owner: `bbp-sprint-006-fixed-germ`
- plugin `/status`: `ready=true`, `version=2.2.4`
- mod `/status`: `ready=true`, `actions=114`
- relay `query_player_state`: `status=success`
- fresh runtime jar copied to bot mods: `BlackBoxPro-forge-1.12.2-2.2.4.jar`, size `2582425`

Fixed-page evidence:

- opened with `/gp open zzzderk blackboxpro_fixed_click`.
- hit-test at `x=214,y=113`: `hitCount=3`.
- best hit id: `root.ALLATORIxDEMO.else[0].class[2]`.
- best hit class: `com.germmc!.OO0O0OO0OO0O`.
- selected horizontal bounds: `x=171.1999969482422`, `y=106.04000091552734`, `width=85.5999984741211`, `height=14.460000038146973`.
- before click marker query since `1777980404166`: `beforeMarkerCount=0`.
- `click_germ_component`: `status=success`, `clickPath=component+screen`, `screenClickPolicy=always`.
- component attempt: `componentClick.ok=true`, signature `ALLATORIxDEMO(float,float):void`.
- screen fallback: `fallbackScreenClick.requested=true`, `fallbackScreenClick.ok=true`, `fallbackPath=screen`, `reflectiveScreenClick.ok=true`, `nativeMouseClick.ok=false`.
- after click marker query: `afterMarkerCount=1`, plain text `<zzzderk> BBP_GERM_FIXED_CLICK_MARKER`.

This satisfies Sprint 6 runtime acceptance: the click action records coordinate/bounds/component evidence and the same real client screen-click path produces an observable Germ `clickDos` side effect.

## Cleanup Evidence

```powershell
powershell -ExecutionPolicy Bypass -File "scripts/test-cells/Invoke-TestCell.ps1" -Mode stop -CellId cell-01
powershell -ExecutionPolicy Bypass -File "scripts/test-cells/Release-TestCell.ps1" -CellId cell-01 -Owner "bbp-sprint-006-germ-real-click"
Get-NetTCPConnection -State Listen -ErrorAction SilentlyContinue | Where-Object { $_.LocalPort -in 25570,38080,38081 } | Select-Object LocalAddress,LocalPort,OwningProcess
Get-CimInstance Win32_Process | Where-Object { $_.Name -in "cmd.exe","java.exe","javaw.exe" -and ($_.CommandLine -like "*BlackBoxProTestCells*cell-01*" -or $_.CommandLine -like "*server-cell-01*") } | Select-Object ProcessId,Name,CommandLine
```

Results:

- stop returned `ok=true`
- release returned `released=true`
- final port check for `25570`, `38080`, `38081`: no output
- final matching `cmd/java/javaw` process check: no output

Latest fixed-page cleanup:

- `Invoke-TestCell.ps1 -Mode stop -CellId cell-01`: `ok=true`.
- `Release-TestCell.ps1 -CellId cell-01 -Owner bbp-sprint-006-fixed-germ`: `released=true`.
- transient fixture removed: `fixtureExists=false`.
- final listener check for `25570/38080/38081`: `listenerCount=0`.
- final matching `cmd/java/javaw` process check: `processCount=0`.

## Findings

- No denied-path source drift was found.
- DeepSeek worker was attempted but failed with `error_max_budget_usd`, so Codex performed the final QA directly.
- The original component candidate list exposed `ALLATORIxDEMO(float,float):void`; the fix retest now invokes that method and records it as a component-path attempt.
- Invoking `ALLATORIxDEMO(float,float):void` on both the small target and larger parent target did not trigger the Germ shop purchase.
- The fixed-page retest proved that the component reflection path alone is insufficient, but `click_germ_component` can now combine component evidence with an explicit screen click path and produce a Germ side effect.

## Next Legal Action

Sprint 6 can move to `done`.

Next sprint: draft Sprint 7 multi-bot scenario orchestrator contract. Do not mix Sprint 7 with further Germ/Lmshop production-page hardening unless a separate contract is approved.
