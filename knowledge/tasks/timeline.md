# BlackBoxPro 时间轴

## 2026-05-04 18:10 +08:00 - Sprint 5 mod1122 matrix contract approved

- 当前阶段：P/G/E 已从 Sprint 4 `done` 推进到 Sprint 5 `contract-approved`。
- 本段重点：新增 `docs/workflow/tasks/sprint-005.md`，目标是固定 CloudStorage jar 在 `cell-20/21/22` 全矩阵跑 Forge 1.12.2 business-mod `startup` + `smoke`。
- 已完成：新增 `docs/workflow/reviews/sprint-005-contract-review.md`，verdict 为 `APPROVED`；`docs/workflow/status.md` 已切到 `bbp-sprint-005-mod1122-matrix-evidence`。
- 关键决策：Sprint 5 补 Sprint 2 缺口，必须三格都通过；`cell-20` 单格证据不能替代 `cell-21/22`。
- 验证记录：本阶段只做 contract 和 handoff 文档，不启动 Minecraft、不运行 test-cell。
- 遗留问题：Sprint 5 runtime matrix 尚未执行，不能声明全矩阵 PASS。
- 下一步：顺序执行或委派 `cell-20`、`cell-21`、`cell-22` 的 CloudStorage startup/smoke acceptance commands，并写 `docs/workflow/qa/sprint-005-qa.md`。

## 2026-05-04 18:05 +08:00 - Sprint 4 branch cleanup

- 当前阶段：Sprint 4 已完成 QA 收口，并进入分支提交前清理。
- 本段重点：确认并删除 untracked `java_pid53476.hprof`，避免把 OOM heap dump 混进后续提交或 handoff。
- 已完成：路径校验确认目标为仓库根下单文件 `F:/mcplugins/BlackBoxPro-dev-2.0/java_pid53476.hprof`，大小约 795MB；随后已移除。
- 关键决策：heap dump 不是 Sprint 4 交付物，不进入 git；后续只提交现代端输入 action、workflow QA 和 handoff 文档。
- 验证记录：删除命令后 `Test-Path` 返回 `False`。
- 遗留问题：无。
- 下一步：跑 fresh 验证，stage 并提交 Sprint 4 分支。

## 2026-05-04 17:45 +08:00 - Sprint 4 modern input sync QA closed

- 当前阶段：Sprint 4 已从 `contract-approved` 推进到 `done`，QA 报告为 `docs/workflow/qa/sprint-004-qa.md`。
- 本段重点：`1.21.11` / `1.21.1` Fabric + NeoForge 均已同步 `move_mouse`、`click_mouse`、`click_screen_at`、`query_cursor_state`、`key_press`、`type_text`。
- 已完成：新增现代端 action/helper 文件与四个 registry 注册；扩展 `PriorityInputActionSupportTest` 覆盖现代 registry 和实现文件存在性。
- 关键决策：Sprint 4 PASS 只覆盖实现、静态搜索、common test、聚合构建与 denied-path compliance；`1.21.x` runtime PASS 必须等真实同版本 `/status` endpoint。
- 验证记录：`.\gradlew.bat -p common test --no-daemon` 通过；`JAVA_TOOL_OPTIONS=-Xmx8g` 下 `.\gradlew.bat common_build plugin_build mod2111_build mod1211_build --no-daemon` 通过；denied-path diff 无输出；`git diff --check` 仅有 LF-to-CRLF warning。
- 遗留问题：`1.21.11` / `1.21.1` runtime smoke 未跑，状态为 `BLOCKED` / 未验证。
- 下一步：整理提交 Sprint 4 分支；随后起草 Sprint 5 contract，做 `cell-20/21/22` Forge 1.12.2 业务模组矩阵验证。

## 2026-05-03 18:45 +08:00 - P/G/E 稳定主线 workflow 落盘

- 当前阶段：新增 `docs/workflow/status.md`、`docs/workflow/spec.md`、`docs/workflow/tasks/sprint-001.md` 和 `docs/workflow/main-log.md`，把 BlackBoxPro 稳定主线切到 P/G/E contract 流程。
- 本段重点：Sprint 1 只做 workflow 与真源收口 contract，不改源码、不启动 test-cell、不运行 Minecraft 客户端或服务端。
- 关键边界：HTTP relay 是当前生产主链；`germ_gui_part_dos` 是 Germ YAML / `clickDos` 语义执行，不等于真实物理点击；多 bot 编排不属于本轮计划，后续应作为 repo 侧 scenario orchestrator 单独设计。
- 审核记录：`docs/workflow/tasks/sprint-001.md` 已补齐显式 `Task ID` 字段，并通过 Evaluator contract review。
- QA 记录：新增 `docs/workflow/qa/sprint-001-qa.md`，静态验收通过；`git diff --check` 无 whitespace error，仅有 Windows line-ending warning。当前 phase 为 `done`。
- 下一步：起草 Sprint 2 contract，聚焦 `cell-20..22` 业务模组 smoke 与 cleanup 证据；Sprint 1 不进入源码实现。

## 2026-05-03 19:05 +08:00 - Sprint 2 mod1122 smoke contract approved

- 当前阶段：`docs/workflow/tasks/sprint-002.md` 已起草并通过 Evaluator review，phase 推进到 `contract-approved`。
- 本段重点：Sprint 2 固定使用 `F:/mcplugins/mod/CloudStorage/build/libs/cloudstorage-0.1.0-SNAPSHOT.jar` 作为真实 Forge 1.12.2 业务模组 jar，在 `cell-20..22` 池执行 startup 和 smoke。
- 执行边界：只允许运行 `Run-TestCellMod1122Regression.ps1` 的 `startup` / `smoke`，不改源码、不改 test-cell 脚本、不改 CloudStorage 仓库；不传 `-KeepCell` 或 `-ShowClient`。
- PASS 条件：两次命令 JSON 均为 `ok=true`，且 cleanup 没有 stop/release error；worker 需写 `docs/workflow/worker-results/bbp-sprint-002-mod1122-smoke-evidence-result.md`。
- 契约修正：本地 `Invoke-PgeWorker.ps1` 要求 worker report 首行为 `DONE` / `FAILED` / `BLOCKED`，Sprint 2 contract 已按该格式修正。

## 2026-05-03 21:36 +08:00 - Sprint 2 CloudStorage mod1122 smoke 验收完成

- 当前阶段：DeepSeek worker 两轮均因 `error_max_budget_usd` 停止，未写出合格 worker report；按 P/G/E stop rule 停止继续烧 worker 预算，改由 Codex/Evaluator 直接执行已批准 acceptance commands。
- 契约修正：Sprint 2 contract 已把命令运行根从原仓库绝对路径收口为当前 checkout/worktree root，并把 report 路径修正为 `docs/workflow/worker-results/bbp-sprint-002-mod1122-smoke-evidence-result.md`。
- 验收记录：固定 jar `F:/mcplugins/mod/CloudStorage/build/libs/cloudstorage-0.1.0-SNAPSHOT.jar` 存在；`startup` 抢到 `cell-20`，`ok=true`，stop/release 成功。
- smoke 证据：`smoke` 抢到 `cell-20`，`ok=true`；`run_test smoke` 返回 `passed=22 failed=0 skipped=0 total=22 totalMs=8081`，截图 `G:/MC/game/BlackBoxProTestCells/cell-20/.minecraft/versions/bot/screenshots/blackboxpro/zzzderk/integration_smoke_1777814618/001_catalog_screenshot.png` 存在，为 `854x480`、`31762` bytes。
- cleanup 证据：两轮均未传 `-KeepCell` 或 `-ShowClient`；最终端口 `25720/38200/38201` 监听数为 0，匹配 `cmd/java/javaw` test-cell 进程为 0，release `released=true`。
- 状态收口：新增 `docs/workflow/qa/sprint-002-qa.md`，`docs/workflow/status.md` phase 推进到 `done`；下一步是起草 Sprint 3 contract，聚焦现代端输入 action 同步与跨版本最小验证。

## 2026-05-03 22:04 +08:00 - Sprint 3 modern input sync contract approved

- 当前阶段：新增 `docs/workflow/tasks/sprint-003.md` 和 `docs/workflow/reviews/sprint-003-contract-review.md`，`docs/workflow/status.md` 推进到 `contract-approved`。
- 本段重点：Sprint 3 只聚焦现代 `1.21.11` / `1.21.1` 客户端线同步 6 个已在 1.12.2 验证过的输入 action：`move_mouse`、`click_mouse`、`click_screen_at`、`query_cursor_state`、`key_press`、`type_text`。
- contract 边界：`ActionCatalog` 仍是真源，禁止修改 action id 或参数顺序；禁止改 `mod/1.12.2/**`、`mod/1.20.1/**`、test-cell 脚本、Gradle 文件和全局 `.codex`。
- 验收口径：静态/构建命令必须跑 `common_build plugin_build mod2111_build mod1211_build`；现代 runtime PASS 只能来自真实 `1.21.x` `/status` 和 smoke，若无真实端点则必须 `BLOCKED`，不能用 `1.20.1` 或 `1.12.2` 证据替代。
- worker 注意：`Invoke-PgeWorker.ps1 -DryRun` 已能识别 Sprint 3 contract，默认 worktree 为 `E:/codex-worktrees/blackboxpro-dev-2.0/bbp-sprint-003-modern-input-sync`；考虑 Sprint 2 worker 超预算，Sprint 3 建议 code-only first pass 或提高预算后再调用。

## 2026-05-04 05:58 +08:00 - Sprint 3 priority changed to 1.20.1 + 1.12.2

- 当前阶段：用户明确要求优先支持 `1.20.1` 和 `1.12.2`，Sprint 3 从旧的 `1.21.x` modern input sync 改为 `bbp-sprint-003-priority-input-support`。
- 实现记录：移除旧的 `1.21.x` 临时实现方向，改为给 Forge `1.20.1` 补齐 `move_mouse`、`click_mouse`、`click_screen_at`、`query_cursor_state`、`key_press`、`type_text` 及 `ScreenMouseHelper` / `ScreenKeyboardHelper`。
- 基线策略：Forge `1.12.2` 已有同名 action 和 helper，本轮不改 `mod/1.12.2/**`，只用 common 测试确认注册和文件仍存在。
- 构建入口：`forge1201_build` 子 wrapper 需要使用用户级 Gradle cache，避免根 Gradle 环境下误用不兼容的 Gradle 9 child invocation。
- 待收口：继续跑 fresh `common test`、`common_build plugin_build forge1201_build forge1122_build`、`git diff --check` 和 `git status --short`，再写 Sprint 3 QA report；没有真实 `/status` 和 smoke 前不能声明 runtime PASS。

## 2026-05-04 06:04 +08:00 - Sprint 3 build/static QA passed

- QA 结论：`docs/workflow/qa/sprint-003-qa.md` 首行为 `### PASS: bbp-sprint-003-priority-input-support`；该 PASS 只覆盖 build/static，不覆盖运行态 smoke。
- 执行命令：`rg -n "move_mouse|click_mouse|click_screen_at|query_cursor_state|key_press|type_text" "mod/1.20.1" "mod/1.12.2" "plugin/src/main/kotlin" "common/src/test/kotlin"` 命中 `1.20.1`、`1.12.2`、plugin wrappers/catalog 和 common tests。
- 环境前提：当前 shell 默认 `JAVA_HOME=C:/Program Files/Java/jdk1.8.0_481` 会拦住根 Gradle 9；使用 `../.local-tools/temurin21/jdk-21.0.10+7` 后 `.\gradlew.bat -p common test --no-daemon` 通过。
- 构建证据：同一 Java 21 环境下 `.\gradlew.bat common_build plugin_build forge1201_build forge1122_build --no-daemon` 退出码 0，仅有 Java deprecation / unchecked warnings。
- 自查证据：`git diff --check` 退出码 0，仅有 LF-to-CRLF warning；denied-path 检查确认 `mod/1.21.11/**`、`mod/1.21.1/**`、`mod/1.12.2/**`、`ActionCatalog` 和 plugin source 无本轮 diff。
- 运行态状态：候选 `/status` 端口 `38201/38211/38221/38081/38171/38231/38241` 均不可用，本轮未跑 GUI/input smoke，不能声明 `1.20.1` 或 `1.12.2` runtime PASS。

## 2026-05-04 11:09 +08:00 - Sprint 3 priority runtime smoke passed

- QA 结论：`docs/workflow/qa/sprint-003-qa.md` 已从 build/static PASS 补齐为 `1.12.2` 和 `1.20.1` priority-version runtime PASS；`1.21.x` 仍不在本轮范围。
- 1.12.2 证据：`cell-01` plugin `/status` ready，mod `/status` 为 `actions=114 ready=true`；relay `query_player_state` for `zzzderk` 成功；`GuiInventory` 上 `query_cursor_state`、`move_mouse x=120 y=80`、`click_screen_at`、`key_press A` 成功；`GuiChat` 上 `type_text "bbp runtime 1122"` 返回 `typedCount=16`，`key_press ENTER` 成功。
- 1.12.2 cleanup：`Invoke-TestCell.ps1 -Mode stop -CellId cell-01` 返回 freed `25570/38080/38081=true`；`Release-TestCell.ps1` 返回 `released=true`；复查无监听端口和无匹配 `cmd/java/javaw` 进程。
- 1.20.1 证据：`Sync-TestCell1201Artifacts.ps1 -CellIds cell-06` 同步新 jar；`cell-06` plugin `/status` ready，mod `/status` 为 `actions=122 ready=true`；relay `query_player_state` for `bot_player` 成功；`InventoryScreen` 上 `query_cursor_state`、`move_mouse x=120 y=80`、`click_screen_at`、`key_press A` 成功；`ChatScreen` 上 `type_text "bbp runtime 1201"` 返回 `typedCount=16`，`key_press ENTER` 成功。
- 1.20.1 修复：第一次 runtime 用旧 root `build/libs` jar 时新 action 返回 `Unknown action`，已通过 `collectJars` 纠正，并让 `forge1201_build` 自动 `finalizedBy(collectJars)`；第一次新 action runtime 又暴露 `Failed to resolve window metrics`，已把 `ScreenMouseHelper` 改为直接读取 Mojang `Window` API。
- 1.20.1 cleanup：`Invoke-TestCell1201.ps1 -Mode stop -CellId cell-06` 返回 freed `25615/38130/38131=true`；`Release-TestCell.ps1 -ConfigPath cells-1201.json` 返回 `released=true`；复查无监听端口和无匹配 `cmd/java/javaw` 进程。
- 本地环境备注：隐藏 1.20.1 chat smoke 需要 `cell-06` 的本地 `options.txt` 为 `pauseOnLostFocus:false`；这是 test-cell 本地运行态设置，不是仓库文件。

## 2026-05-04 11:23 +08:00 - Sprint 3 final sanity and handoff alignment

- fresh verification：`git diff --check` 退出码 0，仅有 Windows LF-to-CRLF warning；denied-path 检查确认 `mod/1.21.11/**`、`mod/1.21.1/**`、`mod/1.12.2/**`、`ActionCatalog` 和 plugin source 无本轮 diff。
- fresh verification：`JAVA_HOME=../.local-tools/temurin21/jdk-21.0.10+7` 下 `.\gradlew.bat -p common test --no-daemon` 通过，`.\gradlew.bat common_build plugin_build forge1201_build forge1122_build --no-daemon` 退出码 0。
- fresh verification：目标 action 静态搜索命中 `mod/1.20.1`、`mod/1.12.2`、plugin wrappers/catalog 和 common tests。
- handoff：`knowledge/tasks/current-task.md` 已把旧的“runtime 未执行，不能声明 PASS”待验证项改为 Sprint 3 priority-version runtime PASS，并明确 `1.21.x` 与 multi-bot 仍需后续单独开计划。

## 2026-05-04 15:53 +08:00 - Sprint 4 modern input sync contract drafted

- 当前阶段：`docs/workflow/status.md` 已从 Sprint 3 `done` 推进到 Sprint 4 `contract-approved`。
- 本段重点：新增 `docs/workflow/tasks/sprint-004.md`，目标是把 `1.21.11` / `1.21.1` Fabric + NeoForge 同步到六个 priority input action：`move_mouse`、`click_mouse`、`click_screen_at`、`query_cursor_state`、`key_press`、`type_text`。
- contract 边界：禁止改 `ActionCatalog`、`1.20.1`、`1.12.2`、plugin source、test-cell 脚本、Gradle 配置和全局 `.codex`；实现优先复用现代 `runtime` 模块，loader 差异只放在 Fabric/NeoForge 边界。
- 验收口径：静态搜索、common tests、`common_build plugin_build mod2111_build mod1211_build`、denied-path diff 和 `git diff --check` 是基础门；runtime PASS 必须来自真实同版本 `/status` 与 smoke，没有端点就按版本/loader 报 `BLOCKED` 或未验证。
- 审核记录：新增 `docs/workflow/reviews/sprint-004-contract-review.md`，verdict 为 `APPROVED`。
- 下一步：进入 implementation，或按 contract 调用 bounded worker。

## 2026-05-01 16:42 +08:00 - Germ clickDos 分支最终复核与 test-cell 清理

- 当前阶段：`codex/germ-hit-test-dos` 已完成提交前收口验证，准备合并主线。
- fresh verification：`git diff --check` 无 whitespace error，仅有 Windows line-ending 提示；`JAVA_HOME=C:/Users/Administrator/.gradle/jdks/eclipse_adoptium-17-amd64-windows/jdk-17.0.18+8` 下执行 `common_build plugin_build forge1122_build` 退出码 0。
- 运行态复核：`cell-01` plugin `/status` ready，mod `/status` ready 且 actions=114；通过 plugin relay 执行 `query_player_state` 成功。
- Germ 语义复核：`query_germ_screen` 返回 `open=true supported=true`；`germ_gui_part_dos execute=false mode=player_command` 对 `分类商城正式模板/商品2点券` 解析出 `playercmd<->lmshop buy 2 player_points <token>`，未执行副作用。
- 清理记录：已执行 `Invoke-TestCell.ps1 -Mode stop -CellId cell-01` 和 `Release-TestCell.ps1 -CellId cell-01 -Owner codex-lmshop-germ-20260501-continue`；`cell-01 locked=false`，端口 `25570/38080/38081` 无监听。
- 技能同步：全局 `blackboxpro-local-regression` skill 已补充 `click_germ_component`、`germ_gui_part_dos`、dry-run 无副作用和“语义执行不等于物理点击”的说明。

## 2026-05-01 16:30 +08:00 - Germ clickDos 显式执行与 Lmshop 运行态证据

- 当前阶段：`germ_gui_part_dos` 已从诊断动作推进到可执行 Germ YAML `clickDos` 语义的显式 action。
- 本段重点：解决 Lmshop Sprint 2 中“自动化坐标点击不触发 Germ clickDos”的测试工具缺口，同时保持 query / dry-run 无副作用。
- 实现记录：新增服务端侧 `germ_gui_part_dos` action；先查 Germ runtime part，runtime 只暴露 `options` 时回退读取 `plugins/GermPlugin/gui/*.yml`；返回 `rawDos`、`resolvedDos`、`part`、`availableParts`；`execute=false` 只解析，`execute=true` 才执行。
- 执行模式：`command_util` 调 Germ `CommandUtil.execute(...)`，`packet` 调 `GermPacketAPI.sendGuiDos(...)`，`player_command` 只解析并执行 `playercmd<->...`。
- 验证记录：`cell-01` relay `query_player_state` 成功；慢速执行 `/gp reload`、`/lmshop open solar`、`/gp open zzzderk 分类商城正式模板` 后 `query_germ_screen open=true`。
- Lmshop 证据：`商品1金币` dry-run 解析出 `playercmd<->lmshop buy 1 vault <token>`；`command_util` / `packet` 均未触发订单；`player_command` 对 vault 按钮触发 Lmshop 但因本 cell Vault 不可用返回“支付渠道当前不可用: vault”。
- 成功路径：`商品2点券` dry-run 解析出 `playercmd<->lmshop buy 2 player_points <token>`；`execute=true, mode=player_command` 后订单为 `#4 [DELIVERY_DISPATCHED] category / solar_key / player_points 500`；后续对 `商品1点券` dry-run 后订单仍为 1，证明 dry-run 无副作用。
- 关键边界：该能力验证的是 Germ YAML `clickDos` 语义执行，不是物理鼠标点击；真实坐标点击 / 客户端 Germ 事件仍需后续单独处理。

## 2026-04-29 18:36 +08:00 - Germ hit-test 只读探针增强

- 当前阶段：`query_germ_hit_test` 已从 action 注册推进到真实 Germ loading GUI 命中验证。
- 本段重点：补 `numericHints` / `boundsCandidates` / `boundsSource` / `hitSource`，让混淆 Germ 组件能暴露候选 bounds；仍保持只读，无点击副作用。
- 已完成：新增 `query_germ_hit_test` action、1.12.2 Forge 实现、插件 API/test catalog 入口；`germ_gui_loading` 的 texture、text、gif 可见元素均可命中。
- 关键决策：不新增 `click_germ_component`，先把 Lmshop 真实页面的只读 bounds 证据补齐，再评估专用点击 hook。
- 验证记录：`common test` 通过；`forge1122_build` 通过；`plugin build` 通过；`cell-01` `/status` 为 `actions=113 ready=true`；`run_test scope=smoke` 为 `passed=22 failed=0 skipped=0 total=22 totalMs=8052`；非 Germ `GuiIngameMenu` 返回 `supported=false hitCount=0`。
- 环境注意：隐藏 1.12.2 bot 做 Germ 真页验证前要把 `pauseOnLostFocus` 临时设为 `false`；`cell-03` 本轮被 GermPlugin CDK 验证失败拦住，已换 `cell-01` 得到通过证据。
- 遗留问题：Lmshop 真实 Germ 商城页的购买按钮/商品组件 bounds 还未复测；test-cell 本轮结束仍需 stop + release。

## 2026-04-28 18:10 +08:00 - 1.12.2 测试服务端统一超平坦

- 当前阶段：`cell-01..05`、隔离 `server-cell-10`、`cell-20..22` 的 1.12.2 测试服务端已统一为超平坦世界配置。
- 本段重点：所有目标 `server.properties` 已写入 `level-type=FLAT`、`generator-settings=`，并保留 `level-name=world`，避免插件依赖默认世界名时出问题。
- 脚本真源：`Invoke-TestCell.ps1` 每次 ensure 会强制收敛 `server-port`、和平/无怪物和超平坦配置；`Provision-TestCells.ps1` 与 `Provision-TestCellMod1122.ps1` 也会在新建/重建 cell 时写入同样配置。
- 验证记录：所有目标 1.12.2 测试服 `server.properties` 均为 `level-type=FLAT`、`generator-settings=`；用 `cell-03` 做了一次启动验证，relay `query_player_state` 返回 overworld、`y=4.0`、满血且非死亡；验证后已 stop/release，目标监听端口与匹配 `cmd/java/javaw` 进程均为 0。

## 2026-04-28 11:45 +08:00 - 1.12.2 测试客户端默认加载语言包

- 当前阶段：`Minecraft-Mod-Language-Modpack.zip` 已从“只复制到 resourcepacks”升级为“默认启用”。
- 本段重点：新增 `Set-TestCellBotResourcePacks.ps1`，默认覆盖 `cell-01..05`、隔离 `cell-10`、`cell-20..22`，会复制缺失语言包并更新 `options.txt`。
- 配置真源：`baseline-1122.json` 新增 `botDefaultResourcePacks=["Minecraft-Mod-Language-Modpack.zip"]`，`Sync-TestCellBaselinePlugins.ps1` 同步基线时会确保 `resourcePacks:["file/Minecraft-Mod-Language-Modpack.zip"]`。
- 验证记录：九个 1.12.2 客户端语言包 SHA256 均为 `CF9FC259349A89D61E748234A8C58F3E3E88CC7BD80DE8FF3EE006D7475C4B9F`，`options.txt` 均已写入 `file/Minecraft-Mod-Language-Modpack.zip`；二次 dry-run 显示无待变更。

## 2026-04-27 20:46 +08:00 - Forge 1.12.2 模组专测扩展到 cell-20..22

- 当前阶段：`cell-20` 已扩展为 `cell-20..22` 三格 Forge 1.12.2 模组专测池。
- 本段重点：`cells-mod1122.json` 新增 `cell-21/22`，端口为 `25721 / 38210 / 38211` 和 `25722 / 38220 / 38221`。
- 已完成：`Provision-TestCellMod1122.ps1` 默认追加准备 `cell-21/22`；`Sync-TestCellMod1122Artifacts.ps1` 默认同步到 `cell-20..22`；`Run-TestCellMod1122Regression.ps1 -AcquireCell` 未显式传 `-CellId` 时从三格池里抢 ready cell；`Invoke-TestCell.ps1` stop 增加二次 orphan `cmd.exe` 清扫。
- 资源配置：1.12.2 普通池和模组专测池的服务端默认内存已调整为 `512M / 1536M`；`cell-03` 已用 `-Xms512M -Xmx1536M` 实测启动，插件和客户端 `/status` 均 ready，随后 stop/release 成功。
- 验证记录：`Provision-TestCellMod1122.ps1` 输出 `sourceCellId=cell-01`、`provisioned.id=cell-21,cell-22`；`cell-21/22` 均完成 acquire/release 与真实 startup 验证，服务端、客户端、连接和 relay 均成功。
- 遗留问题：本轮未同步具体业务模组 jar 做功能验证；下一次带具体 Forge 1.12.2 模组 jar 时再跑 `Run-TestCellMod1122Regression.ps1 -Scope startup -ModJar <jar> -AcquireCell`。

## 2026-04-27 00:55 +08:00 - Forge 1.12.2 模组专测 cell-20

- 当前阶段：`cell-20` 已作为独立 Forge 1.12.2 模组测试环境准备并完成 startup 验证。
- 本段重点：用户确认服务端复制 `cell-01`，客户端也复制 `cell-01`；`cell-20` 使用独立 `cells-mod1122.json`、独立端口和独立 lease。
- 已完成：新增 `cells-mod1122.json`、`Provision-TestCellMod1122.ps1`、`Sync-TestCellMod1122Artifacts.ps1`、`Run-TestCellMod1122Regression.ps1`，为 provision 的 `-Force` 删除补了路径边界校验，并更新测试系统文档和全局本地回归 skill。
- 关键决策：`cell-20` 不混入 `cell-01..05` 普通 1.12.2 插件/Germ/BC 回归池，专门用于需要服务端和客户端同时加载 Forge 1.12.2 模组的场景。
- 验证记录：PowerShell 静态解析 3 个新增脚本均 `errorCount=0`；startup 验证 `ok=true`，bot 连接 `localhost:25720`，relay `query_player_state` 成功，cleanup 后端口无监听且无相关 `cmd/java/javaw` 残留。
