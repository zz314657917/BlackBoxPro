# BlackBoxPro 当前状态与偏差

## 当前最重要的判断

- 当前代码真实主链是 HTTP relay，不是服务端 Plugin Message Channel；旧 Plugin Message Channel 描述只作为历史背景。
- Action 真源是 `common/src/main/kotlin/com/blackboxpro/common/action/ActionCatalog.kt`；精确支持数量以 `ActionCatalog` 和运行时 `/status` 为准。
- 本地 test-cell 默认心智模型：
  - `cell-01..05` 是 1.12.2 Germ + BC 后端统一回归池，不再把 `cell-04/05` 当特殊 Germ 机位。
  - `cell-06..08` 是 Forge 1.20.1 独立池，使用 `scripts/test-cells/cells-1201.json`。
  - `cell-20..22` 是 Forge 1.12.2 模组专测池，使用 `scripts/test-cells/cells-mod1122.json`。
- `cell-01..05` 应保持服务端插件基线：`BlackBoxPro-Plugin`、`PlayerCurrency`、`PlayerPoints`、`LuckPerms`、`GermPlugin`、`Vault`、`PlaceholderAPI`、`ProtocolLib`。
- 1.12.2 bot 基线应保持 `GermMod`、JEI 核心 `辅助-jei.jar`，并默认启用 `Minecraft-Mod-Language-Modpack.zip`。
- 受管测试服务端端口必须全局唯一，且不使用 `25565` / `25566`；当前 `cell-01` 端口为 `25570`。
- 1.12.2 测试服务端默认超平坦：`level-type=FLAT`、`generator-settings=`；旧 `world/` 需要先归档或删除，配置才会在下次启动时生效。

## 当前能力边界

- 1.12.2 Forge 已新增通用屏幕鼠标层：
  - `move_mouse`
  - `click_mouse`
  - `click_screen_at`
  - `query_cursor_state`
- `query_cursor_state`、`move_mouse`、`click_screen_at` 已在本地真实环境完成基础验证；`click_screen_at` 在原版 `GuiInventory` 已验证生效，但不能直接推断所有 Germ 页面都稳定可点。
- 1.12.2 Forge 已新增基础键盘输入层：
  - `key_press`
  - `type_text`
- Germ 只读探针已推进到：
  - `query_germ_screen`
  - `query_germ_hit_test`
- `query_germ_hit_test` 已能在真实 `germ_gui_loading` 页面返回 texture、text、gif 的候选 bounds 和 hit 结果；query 类 action 必须保持只读。
- Germ 显式副作用动作已增加：
  - `click_germ_component`：客户端侧组件点击尝试，仍不纳入默认 `run_test`，在 Lmshop 真页上尚未触发购买。
  - `germ_gui_part_dos`：服务端侧按 `guiName + partId + dosType` 解析 Germ YAML / runtime part 的 dos，`execute=false` 只解析，`execute=true` 才执行。
  - `germ_gui_part_dos` 支持 `command_util`、`packet`、`player_command` 模式；当前 Lmshop 证据显示 `command_util` / `packet` 未触发 `/lmshop buy`，`player_command` 可执行 `playercmd<->...` 并生成订单。

## 已确认的现状差异

- README、AGENTS 和部分旧开发文档仍保留历史架构描述；继续开发 transport、测试框架或构建脚本时，先看代码和 `knowledge/`，再看旧文档。
- 当前插件 API 真实链路是 `BlackBoxApi -> ModRelayClient -> HTTP /execute`。
- `/actions` 目前只有常量，插件和客户端 HTTP server 当前注册的是 `/execute` 和 `/status`。
- 客户端运行时配置目前主要按代码默认值运行；除非后续补配置加载，不要假设端口、超时、安全配置已由外部文件覆盖。
- `docs/testing/` 下很多内容记录的是旧版本、旧平台或旧端口；可作为历史测试思路，不应直接当当前执行说明。

## 当前建议的工作假设

- 改 transport 时，默认以 HTTP relay 为当前生产链路。
- 改 action 时，先判断是否需要同步 1.12.2 Germ / 屏幕鼠标 / 键盘层；如果只是现代端能力，再判断 `1.21.11` 和 `1.21.1` 是否需要同时跟进。
- 改测试时，优先看 `knowledge/03-build-and-verify.md`、`knowledge/06-test-system.md`、`BlackBoxTestCatalog.kt` 和 `BlackBoxTestRunner.kt`。
- 改构建说明时，以 `build.gradle.kts` 和各模块 `build.gradle.kts` 为准，不直接抄 README。
- 做 Germ 点击验收时，先用 `query_germ_hit_test` 采样真实业务页面 bounds；隐藏 1.12.2 bot 需要先确认 `pauseOnLostFocus:false`，否则可能自动回到 `GuiIngameMenu`。
- 物理坐标点击仍要和 `germ_gui_part_dos` 区分：前者验证真实鼠标/Germ 客户端事件，后者验证 Germ YAML `clickDos` 语义执行。不要把 `germ_gui_part_dos` 写成真实物理点击通过。
