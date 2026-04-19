# BlackBoxPro 开发规则

## 总体原则

- 先以当前代码为准，再参考 README、设计文档和旧测试报告。
- 修改范围尽量小，不顺手做与任务无关的重构。
- 多版本仓库改动要明确说明覆盖范围：`1.21.11`、`1.21.1`、`1.12.2` 是否同步。
- 如果设计文档与代码冲突，优先修正知识库，不要直接把旧描述继续传播。

## 插件侧约束

- 插件模块遵循 TabooLib 风格：
  - 配置用 `@Config`
  - 命令用 `@CommandHeader`
  - 调度用 `submit`
- 插件当前的核心调用链是 HTTP 中继，不要默认存在可直接复用的服务端 Plugin Message Channel 收发层。
- `ActionParamRegistry` 不自己维护 action 列表，而是直接读 `ActionCatalog`；新增 action 时不要只改命令补全。

## 客户端侧约束

- 所有 Minecraft 状态读写必须回到客户端主线程。
- 现代端通过 `RuntimeCommandDispatcherBootstrap` 绑定主线程执行器、action 解析器和响应发送器。
- `1.12.2` 端直接在 `BlackBoxProForge` 内绑定 `RuntimeCommandDispatcher` 与 `RuntimeResponseSender`。
- 客户端 HTTP `/execute` 只是入站壳层，真正的参数校验、延迟调度和安全检查在共享运行时里做。

## Action 相关规则

- Action ID 和参数名的真源只有 `common/.../ActionCatalog.kt`。
- 新增 action 时至少检查这些位置：
  - `common/.../ActionCatalog.kt`
  - 对应客户端实现和注册表
  - 插件 API 封装
  - 插件命令补全链路
  - 测试目录 `plugin/.../testframework`
  - 知识库与必要设计文档
- `batch` 内子 action 同样会走安全规则，不能假设嵌套后就绕过限制。

## 配置规则

- 插件配置真源是 `plugin/src/main/resources/config.yml` 与 `BlackBoxSettings.kt`。
- 客户端运行时配置真源是 `RuntimeBlackBoxConfig.current`。
- 当前代码里没有搜到 `RuntimeBlackBoxConfig.update(...)` 的调用，所以现代端和 1.12.2 端默认都在使用共享默认快照。

## 测试规则

- 测试系统分“旧的截图串行 smoke 流程”和“新的 catalog 驱动流程”两套，见 [06-test-system.md](06-test-system.md)。
- 测试数据和跳过逻辑优先看 `BlackBoxTestCatalog.kt`，不要只看 `docs/testing/`。
- 1.12.2 有专门的 `unsupportedOn1122` 集合，不能假设所有 action 三端全覆盖。

## 构建规则

- 构建优先走根任务或模块自身 `buildAll`，避免零散手工拼命令。
- 注意多个 `gradle.properties` 都写了 `localhost:7890` 代理；在没有本地代理的环境里，依赖下载可能失败。
- 当前工作区不是 Git 仓库根目录，提交、分支、diff 相关流程要先确认真实仓库位置。
