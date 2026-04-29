# 当前任务

## 背景

- 用户正在把 `BlackBoxPro` 往“更方便自动化测试 Germ 插件、test-cell 池和 BC 多后端链路”的方向推进。
- 当前仓库已有原版容器点击、hover、tooltip、屏幕状态查询等能力，但这些能力主要面向 `GuiContainer` / 原版槽位体系。
- 本轮新增了 1.12.2 Forge 的通用屏幕鼠标层，并同步扩展了本机 test-cell 基础设施：
  - `cell-01..05` 都应保持 Germ 可用。
  - `cell-01..05` 都可作为 1.12.2 BC 后端。
  - BC smoke 已支持 5 后端自动验证和自动收尾。

## 当前目标

- 收口当前工作树，让下一次会话能快速判断：
  - 哪些功能已经实现并验证。
  - 哪些只是基础设施更新。
  - 哪些还需要继续做。
  - 测试环境是否干净。

## 本次已完成

- Germ / 通用屏幕鼠标 action：
  - 新增设计文档：`docs/superpowers/specs/2026-04-24-screen-mouse-actions-design.md`
  - 新增 action 真源：`move_mouse`、`click_mouse`、`click_screen_at`、`query_cursor_state`
  - 新增 1.12.2 Forge 实现：`ScreenMouseHelper`、`MoveMouseAction`、`ClickMouseAction`、`ClickScreenAtAction`、`QueryCursorStateAction`
  - 更新 `ActionCatalog`、`ActionRegistry`、插件 API 和 `BlackBoxTestCatalog`
- Germ 屏幕探针：
  - 新增设计文档：`docs/superpowers/specs/2026-04-26-germ-screen-probe-design.md`
  - 新增实现计划：`docs/superpowers/plans/2026-04-26-germ-screen-probe.md`
  - 新增 action 真源：`query_germ_screen`
  - 新增 1.12.2 Forge 实现：`GermScreenProbeHelper`、`QueryGermScreenAction`
  - 更新 `ActionCatalog`、`ActionRegistry`、插件 API 和 `BlackBoxTestCatalog`
- Germ hit-test 只读探针：
  - 新增 action 真源：`query_germ_hit_test`
  - 新增 1.12.2 Forge 实现：`QueryGermHitTestAction`
  - `GermScreenProbeHelper` 现在返回 `numericHints`、`boundsCandidates`、`boundsSource`、`hitSource`，用于定位混淆 Germ 组件坐标
  - 已支持 `germ_gui_loading` 的 texture、text、gif 可见元素命中；仍不提供 `click_germ_component` 这类副作用动作
- 键盘输入 action：
  - 新增 action 真源：`key_press`、`type_text`
  - 新增 1.12.2 Forge 实现：`ScreenKeyboardHelper`、`KeyPressAction`、`TypeTextAction`
  - `key_press` 在 GUI 打开时反射调用 `GuiScreen.keyTyped(...)`，无 GUI 时使用 Minecraft key binding
  - `type_text` 面向当前 GUI 输入文本；无 GUI 时应明确失败 `No screen open`
  - 更新插件 API：`KeyboardActions` 和 `HighLevelActions.keyPress/typeText`
  - 更新 `BlackBoxTestCatalog` 默认参数；`type_text` 默认不纳入无夹具 full 回放
- 1.12.2 test-cell 基线：
  - `cell-01` 作为 1.12.2 baseline source
  - `cell-01..05` 服务端插件基线包含 `GermPlugin`
  - `cell-01..05` bot mod 基线包含 `GermMod`
  - 新增 `Sync-TestCellBaselinePlugins.ps1` / `TestCellBaselinePlugins.ps1` 和两套 baseline JSON
- 多会话与清理：
  - 新增/调整 `Run-TestCellRegression.ps1`，完整回归默认执行 stop/release
  - `Invoke-TestCell.ps1` / `Invoke-TestCell1201.ps1` 增强和平难度、怪物生成关闭、死亡自动复活等测试前置
  - 脚本清理时注意 `cmd/java/javaw`，避免只留下提示不关进程
- BC 测试链路：
  - 新增 `Invoke-TestCellBc.ps1`、`Prepare-TestCellBcBackends.ps1`、`Run-TestCellBcSmoke.ps1`、`TestCellBcCommon.ps1`
  - 新增临时 helper 构建入口 `Build-TestCellBcBridgeHelper.ps1`
  - `Run-TestCellBcSmoke.ps1` 已支持 `-BackendCellIds` 多后端参数
  - BC 5 后端 smoke 已通过：`cell-02 -> cell-01 -> cell-03 -> cell-04 -> cell-05`
  - bot 连接 BC 必须用 `localhost:25645`，不要用 `127.0.0.1:25645`
  - BC smoke cleanup 已确认会 stop BC、stop bot、restore 后端配置、release lease、删除 helper jar 和 launcher
- 文档/技能/外部 AI：
  - `.cursor/rules/blackboxpro-local-regression.mdc` 已生成
  - `$blackboxpro-local-regression` 已补入 BC 5 后端 smoke、`localhost`、中断后清理规则
  - `$blackboxpro-manual-session` 的 Germ 手测 profile 已从 `cell-04/05` 扩到 `cell-01..05`
  - `knowledge/03-build-and-verify.md` 和 `knowledge/06-test-system.md` 已同步主要测试入口

## 已确认事实

- 2026-04-24 曾本地执行 `./gradlew.bat forge1122_build plugin_build` 通过。
- 2026-04-25 收尾阶段重新执行 `./gradlew.bat forge1122_build plugin_build` 通过；本机默认 Java 8 会失败，需临时指定 JDK 21，例如 `F:/mcplugins/.local-tools/temurin21/jdk-21.0.10+7`。
- `cell-05` 部署新 `BlackBoxPro-forge-1.12.2-2.2.4.jar` 后，bot `/status` 的 action 数量提升到 `110`，说明 `query_germ_screen` 已被运行时识别。
- 2026-04-27 部署新 `BlackBoxPro-forge-1.12.2-2.2.4.jar` 到 `cell-01` 后，bot `/status` 的 action 数量提升到 `112`，说明 `key_press` / `type_text` 已被运行时识别。
- `query_cursor_state` 已在 `cell-05` 真实返回：
  - Germ 界面下能读到 `screenClass`
  - 能返回 `mouseX/mouseY`
  - 能返回 `scaledWidth/scaledHeight`
- `move_mouse` 已验证会实际改变 `query_cursor_state.mouseX/mouseY`。
- `click_screen_at` 已在原版 `GuiInventory` 中验证生效：
  - 点击前 `query_inventory_slot(slot=0)` 为 `minecraft:stone`
  - 调用 `click_screen_at`
  - 点击后 `query_inventory_slot(slot=0)` 变为空
- `query_germ_screen` 已在 `cell-05` 真实返回：
  - 非 Germ 屏幕 `GuiIngameMenu` 下返回 `supported=false`、`probeMode=reflective-fields`、组件数 `0`、warnings `0`
  - `SkinWardrobe` 的 `/sw open` Germ 页面下返回 `screenClass=O000OOO0O0OO`、`screenClassName=com.germmc!.O000OOO0O0OO`
  - Germ 页面下返回 `supported=true`、`probeMode=screen-class`、组件数 `5`、hovered `0`、warnings `0`
- `query_germ_hit_test` 已在 `cell-01` 的真实 Germ loading GUI 返回可用命中：
  - `/gp open zzzderk germ_gui_loading` 后，`x=185,y=96` 命中 texture，bounds `182.0,92.5,16.0,16.0`
  - `x=210,y=100` 与 `x=238,y=100` 命中文本，bounds `202.0,96.5,100.0,12.0`
  - `x=246,y=100` 命中 gif，bounds `242.0,96.5,10.0,10.0`
  - `x=300,y=140` 返回 `hitCount=0`
  - 非 Germ `GuiIngameMenu` 下仍返回 `supported=false`、组件数 `0`、`hitCount=0`
- 2026-04-29 收口复测确认：
  - `cell-03` 的 Germ 真 GUI 打开被 `GermPlugin 的Cdk不是正确的，导致验证失败` 阻塞，不能作为 Germ hit-test 失败结论。
  - 隐藏 1.12.2 bot 若 `pauseOnLostFocus:true`，会反复回到 `GuiIngameMenu`；Germ 真页验证前应临时设为 `pauseOnLostFocus:false` 并重启 cell。
  - 换到 `cell-01` 同步当前分支 `BlackBoxPro-Plugin-2.2.4.jar` 与 `BlackBoxPro-forge-1.12.2-2.2.4.jar` 后，relay `query_player_state` 成功，`/status` 返回 `actions=113 ready=true`。
  - `run_test scope=smoke` 通过：`passed=22 failed=0 skipped=0 total=22 totalMs=8052`。
  - `query_germ_hit_test` 精简复测：texture/text/gif 均 `containsHit=true` 且返回 `boundsSource` / `hitSource`，outside 点 `hitCount=0`。
- 2026-04-25 已确认 `cell-01..05` 都有：
  - 服务端：`GermPlugin-Snapshot-4.4.2-11.jar`
  - Bot：`GermMod-Snapshot-4.4.2-11.jar`
- 2026-04-25 已执行 `Sync-TestCellBaselinePlugins.ps1 -DryRun`，同步后无待复制/更新差异。
- 2026-04-25 已执行 BC 5 后端 smoke：
  - 命令：`Run-TestCellBcSmoke.ps1 -BotCellId cell-02 -DefaultBackendId cell-02 -BackendCellIds cell-02,cell-01,cell-03,cell-04,cell-05`
  - 结果：`ok=true`
  - `bcConnect=localhost:25645`
  - cleanup 后监听端口为 `0`，匹配测试进程为 `0`，临时 helper/launcher 为 `0`

## 待验证点

- 在真实 Germ GUI（本轮用 `SkinWardrobe` 的 `/sw open` 验证）里，`click_screen_at` 目前尚未观察到稳定 UI 响应。
- 这说明方案 A 已经把“通用屏幕鼠标层”做通，但 Germ 页面是否真正消费 `GuiScreen.mouseClicked(...)` 还不能下结论。
- 方案 B 的只读探针已推进到 `query_germ_hit_test`：能返回真实 Germ loading 页面可见元素的候选 bounds 和 hit 结果；下一步才适合基于这些 bounds 评估专用 Germ 点击动作。
- 现代端 `1.21.11` / `1.21.1` 还没有同步新增的屏幕鼠标 action。
- BC 多后端链路目前依赖临时 `TestCellBcBridgeHelper.jar` 的 `/bbswitch <server>`，不建议现在产品化为正式长期插件；除非后续手测也需要长期跨服切换命令。
- 当前分支的 Germ 屏幕探针实现已拆成多次提交；收尾阶段只应提交本 handoff 更新。

## 当前结论

- 方案 A 值得保留，第一版以 `1.12.2 Forge` 为边界是正确的。
- 当前版本已经把 `BlackBoxPro` 从“只会点原版容器槽位”推进到“可以对任意屏幕做坐标级鼠标移动与点击”。
- 当前版本已补齐 1.12.2 Forge 的基础键盘输入层，可用于打开聊天/背包、关闭 GUI、向当前 GUI 输入文本。
- 对 Germ 自动化而言，方案 A 是必要基础层，但还不等于“Germ 所有界面立刻可点”；如果真实 Germ 页面不吃普通点击，后续需要方案 B 的专用 probe / hook。
- 方案 B 的只读 probe 已落地到 hit-test 层：它能区分非 Germ 屏幕和真实 Germ screen，并能返回可用于后续组件定位的候选 bounds 与命中结果。
- test-cell 基础设施已经从单 cell 回归推进到：
  - `cell-01..05` 并发池
  - 1.12.2 / 1.20.1 分离
  - 1.12.2 Germ 公共基线
  - BC 5 后端 smoke
  - 中断后可恢复/可清理

## 下一步

1. 基于 `query_germ_hit_test` 在 Lmshop 的真实 Germ 商城页上找购买按钮/商品组件 bounds，先补只读证据。
2. 如 bounds 稳定，再单独设计显式 `click_germ_component` 或专用 hook；不要把点击副作用塞进 query action。
3. 继续用真实 Germ 页面做坐标夹具验证，确认 `click_screen_at` 不稳定是坐标/层级问题，还是 Germ 需要专用 hook。
4. 现代端 `1.21.11` / `1.21.1` 如需同等能力，再单独规划 action 迁移。
5. 如果要长期手测 BC 跨服，再评估是否把临时 `TestCellBcBridgeHelper` 产品化；当前自动化 smoke 不需要正式 BC 插件。

## 验证记录

- 2026-04-24 构建：
  - `./gradlew.bat forge1122_build plugin_build`
- 2026-04-24 真实回归：
  - `cell-05` ensure 成功
  - `query_cursor_state` 成功
  - `move_mouse` 成功
  - `click_screen_at` 在原版 `GuiInventory` 成功改变槽位状态
  - `click_screen_at` 在 Germ `SkinWardrobe` 页面尚未观察到稳定 UI 变化
- 2026-04-25 基线同步：
  - `Sync-TestCellBaselinePlugins.ps1`
  - `Sync-TestCellBaselinePlugins.ps1 -DryRun`
- 2026-04-25 BC 5 后端：
  - `Run-TestCellBcSmoke.ps1 -BotCellId cell-02 -DefaultBackendId cell-02 -BackendCellIds cell-02,cell-01,cell-03,cell-04,cell-05`
  - 结果 `ok=true`
- 2026-04-25 fresh 构建：
  - 默认 shell Java 8 下失败，错误为 Gradle 需要 JVM 17+
  - 临时指定 `JAVA_HOME=F:/mcplugins/.local-tools/temurin21/jdk-21.0.10+7` 后执行 `./gradlew.bat forge1122_build plugin_build`
  - 结果通过，仅有 Java 编译 deprecation / unchecked 警告
- 2026-04-25 收尾状态：
  - test-cell 监听端口：`0`
  - 匹配 `cmd/java/javaw` 测试进程：`0`
  - 临时 `TestCellBcBridgeHelper.jar` / `start-bc-backend-*.cmd`：`0`
- 2026-04-27 Germ screen probe 构建：
  - `Gradle 9.4.0 -p common --no-daemon jar`
  - `Gradle 8.9 -p mod/1.12.2 -g .gradle-user-home/forge1122 --no-daemon --console=plain clean build`
  - `Gradle 8.14.3 -p plugin --no-daemon build`
  - 结果通过，仅有既有 deprecation / unchecked 警告
- 2026-04-27 Germ screen probe 真实验证：
  - `cell-05` ensure 成功，bot 连接 `localhost:25605`
  - `http://127.0.0.1:38121/status` 返回 `actions=110`、`ready=true`
  - 非 Germ 屏幕执行 `query_germ_screen` 成功，`supported=false`、组件数 `0`、warnings `0`
  - `/sw open` 后执行 `query_cursor_state` 成功，`screenClass=O000OOO0O0OO`
  - `/sw open` 后执行 `query_germ_screen` 成功，`supported=true`、`probeMode=screen-class`、组件数 `5`、warnings `0`
- 2026-04-27 Germ screen probe 收尾状态：
  - `Invoke-TestCell.ps1 -Mode stop -CellId cell-05` 成功，停止 server cmd `56128`、server java `33524`、bot javaw `12112`
  - `Release-TestCell.ps1 -CellId cell-05 -Owner germ-screen-probe-20260427` 成功
  - `cell-05` lease 已释放
  - `25605/38120/38121` 监听端口：`0`
  - 匹配 `server-cell-05` / `BlackBoxProTestCells/cell-05` 的 `cmd/java/javaw` 进程：`0`
- 2026-04-27 keyboard actions 构建与真实验证：
  - `gradle.bat -p common --no-daemon --console=plain test` 通过；新增 catalog 测试先红后绿
  - `mod/1.12.2/gradlew.bat -g ../../.gradle-user-home/forge1122 --no-daemon --console=plain clean build` 通过
  - `Gradle 8.14.3 -p plugin --no-daemon --console=plain build` 通过
  - `cell-01` ensure 成功，bot `/status` 返回 `actions=112`、`ready=true`
  - `type_text` 在无屏幕时返回失败 `No screen open`
  - `key_press E` 后 `query_cursor_state` 返回 `GuiInventory`
  - `key_press ESCAPE` 后 `query_cursor_state.open=false`
  - `key_press T` 打开 `GuiChat`，`type_text` 输入唯一文本，`key_press RETURN` 发送后 `query_chat_history` 查到该文本
  - `Invoke-TestCell.ps1 -Mode stop -CellId cell-01` 成功，停止 server cmd `35432`、server java `41440`、bot javaw `36652`
  - `Release-TestCell.ps1 -CellId cell-01 -Owner keyboard-actions-20260427110619` 成功，`cell-01` lease 已释放
  - `25565/38080/38081` 监听端口：`0`
  - 匹配 `server-cell-01` / `BlackBoxProTestCells/cell-01` 的 `cmd/java/javaw` 进程：`0`
- 2026-04-29 Germ hit-test 构建与真实验证：
  - `./gradlew.bat -p common --no-daemon --console=plain test --rerun-tasks "-Pkotlin.incremental=false"` 通过
  - `./gradlew.bat forge1122_build --no-daemon --console=plain` 通过
  - `F:/mcplugins/.local-tools/gradle/gradle-8.14.3/bin/gradle.bat -p plugin --no-daemon --console=plain build` 通过
  - `cell-01` 部署新 `BlackBoxPro-forge-1.12.2-2.2.4.jar` 后，`http://127.0.0.1:38081/status` 返回 `actions=113`、`ready=true`
  - `/gp open zzzderk germ_gui_loading` 后，`query_germ_hit_test` 对 texture、text、gif 可见点返回 `hitCount>=1` 和可用 bounds；非 Germ `GuiIngameMenu` 返回 `supported=false`、`hitCount=0`
  - 收口复测中 `run_test scope=smoke` 返回 `passed=22 failed=0 skipped=0 total=22 totalMs=8052`
