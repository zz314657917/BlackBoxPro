# BlackBoxPro 时间轴

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
