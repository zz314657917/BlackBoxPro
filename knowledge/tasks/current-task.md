# 当前任务

## 背景

- BlackBoxPro P/G/E Sprint 6：`bbp-sprint-006-germ-real-physical-click` 已完成。
- Sprint 6 contract：`docs/workflow/tasks/sprint-006.md`。
- Sprint 6 QA：`docs/workflow/qa/sprint-006-qa.md`，首行 `### PASS: bbp-sprint-006-germ-real-physical-click`。
- 多 bot orchestrator、Germ/Lmshop 生产页进一步 hardening、现代端同步都不属于已完成的 Sprint 6。

## 已完成

- 固定 Germ 测试页已落库：`docs/workflow/fixtures/germ/blackboxpro-fixed-click.yml`。
- `click_germ_component` 增加可选参数 `screenClickPolicy`：
  - 默认 `on_component_failure`，保持原有 fallback 行为。
  - Sprint 6 runtime 验收使用 `screenClickPolicy=always`，避免组件反射调用成功后跳过真实 screen click。
- 固定页 runtime smoke 通过：
  - 页面：`blackboxpro_fixed_click`
  - 命令：`/gp open zzzderk blackboxpro_fixed_click`
  - 点击点：`x=214,y=113`
  - hit best id：`root.ALLATORIxDEMO.else[0].class[2]`
  - hit best class：`com.germmc!.OO0O0OO0OO0O`
  - selected bounds：`x=171.1999969482422`, `y=106.04000091552734`, `width=85.5999984741211`, `height=14.460000038146973`
  - `click_germ_component`：`clickPath=component+screen`
  - component signature：`ALLATORIxDEMO(float,float):void`
  - fallback：`fallbackScreenClick.ok=true`, `fallbackPath=screen`, `reflectiveScreenClick.ok=true`
  - side effect：`query_chat_history` 从 `beforeMarkerCount=0` 到 `afterMarkerCount=1`
  - marker：`<zzzderk> BBP_GERM_FIXED_CLICK_MARKER`

## 验证记录

- `.\gradlew.bat -p common test --no-daemon`：`BUILD SUCCESSFUL`。
- `.\gradlew.bat common_build plugin_build forge1122_build --no-daemon`：exit code `0`，仅 Java deprecation/unchecked warnings。
- static search 覆盖 `click_germ_component` / Germ query / `germ_gui_part_dos` / `screenClickPolicy` / fixed marker。
- denied-path diff 为空：未改 `ActionCatalog`、现代端、plugin Germ server action、test-cell scripts 或 Gradle 配置。
- `git diff --check`：无 whitespace error，仅 CRLF warning。
- runtime cleanup：
  - `cell-01` stop `ok=true`
  - release `released=true`
  - transient Germ fixture removed：`fixtureExists=false`
  - `25570/38080/38081` listener count `0`
  - matching `cmd/java/javaw` process count `0`

## 当前结论

Sprint 6 可以视为 `done`。真实 Lmshop 页面仍是诊断失败证据：组件/屏幕 hook 可调用但未产生 PlayerPoints 业务副作用；后续如果要继续 harden Lmshop 生产页，需要单独开 contract。

## 下一步

1. 起草 Sprint 7 multi-bot scenario orchestrator contract。
2. 保持边界：一个真实客户端进程对应一个玩家身份；多 bot 由 repo 侧 orchestrator 编排多个 cell，不塞进单个 Mod。
3. 不把 Sprint 7 和 Germ/Lmshop 生产页进一步 hardening 混在一个 Sprint。
