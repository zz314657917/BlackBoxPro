# 当前任务

## 背景

- 用户正在把 `BlackBoxPro` 往更稳定的 Minecraft 自动化测试基础设施推进。
- 当前重点集中在 1.12.2 Forge：Germ GUI 自动化、屏幕级鼠标/键盘输入、BC 多后端 smoke，以及 Forge 1.12.2 模组专测池。
- 当前仓库真实通讯主链是 HTTP relay；旧 Plugin Message Channel 描述只作为历史背景。

## 当前目标

- 使用 `docs/workflow/status.md` 作为 P/G/E 状态入口，把 BlackBoxPro 稳定主线拆成可审核、可交给 worker 的 Sprint contract。
- 把已经验证过的 test-cell / Germ / BC 能力收口到主线。
- 收口本轮 Lmshop Germ 验收辅助能力：`germ_gui_part_dos` 已能从 Germ YAML 解析 `clickDos`，并用显式 `player_command` 模式复现玩家命令按钮语义。
- 保持 `knowledge/` 作为下一次会话的接手入口。
- 清理已合并分支和旧 worktree，避免后续继续从过期分支开始。

## 已完成

- Germ / 通用屏幕鼠标 action：
  - 新增 `move_mouse`、`click_mouse`、`click_screen_at`、`query_cursor_state`。
  - 1.12.2 Forge 实现已落地，`click_screen_at` 在原版 `GuiInventory` 验证生效。
- Germ 只读探针：
  - 新增 `query_germ_screen` 和 `query_germ_hit_test`。
  - `query_germ_hit_test` 已能在真实 `germ_gui_loading` 页面返回 texture、text、gif 的候选 bounds 和 hit 结果。
- Germ 显式动作：
  - `click_germ_component` 已加入客户端侧尝试，但在 Lmshop 真页上仍未触发购买。
  - `germ_gui_part_dos` 已加入服务端侧执行入口，支持 `execute=false` dry-run、YAML fallback、PAPI placeholder 解析和 `player_command` 模式。
  - 2026-05-01 `cell-01` 验证：`商品2点券` 的 `clickDos` 生成 `#4 [DELIVERY_DISPATCHED] category / solar_key / player_points 500`；`商品1点券` dry-run 后订单数仍为 1。
- 键盘输入 action：
  - 新增 `key_press`、`type_text`。
  - 1.12.2 Forge 已验证打开背包、打开聊天、输入文本、回车发送等基础链路。
- Forge 1.20.1 输入 action：
  - 本轮按用户最新优先级先支持 `1.20.1` 和 `1.12.2`，不继续扩 `1.21.x`。
  - `1.20.1` 已新增 `move_mouse`、`click_mouse`、`click_screen_at`、`query_cursor_state`、`key_press`、`type_text` 实现与 registry 注册。
  - `1.12.2` 已有同名 action，当前作为保持不回退的语义基线。
- 1.12.2 普通 test-cell 池：
  - `cell-01..05` 统一作为 Germ + BC 后端回归池。
  - 服务端公共基线包含 `BlackBoxPro-Plugin`、`PlayerCurrency`、`PlayerPoints`、`LuckPerms`、`GermPlugin`、`Vault`、`PlaceholderAPI`、`ProtocolLib`。
  - bot 基线包含 `GermMod`、`辅助-jei.jar` 和默认启用的 `Minecraft-Mod-Language-Modpack.zip`。
  - 1.12.2 服务端默认超平坦，且端口避开 `25565` / `25566`。
- BC smoke：
  - `Run-TestCellBcSmoke.ps1` 支持多后端链路和业务 hook。
  - 已验证 `cell-02 -> cell-01 -> cell-03 -> cell-04 -> cell-05`。
  - bot 连接 BC 必须用 `localhost:25645`，不要用 `127.0.0.1:25645`。
- Forge 1.12.2 模组专测池：
  - 新增 `scripts/test-cells/cells-mod1122.json`，当前包含 `cell-20..22`。
  - 新增 `Provision-TestCellMod1122.ps1`、`Sync-TestCellMod1122Artifacts.ps1`、`Run-TestCellMod1122Regression.ps1`。
  - `cell-20..22` 使用独立目录、独立端口和独立 lease；服务端和客户端模板都来自 `cell-01`。
  - `Run-TestCellMod1122Regression.ps1 -AcquireCell` 未显式传 `-CellId` 时会从三格池里抢 ready cell，默认结束后 stop + release。
  - 2026-05-03 Sprint 2 已用 `CloudStorage` 固定 jar 在 `cell-20` 跑通 `startup` 与 `smoke`，两次都 `ok=true` 并自动 stop/release。
- 构建入口：
  - 根 `build.gradle.kts` 已补回 `mod1211_build` / `mod1211_pack_neoforge`，`buildAll` 覆盖 `1.21.1`、`1.20.1`、`1.12.2`、plugin 和 common。

## 已确认事实

- `cell-20..22` 配置：
  - `cell-20`：`25720 / 38200 / 38201`
  - `cell-21`：`25721 / 38210 / 38211`
  - `cell-22`：`25722 / 38220 / 38221`
  - 服务端 Java：`C:/Program Files/Java/jdk1.8.0_481/bin/java.exe`
  - 客户端 Java：`C:/Program Files/Java/jdk1.8.0_481/bin/javaw.exe`
- `cell-21/22` 已完成 startup 验证：服务端 `/status`、客户端 `/status`、`connect_to_server` 和 relay `query_player_state` 均成功，并已自动 stop/release。
- 2026-04-29 Germ hit-test 收口复测中，`run_test scope=smoke` 返回 `passed=22 failed=0 skipped=0 total=22 totalMs=8052`。
- 隐藏 1.12.2 bot 做 Germ 真页验证前，需要确认 `pauseOnLostFocus:false`；否则客户端可能自动回到 `GuiIngameMenu`。

## 待验证点

- Sprint 2 已证明 `cell-20` 可跑具体业务 Forge 1.12.2 模组 jar 的 `startup` 与 `smoke`；尚未做 `cell-20/21/22` 全矩阵。
- Lmshop 真实 Germ 商城页已复测：页面可打开，`germ_gui_part_dos` 可按 YAML `partId` 解析并执行 `clickDos`；真实物理坐标点击仍未通过。
- Sprint 3 已补齐 `1.20.1` / `1.12.2` 的真实 `/status` + GUI/input runtime smoke，当前可声明 priority-version runtime PASS。
- `1.21.11` / `1.21.1` 输入 action 同步已从 Sprint 3 延后到后续 sprint。

## 下一步

1. Sprint 3 已完成并保持 `done`：`docs/workflow/qa/sprint-003-qa.md` 首行为 `### PASS: bbp-sprint-003-priority-input-support`。
2. 下一轮若继续输入 action 覆盖，单独开新 sprint 做 `1.21.11` / `1.21.1`，不要把本轮 `1.20.1` / `1.12.2` runtime PASS 外推到 `1.21.x`。
3. 后续如果继续使用 DeepSeek worker，只让它执行测试和写 report；Sprint 2 两次 worker 均因预算耗尽未写出合格 report，最终裁决仍由 Codex/Evaluator 给出。
4. 如果 Lmshop Sprint 2 接受“Germ YAML clickDos 语义执行”作为自动化验收证据，使用 `germ_gui_part_dos execute=true mode=player_command`；如果仍要求“真实物理点击”，继续增强客户端侧 `click_germ_component` / 鼠标注入。
5. 多 bot 编排仍是独立后续计划，不能表述成已实现能力。

## 验证记录

- 2026-05-03 已新增 P/G/E workflow 骨架：`docs/workflow/status.md`、`docs/workflow/spec.md`、`docs/workflow/tasks/sprint-001.md`、`docs/workflow/main-log.md`；随后补齐 `Task ID` 字段，新增 contract review 与 QA 报告，当前 phase 为 `done`。
- 2026-05-03 已新增并审核通过 Sprint 2 contract：`docs/workflow/tasks/sprint-002.md`；目标为 CloudStorage jar 在 `cell-20..22` 上完成 `startup` 和 `smoke` 运行态证据，随后已通过验收并把 phase 推进到 `done`。
- 2026-05-03 Sprint 2 DeepSeek worker 两次尝试均因 `error_max_budget_usd` 停止，未产生合格 worker report；随后 Codex/Evaluator 直接执行 Sprint 2 acceptance commands。
- 2026-05-03 Sprint 2 验收通过：CloudStorage jar 存在；`startup` 抢到 `cell-20`，`ok=true`，stop/release 成功；`smoke` 抢到 `cell-20`，`ok=true`，`run_test smoke passed=22 failed=0 skipped=0 total=22 totalMs=8081`，截图 `G:/MC/game/BlackBoxProTestCells/cell-20/.minecraft/versions/bot/screenshots/blackboxpro/zzzderk/integration_smoke_1777814618/001_catalog_screenshot.png` 存在且为 `854x480 / 31762 bytes`，stop/release 成功；端口 `25720/38200/38201` 无监听，相关 test-cell 进程为 0。
- 2026-05-03 已新增并审核通过 Sprint 3 contract：`docs/workflow/tasks/sprint-003.md`；目标为同步现代端 6 个输入 action，并要求 `common_build plugin_build mod2111_build mod1211_build`。现代 runtime PASS 必须来自真实 `1.21.x` `/status` 和 smoke；没有真实端点时必须报 `BLOCKED`，不能用 `1.20.1` 或 `1.12.2` 代替。
- 2026-05-04 用户把 Sprint 3 优先级改为 `1.20.1` 和 `1.12.2`；旧 `1.21.x` 同步目标延后。本轮新增 `PriorityInputActionSupportTest`，为 `1.20.1` 新增六个输入 action/helper 文件并注册，`1.12.2` 保持原有实现不改。
- 2026-05-04 Sprint 3 build/static QA 通过：`rg` 静态搜索命中目标 action；`JAVA_HOME=../.local-tools/temurin21/jdk-21.0.10+7` 下 `.\gradlew.bat -p common test --no-daemon` 通过；`.\gradlew.bat common_build plugin_build forge1201_build forge1122_build --no-daemon` 退出码 0；`git diff --check` 无 whitespace error。运行态 `/status` + smoke 未执行，不能声明 runtime PASS。
- 2026-05-04 Sprint 3 runtime QA 通过：`cell-01` Forge 1.12.2 `/status` 为 `actions=114 ready=true`，relay `query_player_state` 成功，`GuiInventory` 上 `query_cursor_state/move_mouse/click_screen_at/key_press` 成功，`GuiChat` 上 `type_text` 16 字符和 Enter 成功，随后 stop/release 并确认 `25570/38080/38081` 无监听、无残留进程。
- 2026-05-04 Sprint 3 runtime QA 通过：`cell-06` Forge 1.20.1 同步新 jar 后 `/status` 为 `actions=122 ready=true`，relay `query_player_state` 成功，`InventoryScreen` 上 `query_cursor_state/move_mouse/click_screen_at/key_press` 成功，`ChatScreen` 上 `type_text` 16 字符和 Enter 成功，随后 stop/release 并确认 `25615/38130/38131` 无监听、无残留进程。
- 2026-05-04 1.20.1 runtime QA 过程中修复两点：`ScreenMouseHelper` 改为直接读取 Mojang `Window` API，避免 `Failed to resolve window metrics`；`forge1201_build` 追加 `finalizedBy(collectJars)`，避免 test-cell 同步到旧 root `build/libs` jar。
- 2026-04-24 到 2026-04-29 已多次执行 `common test`、`forge1122_build`、`plugin build`、test-cell startup、BC smoke、Germ hit-test 和 smoke 回归；详细历史见 `knowledge/tasks/timeline.md`。
- 2026-05-01 已执行 `common_build plugin_build`，并在 `cell-01` 通过 relay、Germ 页面打开、`germ_gui_part_dos` dry-run、`player_command` 成功购买和 dry-run 无副作用验证。
- 本轮分支整理前已执行 `git diff --check`，仅有 Windows line-ending 提示，无 whitespace error。
- 2026-05-01 最终收口复核已执行 `common_build plugin_build forge1122_build`，退出码 0；`cell-01` plugin/mod `/status` ready，relay `query_player_state` 成功，`germ_gui_part_dos execute=false` 可解析 `商品2点券`，随后已 stop + release，端口 `25570/38080/38081` 无监听。
