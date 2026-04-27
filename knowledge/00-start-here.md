# BlackBoxPro 知识库入口

## 项目一句话

BlackBoxPro 是一个 Minecraft 自动化黑盒测试框架。服务端插件负责发起动作和测试，客户端 Mod 在真实客户端环境中执行移动、交互、查询、截图等行为，并返回结构化结果。

## 这套知识库的用途

- 给新会话提供稳定入口，避免每次都从零扫仓库。
- 用“当前代码现状”覆盖 README、历史开发文档和旧测试报告里的漂移信息。
- 把构建、联调、验证和多版本协作规则集中到仓库内，而不是散落在聊天记录里。

## 建议阅读顺序

1. [01-project-map.md](01-project-map.md)
2. [02-dev-rules.md](02-dev-rules.md)
3. [03-build-and-verify.md](03-build-and-verify.md)
4. [04-protocol-and-runtime.md](04-protocol-and-runtime.md)
5. [05-current-focus.md](05-current-focus.md)
6. [06-test-system.md](06-test-system.md)
7. [tasks/current-task.md](tasks/current-task.md)

## 快速事实

- 根版本号当前是 `2.2.4`，来源于根目录 `gradle.properties`。
- 动作 ID 和参数的唯一真源是 `common/src/main/kotlin/com/blackboxpro/common/action/ActionCatalog.kt`。
- 当前 `ActionCatalog` 中登记了 `117` 个 action。
- 现代端同时维护 `1.21.11` 与 `1.21.1` 两条客户端线，兼容线为 `1.12.2` Forge。
- 服务端插件当前主链路是 `BlackBoxApi -> ModRelayClient -> Mod HTTP /execute`，不是旧文档里描述的 Plugin Message Channel 主链路。
- 当前工作区根目录不是 Git 仓库根，不能默认依赖 `git status`、`git diff` 做上下文判断。
- 当前 1.12.2 本地回归默认只走 `scripts/test-cells/`；`cell-01..05` 对应受管 `server-cell-01..05` 目录，实际根路径以本地 `cells.json` 为准。

## 先看哪些代码文件

- 根构建入口：`build.gradle.kts`
- 现代端 Gradle 入口：`mod/settings.gradle.kts`
- 服务端插件入口：`plugin/src/main/kotlin/com/blackboxpro/plugin/BlackBoxPro.kt`
- 插件 API：`plugin/src/main/kotlin/com/blackboxpro/plugin/api/BlackBoxApi.kt`
- 插件命令入口：`plugin/src/main/kotlin/com/blackboxpro/plugin/command/BlackBoxCommand.kt`
- 插件测试框架：`plugin/src/main/kotlin/com/blackboxpro/plugin/command/BlackBoxTestRunner.kt`
- 客户端协议模型：`common/src/main/kotlin/com/blackboxpro/common/protocol/CommandMessage.kt`
- 客户端共享调度：`common/src/main/kotlin/com/blackboxpro/common/runtime/dispatcher/RuntimeCommandDispatcher.kt`
- 运行时默认配置：`common/src/main/kotlin/com/blackboxpro/common/runtime/config/RuntimeBlackBoxConfig.kt`

## 文档与代码的使用原则

- `knowledge/` 里的结论优先以当前代码为准。
- `docs/design/`、`docs/testing/`、`plugin/开发文档-1.1.0.md` 仍有参考价值，但很多内容带有历史版本背景。
- 遇到 README、设计文档、测试文档和代码不一致时，先信代码，再把结论补回知识库。
