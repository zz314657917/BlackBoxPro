# BlackBoxPro 时间轴

## 2026-05-05 19:27 +08:00 - Sprint 6 fixed Germ page PASS

- Sprint 6 contract 已从 Lmshop 生产样页验收切到固定 Germ 测试页：`docs/workflow/fixtures/germ/blackboxpro-fixed-click.yml`。
- `click_germ_component` 增加可选 `screenClickPolicy`；默认 `on_component_failure` 保持原行为，验收用 `always` 让组件反射成功后仍执行 screen click。
- 固定页 `blackboxpro_fixed_click` 在 `cell-01` 通过 runtime smoke：`clickPath=component+screen`，component signature `ALLATORIxDEMO(float,float):void`，screen fallback `reflectiveScreenClick.ok=true`。
- 验收副作用为聊天 marker：同一 since 窗口内 `beforeMarkerCount=0`，点击后 `afterMarkerCount=1`，内容 `<zzzderk> BBP_GERM_FIXED_CLICK_MARKER`。
- 构建/静态门禁通过：`common test` 成功，`common_build plugin_build forge1122_build` exit code `0`，denied-path diff 为空，`git diff --check` 无 whitespace error。
- cleanup 完成：`cell-01` stop/release 成功，临时 Germ fixture 已移除，`25570/38080/38081` 无监听，匹配 `cmd/java/javaw` 进程为 0。
- Sprint 6 状态推进到 `done`；下一步是 Sprint 7 multi-bot scenario orchestrator contract。

## 2026-05-05 16:40 +08:00 - Sprint 6 component hook retest still failed

- Sprint 6 remains `fix`, not `done`.
- `component-shape` obfuscated numeric candidates are now invoked instead of being skipped.
- `click_germ_component` can invoke `ALLATORIxDEMO(float,float):void` with `clickPath=component` and `componentClick.ok=true`.
- Real Germ page remained available via `/gp open zzzderk 分类商城正式模板`.
- Runtime business evidence still failed: PlayerPoints for `zzzderk` stayed `7988 -> 7988`.
- Parent target `root.ALLATORIxDEMO.else[0].class[58]` at `x=137.84,y=108.13` also stayed `7988 -> 7988`.
- Static/build gates passed: `common test`, `common_build plugin_build forge1122_build`, denied-path diff, and `git diff --check`.
- Cleanup was clean: `cell-01` stop/release succeeded; `25570/38080/38081` and matching `cmd/java/javaw` checks were empty.
- Next legal action: find a real Germ screen/event queue hook, or rewrite Sprint 6 around a fixed Germ test page with an observable side effect.

## 2026-05-05 00:48 +08:00 - Sprint 6 runtime QA failed

- Sprint 6 implementation/build gates passed, but runtime acceptance failed.
- `click_germ_component` now returns structured evidence and no longer treats generic obfuscated numeric methods like `ALLATORIxDEMO(float,float):void` as a successful component click.
- Real Germ page was available via `/gp open zzzderk 分类商城正式模板`; `query_germ_hit_test` at `x=138,y=84` hit the target region.
- `germ_gui_part_dos execute=false` resolved the `商品2点券` buy command, confirming the semantic business path exists; it was not executed and not used as PASS evidence.
- Final `click_germ_component` result used `clickPath=screen`, `componentClick.ok=false`, `fallbackScreenClick.ok=true`.
- Business side effect failed: PlayerPoints for `zzzderk` stayed `7988 -> 7988`.
- Cleanup was clean: `cell-01` stop/release succeeded; `25570/38080/38081` and matching `cmd/java/javaw` checks were empty.
- Current phase is `fix`; next work is to find a real Germ client click hook or rewrite the contract if no stable hook exists.

## 2026-05-04 23:10 +08:00 - Sprint 6 Germ real click contract approved

- 当前阶段：P/G/E 已从 Sprint 5 `done` 推进到 Sprint 6 `contract-approved`。
- 本段重点：新增 `docs/workflow/tasks/sprint-006.md`，目标是增强 `click_germ_component` 的真实客户端/Germ 点击路径，并用真实 Germ 页面业务副作用验收。
- 审核记录：新增 `docs/workflow/reviews/sprint-006-contract-review.md`，verdict 为 `APPROVED`。
- 关键边界：`germ_gui_part_dos execute=true` 仍只能证明 Germ YAML `clickDos` 语义执行，不能作为物理点击 PASS 证据。
- 验收口径：PASS 必须包含 screen coordinate、component bounds、client event/method invocation 和业务 before/after；没有真实页面或副作用观测时应报 `BLOCKED`。
- 下一步：按 Sprint 6 contract 实现或调用 bounded developer worker，随后跑 static/build/runtime QA 并写 `docs/workflow/qa/sprint-006-qa.md`。

## 2026-05-04 21:26 +08:00 - Sprint 5 mod1122 matrix recovered and passed

- 当前阶段：Sprint 5 已从 `qa` 收口为 `done`，`docs/workflow/qa/sprint-005-qa.md` 首行已更新为 `### PASS: bbp-sprint-005-mod1122-matrix-evidence`。
- 修复过程：`cell-21` 先前失败来自 test-cell 环境漂移，已从 `cell-20` baseline 重新铺环境，并释放残留 lease `bbp-sprint-005-cell-21-smoke-retest2`。
- 验证结果：`cell-21 startup/smoke` 通过，`run_test smoke passed=22 failed=0 skipped=0 total=22 totalMs=8070`，截图 `integration_smoke_1777900827/001_catalog_screenshot.png`；`cell-22 startup/smoke` 通过，`run_test smoke passed=22 failed=0 skipped=0 total=22 totalMs=8136`，截图 `integration_smoke_1777901180/001_catalog_screenshot.png`。
- cleanup：三台 cell 的 stop/release 均成功，最终端口与 `cmd/java/javaw` 进程检查均为空。
- 下一步：起草 Sprint 6 Germ 真物理点击 contract，multi-bot orchestrator 仍单独规划。

## 2026-05-04 20:35 +08:00 - Sprint 5 mod1122 matrix QA failed on cell-21

- `cell-20 startup/smoke` 通过，`cell-21 startup` 失败，`cell-21 smoke` 与 `cell-22` 按 stop rule 未执行。
- 失败点最初表现为 `38211/status` 没有起来；后续日志确认是 `cell-21` 客户端环境漂移，缺少与 `cell-20` 一致的 mod 基线。
- 当时已确认 cleanup 干净，未留下监听端口或 test-cell 进程。

## 2026-05-04 18:10 +08:00 - Sprint 5 contract approved

- 新增 `docs/workflow/tasks/sprint-005.md`，要求 `cell-20/21/22` 在固定 CloudStorage jar 下完成 `startup` + `smoke`。
- 新增 `docs/workflow/reviews/sprint-005-contract-review.md`，verdict 为 `APPROVED`。
- 该 Sprint 只允许测试与 QA 文档，不允许改源码、Gradle、test-cell 脚本或全局 `.codex`。

## 2026-05-04 17:45 +08:00 - Sprint 4 modern input sync QA closed

- `1.21.11` / `1.21.1` Fabric + NeoForge 的六个输入 action 已完成 build/static QA。
- 由于缺少真实同版本 `/status` endpoint，runtime smoke 仍记为 `BLOCKED` / 未验证。
- Sprint 4 结束后，主线重新回到 1.12.2 mod1122 矩阵证据。

## 2026-05-04 15:53 +08:00 - Sprint 4 contract drafted

- Sprint 4 目标是同步 `1.21.11` / `1.21.1` Fabric + NeoForge 的输入 action。
- Contract 明确禁止改 `ActionCatalog`、`1.20.1`、`1.12.2`、test-cell 脚本和 Gradle 配置。

## 2026-05-04 11:09 +08:00 - Sprint 3 priority runtime smoke passed

- `cell-01` 通过 1.12.2 runtime smoke，`cell-06` 通过 1.20.1 runtime smoke。
- 这一步把优先级版本固定为 `1.20.1` + `1.12.2`，`1.21.x` 同步目标延后。

## 2026-05-04 06:04 +08:00 - Sprint 3 build/static QA passed

- `common test`、`common_build plugin_build forge1201_build forge1122_build` 全部通过。
- 通过静态搜索确认 priority input action 已落在 `1.20.1` / `1.12.2` 代码与测试面。

## 2026-05-04 05:58 +08:00 - Sprint 3 priority changed

- 用户明确要求 Sprint 3 优先支持 `1.20.1` 和 `1.12.2`。
- 旧的 `1.21.x` 同步目标改为后续计划。

## 2026-05-03 22:04 +08:00 - Sprint 3 contract approved

- 原始 Sprint 3 contract 先针对 `1.21.x` modern input sync，后续再调整优先级。
- contract 要求 build/static 必须通过，runtime PASS 必须来自真实同版本 `/status` 和 smoke。

## 2026-05-03 21:36 +08:00 - Sprint 2 CloudStorage mod1122 smoke validated

- `cell-20` 以固定 CloudStorage jar 完成 startup / smoke，`run_test smoke passed=22 failed=0 skipped=0 total=22 totalMs=8081`。
- 这一步证明了 mod1122 矩阵与 CloudStorage 业务模组的可重复 smoke 路径。

## 2026-05-03 19:05 +08:00 - Sprint 2 contract approved

- Sprint 2 目标是把 CloudStorage jar 在 `cell-20..22` 上完成 `startup` + `smoke`。
- contract 明确不改源码、不改 test-cell 脚本，只做运行态证据。

## 2026-05-03 18:45 +08:00 - P/G/E workflow landed

- 新增 `docs/workflow/status.md`、`docs/workflow/spec.md`、`docs/workflow/tasks/sprint-001.md`、`docs/workflow/main-log.md`。
- P/G/E 入口固定到 `docs/workflow/status.md`，后续 Sprint 都按 contract / QA 走。

## 2026-05-01 16:30 +08:00 - Germ clickDos 显式执行

- `germ_gui_part_dos` 已能解析 Germ YAML `clickDos` 语义。
- 这一步明确了语义执行和真实物理点击的边界，后续 Germ 真点击仍需单独 contract。

## 2026-04-28 18:10 +08:00 - 1.12.2 test-cell 超平坦基线统一

- `cell-01..05`、`cell-10`、`cell-20..22` 的 1.12.2 测试服统一为超平坦基线。
- 这一步收紧了 1.12.2 smoke 的环境差异，减少 world 配置漂移。

## 2026-05-05 18:50 +08:00 - Sprint 6 Germ physical click retest stayed fail

- 在 `cell-01` 上继续测了 Germ 真实物理点击链路。
- `click_germ_component`、`click_screen_at` 和 root synthetic screen hook `ALLATORIxDEMO(OOOO0O000OO0)` 都可调用，但 PlayerPoints 仍然是 `7988 -> 7988`。
- 这次 retest 结束后已 stop / release，端口和进程收口为 0。
