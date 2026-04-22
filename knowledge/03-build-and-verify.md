# BlackBoxPro 构建与验证

## 构建前提

- 现代端与根构建使用 JDK 21。
- `1.12.2` 模块的 `Gradle` 运行 JVM 需要 17 或 21，但编译目标仍是 Java 8。
- 根、插件、`1.21.1`、`1.12.2` 的 `gradle.properties` 都设置了 `localhost:7890` 代理；无代理环境可能需要先处理依赖下载问题。

## 当前 1.12.2 验证入口

- 当前 active flow 统一走 `scripts/test-cells/`，不再把独立主测试服目录作为默认验证入口。
- 默认 cell 池为 `cell-01..05`，服务端目录命名约定为 `server-cell-01..05`。
- 客户端目录命名约定为 `cell-01..05/.minecraft/versions/bot`。
- 所有 test-cell 客户端当前通过 junction 共享：
  - `<shared_minecraft_root>/.minecraft/assets`
  - `<shared_minecraft_root>/.minecraft/libraries`
- 默认内存配置已收口为：
  - 服务端 `-Xms512M -Xmx1024M`
  - 客户端 `-Xms512m -Xmx1024m`

## 当前 1.20.1 验证入口

- `1.20.1` test-cell 池与 `1.12.2` 分离，固定使用 `scripts/test-cells/cells-1201.json`。
- 当前池为 `cell-06..08`：
  - 服务端目录命名约定：`server-cell-1201-06..08`
  - 客户端目录命名约定：`cell-06..08/.minecraft/versions/1.20.1-Forge_47.3.0`
- 客户端路线是“精简 mod 路线”：
  - `assets` / `libraries` 仍通过 junction 复用主整合包共享目录
  - 每个 cell 的 `mods/` 最终只保留 `BlackBoxPro-forge-1.20.1-*.jar`
  - 源整合包里的第三方 mod 和侧车目录不会保留
- 当前池默认内存已收口为：
  - 服务端 `-Xms1024M -Xmx1024M`
  - 客户端 `-Xms1024m -Xmx1024m`
- 仓库里提交的是脱敏样例配置；本地使用前需要先把 `cells.json` / `cells-1201.json` 改成你自己的真实路径。

## test-cell 常用命令

- 查看状态：
  - `powershell -ExecutionPolicy Bypass -File "scripts/test-cells/Get-TestCellStatus.ps1"`
- 抢占可用 cell：
  - `powershell -ExecutionPolicy Bypass -File "scripts/test-cells/Acquire-TestCell.ps1" -Owner "session-name" -ReadyOnly`
- 启动或校验单个 cell：
  - `powershell -ExecutionPolicy Bypass -File "scripts/test-cells/Invoke-TestCell.ps1" -Mode ensure -CellId cell-01`
- 停止单个 cell：
  - `powershell -ExecutionPolicy Bypass -File "scripts/test-cells/Invoke-TestCell.ps1" -Mode stop -CellId cell-01`
- 停止全部 cell：
  - `powershell -ExecutionPolicy Bypass -File "scripts/test-cells/Stop-AllTestCells.ps1"`
- 精简 test-cell 服务端插件：
  - `powershell -ExecutionPolicy Bypass -File "scripts/test-cells/Minimize-TestCellServerPlugins.ps1"`

## 1.20.1 test-cell 常用命令

- provision 三个 cell：
  - `powershell -ExecutionPolicy Bypass -File "scripts/test-cells/Provision-TestCells1201.ps1"`
- 查看状态：
  - `powershell -ExecutionPolicy Bypass -File "scripts/test-cells/Get-TestCellStatus.ps1" -ConfigPath "scripts/test-cells/cells-1201.json"`
- 抢占可用 cell：
  - `powershell -ExecutionPolicy Bypass -File "scripts/test-cells/Acquire-TestCell.ps1" -Owner "session-name" -ConfigPath "scripts/test-cells/cells-1201.json" -ReadyOnly`
- 同步当前插件和 Forge 1.20.1 客户端产物：
  - `powershell -ExecutionPolicy Bypass -File "scripts/test-cells/Sync-TestCell1201Artifacts.ps1"`
  - 该脚本只应同步正式运行 jar，不应把 `*-dev-run.jar` 推进 `mods/`
- 启动或校验单个 cell：
  - `powershell -ExecutionPolicy Bypass -File "scripts/test-cells/Invoke-TestCell1201.ps1" -Mode ensure -CellId cell-06`
- 跑基础 smoke：
  - `powershell -ExecutionPolicy Bypass -File "scripts/test-cells/Invoke-TestCell1201.ps1" -Mode smoke -CellId cell-06`
- 停止单个 cell：
  - `powershell -ExecutionPolicy Bypass -File "scripts/test-cells/Invoke-TestCell1201.ps1" -Mode stop -CellId cell-06`
- 停止全部 1.20.1 cell：
  - `powershell -ExecutionPolicy Bypass -File "scripts/test-cells/Stop-AllTestCells1201.ps1"`

## test-cell 插件精简边界

- `Minimize-TestCellServerPlugins.ps1` 默认只处理叶子目录名符合 `server-cell-*` 的受管测试服。
- 即使显式传 `cell-01`，只要该 cell 不在 `server-cell-*` 路径下，脚本也会直接拒绝。
- 主测试服 `plugins/` 目录不属于当前 active flow；如果需要操作它，必须走显式人工决策，不应复用 test-cell 精简脚本。

## 根项目常用任务

### 推荐入口

- `.\gradlew buildAll`
  构建 `common`、`1.21.11`、`1.20.1`、`1.12.2` 和 `plugin`，并把产物收集到根 `build/libs/`。
- `.\gradlew plugin_build`
  单独构建服务端插件。
- `.\gradlew forge1122_build`
  单独构建 `1.12.2` 独立工程。
- `.\gradlew forge1201_build`
  单独构建 `1.20.1` 独立 Forge 工程。

### 现代端分开构建

- `.\gradlew mod2111_build`
  构建 `1.21.11 runtime/fabric/neoforge`。
- `1.21.1` 当前不再提供根包装任务。
  如果后续确实要继续构建这条兼容线，改为在 `mod/` 聚合工程内显式执行 `:1.21.1:*` 子模块任务。

### 清理

- `.\gradlew cleanAll`

## 模块内构建入口

- `.\gradlew -p mod buildAll`
  构建 `mod` 聚合工程下的 `common`、`1.21.11`、`1.21.1` 和 `1.12.2` 客户端产物。
- 在 `mod/1.12.2/` 目录下执行 `.\gradlew build` 或 `.\gradlew buildAll`
  构建 `runtime` + `forge`。

## 与 README/AGENTS 的差异

- README 和 AGENTS 里写了 `.\gradlew mod_buildAll`。
- 当前根 `build.gradle.kts` 里没有 `mod_buildAll` 任务。
- 如果要构建现代端聚合，当前可用做法是：
  - 用根任务 `buildAll`
  - 或者执行 `.\gradlew -p mod buildAll`

## 主要产物路径

- 根收集目录：`build/libs/`
- 插件：`plugin/build/libs/BlackBoxPro-Plugin-<version>.jar`
- Fabric 1.21.11：`mod/1.21.11/fabric/build/libs/BlackBoxPro-fabric-1.21.11-<version>.jar`
- NeoForge 1.21.11：`mod/1.21.11/neoforge/build/libs/BlackBoxPro-neoforge-1.21.11-<version>.jar`
- Fabric 1.21.1：`mod/1.21.1/fabric/build/libs/BlackBoxPro-fabric-1.21.1-<version>.jar`（仅在 `mod/` 聚合工程内单独构建时产出）
- NeoForge 1.21.1：`mod/1.21.1/neoforge/build/libs/BlackBoxPro-neoforge-1.21.1-<version>.jar`（仅在 `mod/` 聚合工程内单独构建时产出）
- Forge 1.12.2：`mod/1.12.2/forge/build/libs/BlackBoxPro-forge-1.12.2-<version>.jar`

## 运行时端口

- `cell-01`：`25565 / 38080 / 38081`
- `cell-02`：`25575 / 38090 / 38091`
- `cell-03`：`25585 / 38100 / 38101`
- `cell-04`：`25595 / 38110 / 38111`
- `cell-05`：`25605 / 38120 / 38121`
- `cell-06`：`25615 / 38130 / 38131`
- `cell-07`：`25625 / 38140 / 38141`
- `cell-08`：`25635 / 38150 / 38151`

## 最短验证路径

### 单个 cell

1. 构建需要的插件或 Mod 产物。
2. 按需把产物复制到目标 cell 目录，例如：
   - `<test_cell_server_root>/server-cell-01/plugins/`
   - `<test_cell_workspace_root>/cell-01/.minecraft/versions/bot/mods/`
3. 执行：
   - `powershell -ExecutionPolicy Bypass -File "scripts/test-cells/Invoke-TestCell.ps1" -Mode ensure -CellId cell-01`
4. 观察：
   - `GET http://localhost:38080/status`
   - `GET http://localhost:38081/status`
   - relay 的 `query_player_state` 是否成功
5. 验证完成后执行：
   - `powershell -ExecutionPolicy Bypass -File "scripts/test-cells/Invoke-TestCell.ps1" -Mode stop -CellId cell-01`

### QQFarm / 业务链自动化

1. 使用 `QQFarm/scripts/run-sprint-02-blackbox-qa.ps1`
2. 推荐命令：
  - `powershell -ExecutionPolicy Bypass -File "<qqfarm_repo>/scripts/run-sprint-02-blackbox-qa.ps1" -Mode both -AcquireCell -CleanupCell -CellConfigPath "<blackboxpro_repo>/scripts/test-cells/cells.json"`
3. 期望结果里至少看到：
   - `ok=true`
   - `fixturesRestored=true`
   - `cellStopped=true`

### Forge 1.20.1 单个 cell

1. 执行 `.\gradlew forge1201_build` 和 `.\gradlew plugin_build`。
2. 执行：
   - `powershell -ExecutionPolicy Bypass -File "scripts/test-cells/Sync-TestCell1201Artifacts.ps1"`
3. 拉起 cell：
   - `powershell -ExecutionPolicy Bypass -File "scripts/test-cells/Invoke-TestCell1201.ps1" -Mode ensure -CellId cell-06`
4. 观察：
   - `GET http://localhost:38130/status`
   - `GET http://localhost:38131/status`
   - direct `query_screen_state`
   - direct 或 relay `query_player_state`
5. 需要截图/基础回归时执行：
   - `powershell -ExecutionPolicy Bypass -File "scripts/test-cells/Invoke-TestCell1201.ps1" -Mode smoke -CellId cell-06`

## 已确认但尚未实现的接口

- `common/.../HttpEndpoints.kt` 里定义了 `/actions`
- 当前插件与客户端 HTTP Server 都没有注册这个路由
- 如果后续要做动作枚举接口，需要同时补服务端与客户端实现
