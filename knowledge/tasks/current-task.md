# 当前任务

## 背景

- 用户需要一组专门测试 Forge 1.12.2 模组的 BlackBoxPro test-cell。
- 用户已确认方案：服务端直接复制 `cell-01`，客户端也直接复制 `cell-01`；先有 `cell-20`，本轮追加 `cell-21/22`。
- 普通 1.12.2 插件/Germ/BC 回归仍使用 `cell-01..05`，Forge 1.20.1 仍使用 `cell-06..08`；`cell-20..22` 不混入这些池。

## 当前目标

- 收口 `cell-20..22` 作为 Forge 1.12.2 模组专测池：
  - 独立配置文件
  - 独立服务端与客户端目录
  - 独立端口与 lease
  - 可同步被测模组到服务端和客户端 `mods/`
  - 可一条命令做 startup/smoke 并自动清理

## 本次已完成

- 新增 `scripts/test-cells/cells-mod1122.json`，默认 cell 为 `cell-20`，当前池包含 `cell-20..22`。
- 新增 `scripts/test-cells/Provision-TestCellMod1122.ps1`：
  - 默认从 `scripts/test-cells/cells.json` 的 `cell-01` 读取源服务端和源客户端。
  - 默认目标为 `cell-21/22`，用于给已有 `cell-20` 追加两格模组测试端。
  - 如需重建完整三格池，显式传 `-TargetCellIds cell-20,cell-21,cell-22 -Force`。
  - 复制服务端到 `F:/minecraft/test-cells/server-cell-mod1122-20..22`。
  - 复制客户端版本目录到 `G:/MC/game/BlackBoxProTestCells/cell-20..22/.minecraft/versions/bot`。
  - `assets` / `libraries` 用 junction 指向源 cell 的共享资源。
  - 重写 `server.properties` 和 `plugins/BlackBoxPro/config.yml` 的端口与 dual 模式。
  - `-Force` 删除前校验目标必须位于 `AllowedServerRoot` / `AllowedClientRoot` 下，避免误删非 test-cell 目录。
- 新增 `scripts/test-cells/Sync-TestCellMod1122Artifacts.ps1`：
  - 默认把指定模组 jar 同步到 `cell-20..22` 服务端和客户端 `mods/`。
  - 默认按 jar 名推断清理模式，例如 `cloudstorage-*.jar`。
- 新增 `scripts/test-cells/Run-TestCellMod1122Regression.ps1`：
  - 支持 `-Scope startup|smoke`。
  - 支持 `-ModJar`、`-AcquireCell`、`-KeepCell`。
  - `-AcquireCell` 未显式传 `-CellId` 时，从 `cell-20..22` 里抢一个 ready cell。
  - 默认结束后执行 `stop + release`。
- 加固 `Invoke-TestCell.ps1` 的 stop 收尾：先杀匹配 server cmd，再杀 server/bot Java，随后二次清扫孤儿 `cmd.exe`，避免留下无监听但窗口未关的残留。
- 1.12.2 测试客户端资源已补齐：
  - `辅助-jei.jar` 已复制到 `cell-01..05`、隔离 `cell-10`、`cell-20..22` 的 `mods/`
  - `Minecraft-Mod-Language-Modpack.zip` 已复制到同一批客户端的 `resourcepacks/`
  - `baseline-1122.json` 已加入 JEI、资源包模式和 `botDefaultResourcePacks`
  - `Sync-TestCellBaselinePlugins.ps1` 已支持 bot `resourcepacks/` 同步，并会把默认资源包写入 `options.txt`
  - `Set-TestCellBotResourcePacks.ps1` 已补为一键同步/启用脚本，当前 `cell-01..05`、隔离 `cell-10`、`cell-20..22` 都已默认加载 `file/Minecraft-Mod-Language-Modpack.zip`
- 1.12.2 测试服务端已统一超平坦：`cell-01..05`、隔离 `server-cell-10`、`cell-20..22` 的 `server.properties` 均为 `level-type=FLAT`、`generator-settings=`；旧 `world/` 已归档到各自 `_world-backups/world-20260428-181025`。
- 已用 `cell-03` 做真实启动验证：relay `query_player_state` 返回 overworld、`y=4.0`、满血且非死亡；验证后已 stop/release，目标监听端口与匹配 `cmd/java/javaw` 进程均为 0。
- 更新 `knowledge/05-current-focus.md`、`knowledge/06-test-system.md` 和全局 `$blackboxpro-local-regression` skill 的 `cell-20..22` 入口说明。

## 已确认事实

- `cell-20..22` 配置：
  - `cell-20`：`F:/minecraft/test-cells/server-cell-mod1122-20`、`G:/MC/game/BlackBoxProTestCells/cell-20/.minecraft/versions/bot`、`25720 / 38200 / 38201`
  - `cell-21`：`F:/minecraft/test-cells/server-cell-mod1122-21`、`G:/MC/game/BlackBoxProTestCells/cell-21/.minecraft/versions/bot`、`25721 / 38210 / 38211`
  - `cell-22`：`F:/minecraft/test-cells/server-cell-mod1122-22`、`G:/MC/game/BlackBoxProTestCells/cell-22/.minecraft/versions/bot`、`25722 / 38220 / 38221`
  - 服务端 Java：`C:/Program Files/Java/jdk1.8.0_481/bin/java.exe`
  - 客户端 Java：`C:/Program Files/Java/jdk1.8.0_481/bin/javaw.exe`
  - 1.12.2 服务端默认内存：`512M / 1536M`
  - 1.12.2 客户端默认内存：`512M / 1024M`
- `Provision-TestCellMod1122.ps1` 已成功追加执行，输出确认 `sourceCellId=cell-01`、`provisioned.id=cell-21,cell-22`。
- provision 后 `Get-TestCellStatus.ps1 -ConfigPath scripts/test-cells/cells-mod1122.json` 返回 `cell-20..22` 全部 `ready=true`、`locked=false`。
- `Acquire-TestCell.ps1 -CellId cell-21/cell-22 -ReadyOnly -ConfigPath scripts/test-cells/cells-mod1122.json` 已成功抢占，`Release-TestCell.ps1` 已成功释放。
- `cell-21/22` 的 `assets` 和 `libraries` 都是 `Directory, ReparsePoint`，目标指向 `cell-01` 的共享资源目录。

## 待验证点

- 本轮已对 `cell-21/22` 做了 startup 验证：服务端 `/status`、客户端 `/status`、`connect_to_server` 和 relay `query_player_state` 均成功，并自动 stop/release。
- 尚未同步具体业务模组 jar 做功能测试。
- 后续测试 CloudStorage 或其他 Forge 1.12.2 模组时，需要先构建目标 jar，再通过 `Sync-TestCellMod1122Artifacts.ps1` 或 `Run-TestCellMod1122Regression.ps1 -ModJar` 同步。
- `Scope smoke` 还未在 `cell-20..22` 上跑完整 `run_test`。

## 当前结论

- `cell-20..22` 已可作为独立 Forge 1.12.2 模组测试池使用。
- 用户澄清的复制策略已落实：服务端复制 `cell-01`，客户端也复制 `cell-01`。
- 环境验证后已自动收尾，当前没有 `cell-20..22` 相关监听端口或 `cmd/java/javaw` 残留进程。

## 下一步

1. 如果要测试 CloudStorage，先在 `F:/mcplugins/mod/CloudStorage` 构建 jar，再执行：
   `powershell -ExecutionPolicy Bypass -File "F:/mcplugins/BlackBoxPro-dev-2.0/scripts/test-cells/Run-TestCellMod1122Regression.ps1" -Scope startup -ModJar "F:/mcplugins/mod/CloudStorage/build/libs/cloudstorage-0.1.0-SNAPSHOT.jar" -AcquireCell`
2. 如果要人工进游戏测 GUI，在上面的命令后加 `-KeepCell`，结束后手动执行：
   `powershell -ExecutionPolicy Bypass -File "F:/mcplugins/BlackBoxPro-dev-2.0/scripts/test-cells/Invoke-TestCell.ps1" -Mode stop -CellId <cell-id> -ConfigPath "F:/mcplugins/BlackBoxPro-dev-2.0/scripts/test-cells/cells-mod1122.json"`
3. 如果要把这批 test-cell 基础设施提交，先区分已有历史改动和本轮新增文件，避免把无关 `cells.json` 本地路径状态混入提交。

## 验证记录

- PowerShell 静态解析：
  - `Provision-TestCellMod1122.ps1`：`errorCount=0`
  - `Sync-TestCellMod1122Artifacts.ps1`：`errorCount=0`
  - `Run-TestCellMod1122Regression.ps1`：`errorCount=0`
- 当前状态：
  - `Get-TestCellStatus.ps1 -ConfigPath scripts/test-cells/cells-mod1122.json`
  - `cell-20..22` 全部 `ready=true`、`locked=false`
- 本轮追加准备：
  - `Provision-TestCellMod1122.ps1`
  - 结果 `sourceCellId=cell-01`，`provisioned.id=cell-21,cell-22`
- lease 验证：
  - `Acquire-TestCell.ps1 -CellId cell-21 -Owner codex-mod1122-pool-check-* -ConfigPath scripts/test-cells/cells-mod1122.json -ReadyOnly`
  - `Release-TestCell.ps1 -CellId cell-21 -Owner codex-mod1122-pool-check-* -ConfigPath scripts/test-cells/cells-mod1122.json`
  - `Acquire-TestCell.ps1 -CellId cell-22 -Owner codex-mod1122-pool-check-* -ConfigPath scripts/test-cells/cells-mod1122.json -ReadyOnly`
  - `Release-TestCell.ps1 -CellId cell-22 -Owner codex-mod1122-pool-check-* -ConfigPath scripts/test-cells/cells-mod1122.json`
- startup 验证：
  - `Run-TestCellMod1122Regression.ps1 -Scope startup -CellId cell-21 -AcquireCell -LeaseOwner codex-mod1122-startup-cell21`
  - `cell-21` 结果 `ok=true`；服务端 `httpPort=38210`、`modHttpAddress=http://localhost:38211`、`ready=true`；bot `httpPort=38211`、`actions=112`、`ready=true`；连接 `localhost:25721`；relay `query_player_state` 成功；cleanup stop/release 成功
  - `Run-TestCellMod1122Regression.ps1 -Scope startup -CellId cell-22 -AcquireCell -LeaseOwner codex-mod1122-startup-cell22`
  - `cell-22` 结果 `ok=true`；服务端 `httpPort=38220`、`modHttpAddress=http://localhost:38221`、`ready=true`；bot `httpPort=38221`、`actions=112`、`ready=true`；连接 `localhost:25722`；relay `query_player_state` 成功；cleanup stop/release 成功
- 收尾复查：
  - `Get-TestCellStatus.ps1` 返回 `cell-20..22` 全部 `ready=true`、`locked=false`
  - `25720/38200/38201/25721/38210/38211/25722/38220/38221` 无监听
  - 未发现匹配 `cell-20..22` 路径的 `cmd.exe/java.exe/javaw.exe` 残留进程
  - 复查中发现并清掉过旧 `cell-20` 空 cmd / Java 残留，已通过二次清扫逻辑补强 stop 脚本
- 内存配置：
  - `scripts/test-cells/cells.json` 和 `scripts/test-cells/cells-mod1122.json` 的 1.12.2 服务端默认值已改为 `serverMinMemoryMb=512`、`serverMaxMemoryMb=1536`
  - 已用 `cell-03` 实测新参数启动：服务端命令行为 `-Xms512M -Xmx1536M`，插件 `/status` 返回 `httpPort=38100`、`ready=true`，客户端 `/status` 返回 `httpPort=38101`、`ready=true`
  - `cell-03` 实测后已 stop/release，最终相关端口和匹配 `cmd/java/javaw` 进程均为 `0`
- 客户端资源：
  - JEI hash：`A4EDF0CB590409D48AD94363F917779C21661544C67651B69A526E1291996991`
  - 语言包 hash：`CF9FC259349A89D61E748234A8C58F3E3E88CC7BD80DE8FF3EE006D7475C4B9F`
  - `Sync-TestCellBaselinePlugins.ps1 -DryRun` 已确认 `cell-02..05` 的 JEI 与语言包均为 `skipped`
  - `Set-TestCellBotResourcePacks.ps1` 已确认 `cell-01..05`、`cell-10`、`cell-20..22` 的 `options.txt` 均为 `resourcePacks:["file/Minecraft-Mod-Language-Modpack.zip"]`
