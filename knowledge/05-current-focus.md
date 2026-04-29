# BlackBoxPro 当前现状与偏差

## 当前最重要的判断

- 当前代码真实主链是 HTTP 中继，不是服务端 Plugin Message Channel。
- README、AGENTS 和部分旧开发文档仍保留旧架构描述，阅读时必须带着“历史背景文档”的心态。
- 如果后续有人要继续开发 transport、测试框架或构建脚本，先看代码，再看旧文档。
- 本地 test-cell 现在分成两套池：
  - `1.12.2` 仍用 `cell-01..05`
  - Forge `1.20.1` 独立用 `cell-06..08` + `cells-1201.json` + 专用 `Invoke/Provision/Sync/Stop` 脚本
- Forge `1.20.1` 客户端路线已经切到“精简 mod”模式，默认只保留 `BlackBoxPro` 客户端模组，不再沿用整合包第三方 mod 列表。
- 1.12.2 Forge 的 Germ 只读探针已推进到 `query_germ_hit_test`：当前能在真实 `germ_gui_loading` 页面返回 texture、text、gif 的候选 bounds 和 hit 结果，但还没有专用点击动作。

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
- 当前 `ActionCatalog.kt` 已登记 `117` 个 action。
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
- 改 action 时，默认先同步 `1.21.11`，再判断 `1.21.1` 和 `1.12.2` 是否需要跟进。
- 改测试时，优先看 `BlackBoxTestCatalog.kt` 和 `BlackBoxTestRunner.kt`，再参考 `docs/testing/`。
- 改构建说明时，以 `build.gradle.kts` 和各模块 `build.gradle.kts` 为准，不直接抄 README。
- 做 Germ 点击验收时，先用 `query_germ_hit_test` 采样真实业务页面 bounds；隐藏 1.12.2 bot 需要先确保 `pauseOnLostFocus:false`，否则会自动回到 `GuiIngameMenu`；只有 bounds 稳定后再设计显式点击 hook，不要把副作用放进 query action。
