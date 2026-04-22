# BlackBoxPro 测试系统

## 当前环境编排

### test-cell 池

- 当前 1.12.2 本地回归统一走 `cell-01..05`。
- 当前 Forge 1.20.1 本地回归走独立的 `cell-06..08`，配置文件固定为 `scripts/test-cells/cells-1201.json`，不混入原 `cells.json`。
- 服务端目录命名约定：
  - `cell-01..05` -> `server-cell-01..05`
  - `cell-06..08` -> `server-cell-1201-06..08`
- 客户端目录命名约定：
  - `cell-01..05` -> `cell-xx/.minecraft/versions/bot`
  - `cell-06..08` -> `cell-xx/.minecraft/versions/1.20.1-Forge_47.3.0`
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

### 端口约定

- `cell-01`：`25565 / 38080 / 38081`
- `cell-02`：`25575 / 38090 / 38091`
- `cell-03`：`25585 / 38100 / 38101`
- `cell-04`：`25595 / 38110 / 38111`
- `cell-05`：`25605 / 38120 / 38121`
- `cell-06`：`25615 / 38130 / 38131`
- `cell-07`：`25625 / 38140 / 38141`
- `cell-08`：`25635 / 38150 / 38151`

### 与独立主测试服的边界

- 独立主测试服目录现在不属于当前测试系统的 active flow。
- `scripts/test-cells/` 下的状态、抢占、启动、停止、插件精简脚本，都应默认只面向 `server-cell-*`。
- `Minimize-TestCellServerPlugins.ps1` 已加保护：如果目标目录叶子名不匹配 `server-cell-*`，脚本会直接拒绝。
- `1.20.1` 池使用单独脚本：
  - `Provision-TestCells1201.ps1`
  - `Invoke-TestCell1201.ps1`
  - `Sync-TestCell1201Artifacts.ps1`
  - `Stop-AllTestCells1201.ps1`
- `Get-TestCellStatus.ps1`、`Acquire-TestCell.ps1`、`Release-TestCell.ps1` 继续复用，但必须显式传 `-ConfigPath "scripts/test-cells/cells-1201.json"`。

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

## QQFarm 闭环验证现状

- `QQFarm Sprint 02` 的 `visit -> steal -> owner event` 成功分支和 breeder 拦截分支，已经在这套 test-cell 环境里通过自动化拿到 PASS 证据。
- 推荐直接用：
  - `powershell -ExecutionPolicy Bypass -File "<qqfarm_repo>/scripts/run-sprint-02-blackbox-qa.ps1" -Mode both -AcquireCell -CleanupCell -CellConfigPath "<blackboxpro_repo>/scripts/test-cells/cells.json"`

## 如何理解 `docs/testing/`

- 这些文档主要保留测试计划、旧回归记录和历史环境快照。
- 真正的“当前哪些 action 会跑、哪些会跳过、为什么会跳过”，应优先查 `BlackBoxTestCatalog.kt`。
