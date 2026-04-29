# BlackBoxPro 当前现状与偏差

## 当前最重要的判断

- 当前代码真实主链是 HTTP 中继，不是服务端 Plugin Message Channel。
- README、AGENTS 和部分旧开发文档仍保留旧架构描述，阅读时必须带着“历史背景文档”的心态。
- 如果后续有人要继续开发 transport、测试框架或构建脚本，先看代码，再看旧文档。
- 当前默认测试心智模型已经变了：
  - `1.12.2` 的 `cell-01..05` 是统一的 Germ + BC 后端回归池，不再只把 `cell-04/05` 当特殊 Germ 机位
  - Forge `1.20.1` 独立用 `cell-06..08` + `cells-1201.json` + 专用 `Invoke/Provision/Sync/Stop` 脚本
  - Forge `1.20.1` 客户端路线已经切到“精简 mod”模式，默认只保留 `BlackBoxPro` 客户端模组，不再沿用整合包第三方 mod 列表
- 当前 `BlackBoxPro` 已经从“主要操作原版容器槽位”推进到“可对任意屏幕执行坐标级鼠标移动、点击和状态查询”的阶段，但这不等于 Germ 页面已经全部稳定可点。

## 当前最该先记住的新事实

- 1.12.2 Forge 已新增通用屏幕鼠标层：
  - `move_mouse`
  - `click_mouse`
  - `click_screen_at`
  - `query_cursor_state`
- `query_cursor_state`、`move_mouse`、`click_screen_at` 已在本地真实环境完成基础验证；其中 `click_screen_at` 在原版 `GuiInventory` 已验证生效，但在真实 Germ 页面上仍需继续确认事件消费链路。
- `cell-01..05` 当前都应保持：
  - 服务端插件基线包含 `BlackBoxPro-Plugin`、`PlayerCurrency`、`PlayerPoints`、`LuckPerms`、`GermPlugin`、`Vault`、`PlaceholderAPI`、`ProtocolLib`
  - bot mod 基线包含 `GermMod`
  - bot `resourcepacks/` 包含并默认启用 `Minecraft-Mod-Language-Modpack.zip`
- 受管测试服务端端口必须全局唯一，且不使用 `25565` / `25566`；当前 `cell-01` 端口为 `25570`。
- 1.12.2 测试服务端默认超平坦：`level-type=FLAT`、`generator-settings=`，旧 `world/` 已归档后才会生成新超平坦世界。
- 1.12.2 BC smoke 已支持 5 后端链路：
  - `cell-02 -> cell-01 -> cell-03 -> cell-04 -> cell-05`
  - cleanup 需要 stop BC、stop bot、restore 后端配置、release lease、删除 helper jar 和临时 launcher
- 后续做 BC/多后端联调时，bot 连接代理必须使用 `localhost:25645`，不要用 `127.0.0.1:25645`。
- Forge 1.12.2 模组专测已扩展为独立 `cell-20..22` 池：
  - 服务端从 `cell-01` 的 CatServer 模板复制
  - 客户端从 `cell-01` 的 1.12.2 bot 模板复制
  - 配置文件固定为 `scripts/test-cells/cells-mod1122.json`

## 已确认的现状差异

### 1. 旧文档写 Plugin Message Channel，当前插件代码没有对应实现目录

- `plugin/开发文档-1.1.0.md` 仍描述 `channel/ChannelHandler.kt` 等结构。
- 当前 `plugin/src/main/kotlin` 下没有 `channel/` 目录。
- 当前插件 API 真实链路是 `BlackBoxApi -> ModRelayClient -> HTTP /execute`。

### 2. README 与 AGENTS 写 `mod_buildAll`，当前根任务不存在

- README 和 AGENTS 都写了 `.\gradlew mod_buildAll`。
- 当前根 `build.gradle.kts` 里没有这个 task。
- 当前实际可用入口是根 `buildAll`，或者 `.\gradlew -p mod buildAll`。

### 3. Action 数量的历史数字已经漂移

- README 中的 action 数量是历史描述。
- 当前 `ActionCatalog.kt` 已登记 `115` 个 action。
- 后续涉及“支持多少 action”的描述时，应直接查 `ActionCatalog` 或运行时 `StatusHandler`。

### 4. `/actions` 路由仅有常量，没有实现

- `HttpEndpoints.kt` 有 `ACTIONS = "/actions"`。
- 插件和客户端 HTTP Server 当前都只注册了 `/execute` 与 `/status`。

### 5. 客户端运行时配置目前看起来仍是默认快照

- `RuntimeBlackBoxConfig` 提供了 `update(...)`。
- 当前仓库里没有找到任何 `update(...)` 调用。
- 这意味着客户端端口、超时、安全配置、截图目录等都按代码默认值运行，除非后续有人补上配置加载。

### 6. 测试文档大量带有旧版本和旧环境信息

- `docs/testing/` 下很多文档记录的是 `v1.3.1`、macOS、历史路径或历史端口。
- 它们对测试思路仍有价值，但不能直接当当前执行说明。

### 7. 当前工作区不是 Git 仓库根

- 根目录没有 `.git/`。
- 依赖 Git 的自动分析、版本差异判断、提交流程都需要先找到真正仓库根再做。

## 当前建议的工作假设

- 改 transport 时，默认以 HTTP 链路为当前生产链路。
- 改 action 时，默认先区分“是否需要同步 1.12.2 Germ / 屏幕鼠标层”；如果只是现代端能力，再判断 `1.21.11` 与 `1.21.1` 是否需要同时跟进。
- 改测试时，优先看 `knowledge/03-build-and-verify.md`、`knowledge/06-test-system.md`、`BlackBoxTestCatalog.kt` 和 `BlackBoxTestRunner.kt`，再参考 `docs/testing/`。
- 改构建说明时，以 `build.gradle.kts` 和各模块 `build.gradle.kts` 为准，不直接抄 README。
- 做 1.12.2 Germ 自动化时，默认先假设“屏幕鼠标层已经可用，但 Germ 组件级命中仍可能需要专用 probe / hook”，不要把 `click_screen_at` 的基础成功误读成“所有 Germ GUI 都已收口”。
