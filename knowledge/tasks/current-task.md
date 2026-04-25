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
- `cell-05` 部署新 `BlackBoxPro-forge-1.12.2-2.2.4.jar` 后，bot `/status` 的 action 数量从旧值提升到 `109`，说明新 action 已被运行时识别。
- `query_cursor_state` 已在 `cell-05` 真实返回：
  - Germ 界面下能读到 `screenClass`
  - 能返回 `mouseX/mouseY`
  - 能返回 `scaledWidth/scaledHeight`
- `move_mouse` 已验证会实际改变 `query_cursor_state.mouseX/mouseY`。
- `click_screen_at` 已在原版 `GuiInventory` 中验证生效：
  - 点击前 `query_inventory_slot(slot=0)` 为 `minecraft:stone`
  - 调用 `click_screen_at`
  - 点击后 `query_inventory_slot(slot=0)` 变为空
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
- 现代端 `1.21.11` / `1.21.1` 还没有同步新增的屏幕鼠标 action。
- BC 多后端链路目前依赖临时 `TestCellBcBridgeHelper.jar` 的 `/bbswitch <server>`，不建议现在产品化为正式长期插件；除非后续手测也需要长期跨服切换命令。
- 当前工作树还有大量未提交文件，下一步提交前需要按主题拆分或至少明确一次性提交边界。

## 当前结论

- 方案 A 值得保留，第一版以 `1.12.2 Forge` 为边界是正确的。
- 当前版本已经把 `BlackBoxPro` 从“只会点原版容器槽位”推进到“可以对任意屏幕做坐标级鼠标移动与点击”。
- 对 Germ 自动化而言，方案 A 是必要基础层，但还不等于“Germ 所有界面立刻可点”；如果真实 Germ 页面不吃普通点击，后续需要方案 B 的专用 probe / hook。
- test-cell 基础设施已经从单 cell 回归推进到：
  - `cell-01..05` 并发池
  - 1.12.2 / 1.20.1 分离
  - 1.12.2 Germ 公共基线
  - BC 5 后端 smoke
  - 中断后可恢复/可清理

## 下一步

1. 提交前复核当前工作树，建议按以下主题拆分：
   - 屏幕鼠标 action
   - test-cell 基线与清理脚本
   - BC 准备/5 后端 smoke
   - 文档、技能和 Cursor rule
2. 继续用真实 Germ 页面做坐标夹具验证，确认是否只是坐标不准。
3. 如果确认 Germ 页面不消费 `GuiScreen.mouseClicked(...)`，再进入方案 B：
   - 做可选 `GermScreenProbe`
   - 读取组件树 / hover / 命中测试
4. 如果要长期手测 BC 跨服，再评估是否把临时 `TestCellBcBridgeHelper` 产品化；当前自动化 smoke 不需要正式 BC 插件。

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
