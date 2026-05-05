# 当前任务

## 背景

- BlackBoxPro 当前处于 P/G/E Sprint 6：Germ 真实客户端物理点击证据链。
- Sprint 6 contract：`docs/workflow/tasks/sprint-006.md`。
- 当前状态仍是 `fix`，不是 `PASS` / `done`。
- 多 bot orchestrator、`germ_gui_part_dos execute=true` 替代物理点击、现代端同步都不属于当前 Sprint。

## 当前目标

修复 Sprint 6 runtime QA 失败：让 `click_germ_component` 能在真实 Germ 页面触发客户端 Germ 点击路径，并产生可观察业务副作用。

## 本次已完成

- 强化了 Forge `1.12.2` `click_germ_component` 返回证据：
  - selected coordinate
  - target component and bounds
  - component click report
  - fallback screen click report
  - probe warnings
- 修正了组件候选判定：
  - `component-shape` obfuscated numeric candidates 不再被静默跳过。
  - 组件调用记录 `ok` 和 `handled`，避免把返回值语义和调用成功混在一起。
  - `componentClick.coordinate` 记录 screen 坐标，并保留 relative 坐标。
- Codex 完成 static/build/runtime QA；DeepSeek worker 先前因预算上限失败，没有可用 QA report。

## 已确认事实

- `common test` 通过。
- `common_build plugin_build forge1122_build` 通过。
- denied-path diff 为空：未改 `ActionCatalog`、现代端、plugin Germ server action、test-cell scripts 或 Gradle 配置。
- `cell-01` plugin `/status` 和 mod `/status` 均 ready。
- 真实 Germ 页面可打开：`/gp open zzzderk 分类商城正式模板`。
- `query_germ_hit_test` 在 `x=138,y=84` 能命中 `商品2点券` 所在区域的 Germ 候选组件。
- `germ_gui_part_dos execute=false` 能解析出 `商品2点券` 的 `lmshop buy 2 player_points <redacted-token>`，证明语义业务入口存在。
- 最新 `click_germ_component` 能走 `component` 路径并调用 `ALLATORIxDEMO(float,float):void`。
- PlayerPoints before/after 仍为 `7988 -> 7988`，真实点击未触发购买副作用。
- 父级目标 `root.ALLATORIxDEMO.else[0].class[58]` 中心点也未触发副作用。
- cleanup 已完成：`cell-01` stop/release 成功，`25570/38080/38081` 无监听，匹配 `cmd/java/javaw` 进程为 0。

## 当前结论

Sprint 6 当前仍为 `FAIL/fix`，不是 `PASS`。真实页面和业务状态观测口都可用，但当前组件反射路径没有业务副作用。

## 下一步

1. 动作：定位 Germ screen/event queue 真实点击 hook。
   验证：`click_germ_component` 返回的路径不只是普通 obfuscated setter/paint/update 方法，而是能触发 Germ click event 或组件 callback。
2. 动作：如果无法稳定定位 hook，重写 Sprint 6 contract 为固定 Germ 测试页。
   验证：测试页提供明确 click side effect，例如 scoreboard、日志、GUI 状态或数据库可观测变化。
3. 动作：重跑 static/build/runtime QA。
   验证：`common test`、`common_build plugin_build forge1122_build`、denied-path diff、`git diff --check` 全部通过；runtime 有 before/after 业务副作用。

## 验证记录

- `2026-05-05 00:48 +08:00`：`docs/workflow/qa/sprint-006-qa.md` 首行 `### FAIL: bbp-sprint-006-germ-real-physical-click`。
- `2026-05-05 16:40 +08:00`：component hook retest 仍失败；`ALLATORIxDEMO(float,float):void` 可调用，但 PlayerPoints `7988 -> 7988`。
- cleanup：stop `ok=true`，release `released=true`，端口/进程检查为 0。
