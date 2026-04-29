# BlackBoxPro 时间轴

## 2026-04-28 18:10 +08:00 - 1.12.2 测试服务端统一超平坦

- 当前阶段：`cell-01..05`、隔离 `server-cell-10`、`cell-20..22` 的 1.12.2 测试服务端已统一为超平坦世界配置。
- 本段重点：所有目标 `server.properties` 已写入 `level-type=FLAT`、`generator-settings=`，并保留 `level-name=world`，避免插件依赖默认世界名时出问题。
- 脚本真源：`Invoke-TestCell.ps1` 每次 ensure 会强制收敛 `server-port`、和平/无怪物和超平坦配置；`Provision-TestCells.ps1` 与 `Provision-TestCellMod1122.ps1` 也会在新建/重建 cell 时写入同样配置。
- 运行态处理：发现未加锁但仍监听的 `cell-20` 残留服务端，已用 `Invoke-TestCell.ps1 -Mode stop -ConfigPath cells-mod1122.json` 停止并确认端口释放。
- 世界目录处理：旧 `world/` 已归档到各服务端 `_world-backups/world-20260428-181025`；下次启动会重新生成名为 `world` 的 FLAT 世界。
- 验证记录：所有目标 1.12.2 测试服 `server.properties` 均为 `level-type=FLAT`、`generator-settings=`；用 `cell-03` 做了一次启动验证，relay `query_player_state` 返回 overworld、`y=4.0`、满血且非死亡；验证后已 stop/release，目标监听端口与匹配 `cmd/java/javaw` 进程均为 0。

## 2026-04-28 11:45 +08:00 - 1.12.2 测试客户端默认加载语言包

- 当前阶段：`Minecraft-Mod-Language-Modpack.zip` 已从“只复制到 resourcepacks”升级为“默认启用”。
- 本段重点：新增 `Set-TestCellBotResourcePacks.ps1`，默认覆盖 `cell-01..05`、隔离 `cell-10`、`cell-20..22`，会复制缺失语言包并更新 `options.txt`。
- 配置真源：`baseline-1122.json` 新增 `botDefaultResourcePacks=["Minecraft-Mod-Language-Modpack.zip"]`，`Sync-TestCellBaselinePlugins.ps1` 同步基线时会确保 `resourcePacks:["file/Minecraft-Mod-Language-Modpack.zip"]`。
- 验证记录：九个 1.12.2 客户端语言包 SHA256 均为 `CF9FC259349A89D61E748234A8C58F3E3E88CC7BD80DE8FF3EE006D7475C4B9F`，`options.txt` 均已写入 `file/Minecraft-Mod-Language-Modpack.zip`；二次 dry-run 显示无待变更。
- 清理记录：发现并停止一个未加锁的 `cell-22` orphan 服务端；最终普通池和模组池 lease 全部为空，相关端口和匹配 `cmd/java/javaw` 进程为 0。
- 边界说明：该资源包 `pack.mcmeta` 标注 `pack_format=3` 且“仅限1.12.2”，未强行写入 1.20.1 的 `cell-06..08`。

## 2026-04-27 20:46 +08:00 - Forge 1.12.2 模组专测扩展到 cell-20..22

- 当前阶段：`cell-20` 已扩展为 `cell-20..22` 三格 Forge 1.12.2 模组专测池。
- 本段重点：`cells-mod1122.json` 新增 `cell-21/22`，目录为 `server-cell-mod1122-21/22` 与 `G:/MC/game/BlackBoxProTestCells/cell-21/22`，端口为 `25721 / 38210 / 38211` 和 `25722 / 38220 / 38221`。
- 已完成：`Provision-TestCellMod1122.ps1` 默认追加准备 `cell-21/22`；`Sync-TestCellMod1122Artifacts.ps1` 默认同步到 `cell-20..22`；`Run-TestCellMod1122Regression.ps1 -AcquireCell` 未显式传 `-CellId` 时从三格池里抢 ready cell；`Invoke-TestCell.ps1` stop 增加二次 orphan `cmd.exe` 清扫。
- 资源配置：1.12.2 普通池和模组专测池的服务端默认内存已从 `2048M / 2048M` 调整为 `512M / 1536M`，用于降低并发测试占用；`cell-03` 已用 `-Xms512M -Xmx1536M` 实测启动，插件和客户端 `/status` 均 ready，随后 stop/release 成功。
- 客户端资源：已把 JEI 核心切换为 `辅助-jei.jar`，并和 `Minecraft-Mod-Language-Modpack.zip` 同步到 `cell-01..05`、隔离 `cell-10`、`cell-20..22`；`baseline-1122.json` 和 `Sync-TestCellBaselinePlugins.ps1` 已补成可同步 JEI 与 bot `resourcepacks/`。
- 验证记录：`Provision-TestCellMod1122.ps1` 输出 `sourceCellId=cell-01`、`provisioned.id=cell-21,cell-22`；`Get-TestCellStatus.ps1 -ConfigPath cells-mod1122.json` 返回三格全部 `ready=true`、`locked=false`；`cell-21/22` 均完成 acquire/release 与真实 startup 验证，服务端、客户端、连接和 relay 均成功；收尾后九个相关端口无监听且无匹配 `cmd/java/javaw` 残留。
- 遗留问题：本轮未同步具体业务模组 jar 做功能验证；下一次带具体 Forge 1.12.2 模组 jar 时再跑 `Run-TestCellMod1122Regression.ps1 -Scope startup -ModJar <jar> -AcquireCell`。

## 2026-04-27 00:55 +08:00 - Forge 1.12.2 模组专测 cell-20

- 当前阶段：`cell-20` 已作为独立 Forge 1.12.2 模组测试环境准备并完成 startup 验证。
- 本段重点：用户确认服务端复制 `cell-01`，客户端也复制 `cell-01`；`cell-20` 使用独立 `cells-mod1122.json`、独立端口和独立 lease。
- 已完成：新增 `cells-mod1122.json`、`Provision-TestCellMod1122.ps1`、`Sync-TestCellMod1122Artifacts.ps1`、`Run-TestCellMod1122Regression.ps1`，为 provision 的 `-Force` 删除补了路径边界校验，并更新测试系统文档和全局本地回归 skill。
- 关键决策：`cell-20` 不混入 `cell-01..05` 普通 1.12.2 插件/Germ/BC 回归池，专门用于需要服务端和客户端同时加载 Forge 1.12.2 模组的场景。
- 验证记录：PowerShell 静态解析 3 个新增脚本均 `errorCount=0`；provision 输出 `sourceCellId=cell-01`；startup 验证 `ok=true`，bot 连接 `localhost:25720`，relay `query_player_state` 成功，cleanup 后端口无监听且无相关 `cmd/java/javaw` 残留。
- 遗留问题：尚未同步具体业务模组 jar 做功能测试；`Scope smoke` 还未在 `cell-20` 上跑完整 `run_test`。
- 下一步：构建目标 Forge 1.12.2 模组 jar 后，用 `Run-TestCellMod1122Regression.ps1 -Scope startup -ModJar <jar> -AcquireCell` 做带业务模组的启动验证；需要手测 GUI 时加 `-KeepCell`，结束后用 `Invoke-TestCell.ps1 -Mode stop -ConfigPath scripts/test-cells/cells-mod1122.json` 收尾。
