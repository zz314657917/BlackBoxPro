# BlackBoxPro 测试系统

## 当前环境编排

### test-cell 池

- 当前 1.12.2 本地回归统一走 `cell-01..05`。
- `cell-01..05` 都应保持 Germ 可用：服务端公共基线包含 `GermPlugin`，bot 端公共 mod 基线包含 `GermMod`；不要再把 Germ 测试限定到 `cell-04/05`。
- `cell-01..05` 的 1.12.2 bot 客户端基线还包含 JEI 核心 `辅助-jei.jar` 和资源包 `Minecraft-Mod-Language-Modpack.zip`；语言包不仅要复制到 `resourcepacks/`，还要写入 `options.txt` 的 `resourcePacks:["file/Minecraft-Mod-Language-Modpack.zip"]` 默认启用。
- 所有 1.12.2 测试服务端统一使用超平坦世界：`level-type=FLAT`、`generator-settings=`；已有旧 `world/` 必须先归档或删除，配置才会在下次启动时生效。
- 当前 Forge 1.20.1 本地回归走独立的 `cell-06..08`，配置文件固定为 `scripts/test-cells/cells-1201.json`，不混入原 `cells.json`。
- 当前 Forge 1.12.2 模组专测走独立的 `cell-20..22`，配置文件固定为 `scripts/test-cells/cells-mod1122.json`，由 `cell-01` 复制服务端和客户端模板。
- 服务端目录命名约定：
  - `cell-01..05` -> `server-cell-01..05`
  - `cell-06..08` -> `server-cell-1201-06..08`
  - `cell-20..22` -> `server-cell-mod1122-20..22`
- 客户端目录命名约定：
  - `cell-01..05` -> `cell-xx/.minecraft/versions/bot`
  - `cell-06..08` -> `cell-xx/.minecraft/versions/1.20.1-Forge_47.3.0`
  - `cell-20..22` -> `cell-20..22/.minecraft/versions/bot`
- 仓库内的 `cells.json` / `cells-1201.json` 只提交脱敏样例，真实根路径应由本地操作者自行填写。

### 客户端共享资源

- 每个 cell 自己持有 `versions/bot`。
- `1.20.1` 路线每个 cell 自己持有独立的 `versions/1.20.1-Forge_47.3.0`。
- 但 `assets` 与 `libraries` 当前通过 junction 共享：
  - `<shared_minecraft_root>/.minecraft/assets`
  - `<shared_minecraft_root>/.minecraft/libraries`
- 如果客户端启动异常，先检查 junction 是否还指向本地共享资源目录。
- `1.20.1` 路线已改成“精简 mod 客户端”：
  - `mods/` 只保留 `BlackBoxPro-forge-1.20.1-*.jar`
  - `journeymap`、`patchouli_books`、`ldlib`、`local`、`tlm_custom_pack` 等整合包侧车目录会被清掉
  - 保留版本壳、自带 `config/defaultconfigs/resourcepacks/logs/screenshots/saves/PCL` 等最小运行内容
- `1.20.1` 池当前默认内存：
  - 服务端 `1G / 1G`
  - 客户端 `1G / 1G`
- `1.12.2` 普通池和模组专测池当前默认服务端内存：
  - 服务端 `512M / 1536M`
  - 客户端 `512M / 1024M`

### 端口约定

受管测试服务端端口必须全局唯一，且不使用 `25565` / `25566`。

- `cell-01`：`25570 / 38080 / 38081`
- `cell-02`：`25575 / 38090 / 38091`
- `cell-03`：`25585 / 38100 / 38101`
- `cell-04`：`25595 / 38110 / 38111`
- `cell-05`：`25605 / 38120 / 38121`
- `cell-06`：`25615 / 38130 / 38131`
- `cell-07`：`25625 / 38140 / 38141`
- `cell-08`：`25635 / 38150 / 38151`
- `cell-20`：`25720 / 38200 / 38201`
- `cell-21`：`25721 / 38210 / 38211`
- `cell-22`：`25722 / 38220 / 38221`

### 与独立主测试服的边界

- 独立主测试服目录现在不属于当前测试系统的 active flow。
- `scripts/test-cells/` 下的状态、抢占、启动、停止、插件精简脚本，都应默认只面向 `server-cell-*`。
- `Minimize-TestCellServerPlugins.ps1` 已加保护：如果目标目录叶子名不匹配 `server-cell-*`，脚本会直接拒绝。
- `1.20.1` 池使用单独脚本：
  - `Provision-TestCells1201.ps1`
  - `Invoke-TestCell1201.ps1`
  - `Sync-TestCell1201Artifacts.ps1`
  - `Stop-AllTestCells1201.ps1`
- `1.12.2` 模组专测使用单独配置和辅助脚本：
  - `cells-mod1122.json`
  - `Provision-TestCellMod1122.ps1`
  - `Sync-TestCellMod1122Artifacts.ps1`
  - `Run-TestCellMod1122Regression.ps1`
  - `Set-TestCellBotResourcePacks.ps1`
  - 启停仍复用 `Invoke-TestCell.ps1 -ConfigPath scripts/test-cells/cells-mod1122.json`
- `Sync-TestCellBaselinePlugins.ps1` 会从 `cell-01` 同步 1.12.2 服务端插件、bot `mods/` 和 bot `resourcepacks/` 基线到 `cell-02..05`，并按 `baseline-1122.json` 的 `botDefaultResourcePacks` 写入默认加载项；1.12.2 服务端公共基线包含 `BlackBoxPro-Plugin`、`PlayerCurrency`、`PlayerPoints`、`LuckPerms`、`GermPlugin`、`Vault`、`PlaceholderAPI`、`ProtocolLib`。
- `Sync-TestCellBaselinePlugins1201.ps1` 会从 `cell-06` 同步 1.20.1 服务端插件基线到 `cell-07..08`；1.20.1 服务端公共基线包含 `BlackBoxPro-Plugin`、`PlayerCurrency`、`LuckPerms`。
- `Set-TestCellBotResourcePacks.ps1` 默认覆盖当前 1.12.2 受管客户端：`cell-01..05`、隔离 `cell-10`、`cell-20..22`；它会复制缺失的 `Minecraft-Mod-Language-Modpack.zip` 并更新 `options.txt`。
- `Get-TestCellStatus.ps1`、`Acquire-TestCell.ps1`、`Release-TestCell.ps1` 继续复用，但独立池必须显式传对应配置：
  - Forge 1.20.1：`-ConfigPath "scripts/test-cells/cells-1201.json"`
  - Forge 1.12.2 模组专测：`-ConfigPath "scripts/test-cells/cells-mod1122.json"`

## 测试入口总览

### 插件命令

`plugin/src/main/kotlin/com/blackboxpro/plugin/command/BlackBoxCommand.kt`

- `/blackbox test <player>`
  调用 `BlackBoxTestRunner.runAll(...)`
- `/blackbox test <player> full`
  调用 `BlackBoxTestRunner.runFull(...)`
- `/blackbox test <player> category <name>`
  调用 `BlackBoxTestRunner.runCategory(...)`
- `/blackbox test <player> action <id>`
  调用 `BlackBoxTestRunner.runAction(...)`

### 插件 HTTP

插件 `POST /execute` 支持特殊 action：

- `run_test`
  - `scope=smoke` -> `runSmoke(...)`
  - 其他值 -> `runFull(...)`

### Germ 屏幕探针与 hit-test

- `query_germ_screen` 是只读组件树探针，适合 GUI/PAPI/测试脚本读取当前 Germ screen 状态，不触发点击。
- `query_germ_hit_test` 是只读命中探针，参数为 `x`、`y`、`maxDepth`、`maxComponents`、`includeFields`。
- `includeFields=true` 时会返回 `numericHints`、`boundsCandidates`、`boundsSource`、`hitSource`，用于分析 Germ 混淆字段中的坐标、尺寸和命中来源。
- 隐藏 1.12.2 bot 做真实 Germ 页验证时，先确认 `versions/bot/options.txt` 中 `pauseOnLostFocus:false`，否则客户端可能自动回到 `GuiIngameMenu`，导致 `/gp open ...` 后看起来没有打开 Germ GUI。
- 2026-04-29 已在 `cell-01` 的 `germ_gui_loading` 验证：
  - texture 点 `185,96` 命中 bounds `182.0,92.5,16.0,16.0`
  - text 点 `210,100` / `238,100` 命中 bounds `202.0,96.5,100.0,12.0`
  - gif 点 `246,100` 命中 bounds `242.0,96.5,10.0,10.0`
  - outside 点 `300,140` 返回 `hitCount=0`
  - 非 Germ `GuiIngameMenu` 返回 `supported=false`、组件数 `0`、`hitCount=0`
  - 同轮 `run_test scope=smoke` 返回 `passed=22 failed=0 skipped=0 total=22 totalMs=8052`
- 若某个 cell 的 `/gp open ...` 返回聊天提示 `GermPlugin 的Cdk不是正确的，导致验证失败`，把它记录为该 cell 的 Germ 环境 blocker，换空闲 cell 复测，不要归因到 hit-test action。
- 显式副作用动作：
  - `click_germ_component` 是客户端侧组件点击尝试，必须显式调用，不进入默认 `run_test`。
  - `germ_gui_part_dos` 是服务端侧 Germ dos 执行入口，参数为 `guiName`、`partId`、`dosType`、`execute`、`resolvePlaceholders`、`mode`。
  - `execute=false` 只解析 raw/resolved dos 和 part 来源，不触发命令；`execute=true` 才允许副作用。
  - 当 Germ runtime part 树只暴露 `options` 时，`germ_gui_part_dos` 会回退读取 `plugins/GermPlugin/gui/*.yml`，按 GUI 根名和 partId 查找 `clickDos` 等配置。
  - `mode=player_command` 只执行 `playercmd<->...`，用于复现 Germ YAML 中玩家命令按钮语义；不要把它写成真实鼠标点击通过。
- 2026-05-01 在 Lmshop `cell-01` 上验证：`商品2点券` dry-run 解析为 `playercmd<->lmshop buy 2 player_points <token>`；`execute=true, mode=player_command` 后订单为 `#4 [DELIVERY_DISPATCHED] category / solar_key / player_points 500`；随后对 `商品1点券` dry-run 后订单仍为 1，确认 dry-run 无副作用。

### 1.12.2 BC 手测和 smoke

- BC 代理服入口：
  - `scripts/test-cells/Invoke-TestCellBc.ps1`
  - `scripts/test-cells/cells-bc.json`
- BC 后端准备入口：
  - `scripts/test-cells/Prepare-TestCellBcBackends.ps1`
- 真实联通 smoke：
  - `scripts/test-cells/Run-TestCellBcSmoke.ps1`
- 默认目标：
  - bot cell：`cell-02`
  - backend cells：`cell-02,cell-03`
  - BC listen：`127.0.0.1:25645`
- `Prepare-TestCellBcBackends.ps1 -Mode prepare` 会：
  - 校验目标 cell 可用并写入同一 owner 的 lease
  - 保存原始 `spigot.yml` 与 `plugins/BlackBoxPro/config.yml` 到 `locks/bc-backend-prep/<owner>.json`
  - 临时切 `bungeecord: true`
  - 保持各 backend 自己的 `http-port`
  - 把所有 backend 的 `mod-http-address` 指向 bot cell 的共享 mod 端口
- `Prepare-TestCellBcBackends.ps1 -Mode restore` 会：
  - 停止目标后端进程
  - 恢复原文
  - 删除 state
  - 释放本次 owner 写下的 lease
- `Run-TestCellBcSmoke.ps1` 当前仍使用临时 helper 插件 `bbswitch` 做跨服，因为 Waterfall 自带 `cmd_server` 模块下载链路在本机返回 `403`。
- `Run-TestCellBcSmoke.ps1` 支持 `-BackendCellIds` 多后端 smoke；当前已验证 `cell-02 -> cell-01 -> cell-03 -> cell-04 -> cell-05`。
- `Run-TestCellBcSmoke.ps1` 支持业务专项 hook：
  - `-BeforeTransferHookScript <path>` 在首次跨服命令前执行
  - `-AfterTransferHookScript <path>` 在每次目标 backend relay 成功后执行
  - hook 脚本会收到 `-Phase`、`-PlayerName`、source/target cell id 和 source/target plugin/mod HTTP port
  - 外部 wrapper 已经自行调用 `Prepare-TestCellBcBackends.ps1` 时，可配合 `-SkipPrepare -SkipRestore` 复用同一租约和 artifact 部署窗口
- Bot 连接 BC 时使用 `localhost:25645`，不要使用 `127.0.0.1:25645`。
- `Run-TestCellBcSmoke.ps1` 的 cleanup 必须完成：
  - stop BC
  - stop bot
  - restore 后端配置
  - release lease
  - 删除 `TestCellBcBridgeHelper.jar`，若刚停服后文件仍被短暂占用，需要等待后端进程退出并重试
  - 删除临时 `start-bc-backend-*.cmd` launcher

## 两套测试流

### 1. `runAll` 旧流程

- 在 `BlackBoxTestRunner.kt` 内部手工拼装一组 `TestCase`
- 强依赖截图前中后链路
- 更像“演示型 smoke 回放”

### 2. catalog 驱动流程

- `runSmoke` / `runFull` / `runCategory` / `runAction`
- 由 `BlackBoxTestCatalog.kt` 从 `ActionCatalog` 派生测试项
- 支持：
  - 分类
  - profile
  - 版本差异
  - 夹具准备
  - 自动跳过

当前更应该优先关注第二套。

## 测试真源文件

- Action 来源：`common/.../ActionCatalog.kt`
- 测试目录：`plugin/.../BlackBoxTestCatalog.kt`
- 执行器：`plugin/.../BlackBoxTestRunner.kt`
- 上下文与夹具：`plugin/.../command/testframework/`

## Loader Profile

测试框架当前只区分两类：

- `MC_12111`
- `MC_1122`

检测方式不是看客户端 Mod，而是看 Bukkit 版本前缀：

- `1.12.*` -> `MC_1122`
- 其他 -> `MC_12111`

## 夹具系统

`BlackBoxFixtureManager` 负责：

- 重置玩家基线状态
- 恢复/清理追踪方块
- 恢复/清理追踪实体
- 打开箱子容器
- 生成基础方块、实体、书、告示牌等测试前置条件

`resetBaseline()` 默认会：

- 关闭背包
- 传送回 origin
- 切生存模式
- 清空背包
- 清火焰、坠落、药水效果
- 预放几种热键栏测试物品

## 版本差异与自动跳过

### `unsupportedOn1122`

`BlackBoxTestCatalog.kt` 明确列出了一批 `1.12.2` 不支持的 action。

### `pendingFixtureActions`

很多 action 会在没有夹具、环境或专用场景时被跳过，而不是直接执行失败。

### 已在代码里显式标注的风险点

- `keep_alive`
  可能因为硬编码 id 造成玩家被踢出，已被测试目录特殊处理。
- 一部分实体交互、书本操作、特殊 GUI、命令方块类 action
  已在测试目录里写明“待专门夹具/待排查”。
- `type_text`
  需要先打开带文本输入目标的 GUI；当前 `BlackBoxTestCatalog` 把它放在 `pendingFixtureActions`，避免 full 回放在无屏幕状态下误失败。
- `key_press`
  可用于基础热键验证；真实场景里要先确认当前屏幕状态，否则同一个按键会根据 GUI 是否打开走不同路径。

## 验证结果输出

catalog 流程最终会汇总：

- `passed`
- `failed`
- `skipped`
- `total`
- `totalMs`
- `results[]`

截图 action 仍会把文件落到默认截图目录下。

## 推荐验证顺序

1. `Get-TestCellStatus.ps1` 看状态。
2. `Acquire-TestCell.ps1` 抢一个空闲 cell，避免与别的会话冲突。
3. `Invoke-TestCell.ps1 -Mode ensure -CellId cell-0x` 拉起服务端和 bot。
4. 先看 relay 是否通，再跑 `run_test` 或业务自动化脚本。
5. 结束后执行 `Invoke-TestCell.ps1 -Mode stop -CellId cell-0x` 或 `Stop-AllTestCells.ps1`。

## Forge 1.20.1 test-cell 流程

1. `Provision-TestCells1201.ps1` 需要显式传 `-SourceServerDir`，或通过环境变量 `BLACKBOXPRO_TESTCELLS_1201_SERVER_TEMPLATE` 提供服务端模板目录。
2. provision 后会重写：
   - `server.properties` 的 `server-port`、`online-mode=false`、`enforce-secure-profile=false`
   - `plugins/BlackBoxPro/config.yml` 的 `http-port`、`mod-http-address`、`test-mode=dual`
3. 客户端 provision 需要显式传 `-SourceVersionDir`，或通过 `-GameRoot` / `BLACKBOXPRO_TESTCELLS_GAME_ROOT` 自动发现 `1.20.1-Forge_47.3.0` 版本目录，然后做精简：
   - `mods/` 清空
   - 删除整合包侧车目录
   - 建立 `assets` / `libraries` junction
4. 日常联调顺序：
   - `.\gradlew forge1201_build`
   - `.\gradlew plugin_build`
   - `Sync-TestCell1201Artifacts.ps1`
   - `Invoke-TestCell1201.ps1 -Mode ensure -CellId cell-06`
   - 需要基础 smoke 时再执行 `Invoke-TestCell1201.ps1 -Mode smoke -CellId cell-06`
   - `Sync-TestCell1201Artifacts.ps1` 当前只应同步正式运行 jar，不应把 `*-dev-run.jar` 或其他调试产物推进 cell
5. `Invoke-TestCell1201.ps1 -Mode ensure` 会：
   - 清理旧服务端 `cmd/java` 进程
   - 起 Arclight 服务端并等待 `serverPort` 与插件 `/status`
   - 直启 Java 17 客户端并等待 `modHttpPort`
   - 自动执行 `connect_to_server`
   - 若进入 `DisconnectedScreen` 且原因包含 `Server is still starting`，会先 `close_screen` 再自动重连
   - 最终以 `query_player_state` 成功作为 ready 判定

## Forge 1.12.2 模组 cell-20..22 池流程

`cell-20..22` 用于测试 Forge 1.12.2 模组，不混入普通 `cell-01..05` 插件回归池。它们复用 `cell-01` 的 CatServer 服务端和 1.12.2 bot 客户端模板，但有独立目录、端口和 lease。

给已有 `cell-20` 追加准备 `cell-21/22`：

```powershell
powershell -ExecutionPolicy Bypass -File "scripts/test-cells/Provision-TestCellMod1122.ps1"
```

如需从空目录重建三格池，显式传入全部目标并加 `-Force`：

```powershell
powershell -ExecutionPolicy Bypass -File "scripts/test-cells/Provision-TestCellMod1122.ps1" -TargetCellIds cell-20,cell-21,cell-22 -Force
```

同步被测模组 jar 到服务端和客户端：

```powershell
powershell -ExecutionPolicy Bypass -File "scripts/test-cells/Sync-TestCellMod1122Artifacts.ps1" -ModJar "F:/mcplugins/mod/CloudStorage/build/libs/cloudstorage-0.1.0-SNAPSHOT.jar"
```

只做启动和联通验证：

```powershell
powershell -ExecutionPolicy Bypass -File "scripts/test-cells/Run-TestCellMod1122Regression.ps1" -Scope startup -ModJar "F:/mcplugins/mod/CloudStorage/build/libs/cloudstorage-0.1.0-SNAPSHOT.jar" -AcquireCell
```

保留现场给人工进游戏测 GUI：

```powershell
powershell -ExecutionPolicy Bypass -File "scripts/test-cells/Run-TestCellMod1122Regression.ps1" -Scope startup -ModJar "F:/mcplugins/mod/CloudStorage/build/libs/cloudstorage-0.1.0-SNAPSHOT.jar" -AcquireCell -KeepCell
```

收尾：

```powershell
powershell -ExecutionPolicy Bypass -File "scripts/test-cells/Invoke-TestCell.ps1" -Mode stop -CellId <cell-id> -ConfigPath "scripts/test-cells/cells-mod1122.json"
```

`Sync-TestCellMod1122Artifacts.ps1` 默认同步到 `cell-20..22`，并按 jar 名推断旧版本清理模式，例如 `cloudstorage-0.1.0-SNAPSHOT.jar` 会清理两端 `cloudstorage-*.jar`，不会清掉 BlackBoxPro / GermMod 等基线 mod。

## Client Visibility Policy

- Automated regression scripts hide the Minecraft client by default: `Invoke-TestCell.ps1`, `Invoke-TestCell1201.ps1`, `Run-TestCellRegression.ps1`, `Run-TestCellMod1122Regression.ps1`, and `Run-TestCellBcSmoke.ps1`.
- Pass `-ShowClient` only when visual debugging is needed.
- Manual hand-test sessions pass `ShowClient=true` automatically, so the client window is visible to the operator.
- Each current client root under `G:/MC/game/BlackBoxProTestCells/cell-*` has `Start-ManualTest.cmd` for double-click hand-testing. It calls `scripts/test-cells/Start-TestCellManualClient.ps1`, acquires the pinned cell, starts the matching server/client visibly, waits for Enter, then runs `stop + release`.
- The launchers cover `cell-01..08`, isolated `cell-10`, and `cell-20..22`; `cell-10` uses a generated ignored runtime config under `scripts/test-cells/locks/manual-client/`.
- `screenshot` / `screenshot_tooltip` capture the Minecraft framebuffer, not the Windows desktop. Hidden clients are acceptable for automation, but screenshot tests must still validate `filePath`, dimensions, file size, and the expected screen state.

## QQFarm 闭环验证现状

- `QQFarm Sprint 02` 的 `visit -> steal -> owner event` 成功分支和 breeder 拦截分支，已经在这套 test-cell 环境里通过自动化拿到 PASS 证据。
- 推荐直接用：
  - `powershell -ExecutionPolicy Bypass -File "<qqfarm_repo>/scripts/run-sprint-02-blackbox-qa.ps1" -Mode both -AcquireCell -CleanupCell -CellConfigPath "<blackboxpro_repo>/scripts/test-cells/cells.json"`

## 如何理解 `docs/testing/`

- 这些文档主要保留测试计划、旧回归记录和历史环境快照。
- 真正的“当前哪些 action 会跑、哪些会跳过、为什么会跳过”，应优先查 `BlackBoxTestCatalog.kt`。
