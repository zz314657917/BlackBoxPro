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
- 方案 B 的第一步 `query_germ_screen` 已完成并通过真实 Germ 页面验证；后续应继续增强组件坐标、层级语义、hover/命中字段，而不是重新做探针入口。
- 现代端 `1.21.11` / `1.21.1` 还没有同步新增的屏幕鼠标 action。
- BC 多后端链路目前依赖临时 `TestCellBcBridgeHelper.jar` 的 `/bbswitch <server>`，不建议现在产品化为正式长期插件；除非后续手测也需要长期跨服切换命令。
- 当前分支的 Germ 屏幕探针实现已拆成多次提交；收尾阶段只应提交本 handoff 更新。

## 当前结论

- 方案 A 值得保留，第一版以 `1.12.2 Forge` 为边界是正确的。
- 当前版本已经把 `BlackBoxPro` 从“只会点原版容器槽位”推进到“可以对任意屏幕做坐标级鼠标移动与点击”。
- 对 Germ 自动化而言，方案 A 是必要基础层，但还不等于“Germ 所有界面立刻可点”；如果真实 Germ 页面不吃普通点击，后续需要方案 B 的专用 probe / hook。
- 方案 B 的首个只读 probe 已落地：它能区分非 Germ 屏幕和真实 Germ screen，并能返回可用于后续组件定位的初始组件树。
- test-cell 基础设施已经从单 cell 回归推进到：
  - `cell-01..05` 并发池
  - 1.12.2 / 1.20.1 分离
  - 1.12.2 Germ 公共基线
  - BC 5 后端 smoke
  - 中断后可恢复/可清理

## 下一步

1. 提交本 handoff 更新。
2. 后续继续增强 `query_germ_screen` 的组件坐标、可读名称、层级语义和 hover/命中字段，形成可稳定点击 Germ GUI 的夹具基础。
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
