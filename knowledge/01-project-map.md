# BlackBoxPro 项目地图

## 顶层目录

- `common/`
  共享协议层、共享 action 清单、共享运行时调度与截图桥接，不依赖具体 Minecraft Loader。
- `mod/`
  现代客户端多版本工程入口，包含 `1.21.11`、`1.21.1`，并额外管理 `1.12.2` 独立工程的聚合构建。
- `plugin/`
  Bukkit/Paper 服务端插件，使用 TabooLib，负责管理命令、测试框架和 HTTP 中继。
- `docs/`
  历史设计文档、测试计划、测试报告。适合补背景，不适合直接当当前实现真源。
- `knowledge/`
  当前仓库知识库入口。

## 模块关系

### `common`

- 提供 `CommandMessage`、`ResponseMessage`、`HttpEndpoints`。
- 提供 `ActionCatalog`，由插件命令补全、测试框架和客户端实现共同依赖。
- 提供 `RuntimeCommandDispatcher`、`RuntimeResponseSender`、`RuntimeTickScheduler` 等共享运行时组件。
- 提供 `RuntimeBlackBoxConfig` 默认快照。

### `mod/1.21.11`

- `runtime/`
  现代端共享运行时核心，基于 NeoForm，承载 `com.blackboxpro.runtime` 下的共享桥接与 NeoForge 公共实现。
- `fabric/`
  Fabric 1.21.11 包装层。通过 `srcDir(runtimeSharedDir)` 直接把 `runtime` 的共享源码并入 Fabric 主源码集。
- `neoforge/`
  NeoForge 1.21.11 包装层。运行和打包时同时装入本地 source set、`runtime` source set 和 `common` source set。

### `mod/1.21.1`

- 仍保留完整的 `runtime/fabric/neoforge` 三段结构。
- 这一线是旧现代端兼容线，Gradle 属性来自 `mod/1.21.1/gradle.properties`，不走根目录的 `minecraft_version=1.21.11`。
- `1.21.1` NeoForge 产物由根构建任务 `mod1211_pack_neoforge` 额外手工打包。

### `mod/1.12.2`

- 这是独立 Gradle 根，不在根 `settings.gradle.kts` 中。
- 内部再分 `runtime/` 与 `forge/`。
- 构建入口是 `mod/1.12.2/build.gradle.kts`，根项目通过 `forge1122_build` 代调用。

### `plugin`

- 独立 Gradle 工程，`includeBuild("../common")` 组合依赖 `common`。
- 入口类是 `com.blackboxpro.plugin.BlackBoxPro`。
- 对外动作入口主要有三层：
  - `api/BlackBoxApi.kt`
  - `command/BlackBoxCommand.kt`
  - `http/BlackBoxHttpServer.kt`

## 当前代码里的真实入口

### 服务端插件

- 生命周期入口：`plugin/src/main/kotlin/com/blackboxpro/plugin/BlackBoxPro.kt`
- 配置入口：`plugin/src/main/kotlin/com/blackboxpro/plugin/config/BlackBoxSettings.kt`
- HTTP 服务：`plugin/src/main/kotlin/com/blackboxpro/plugin/http/BlackBoxHttpServer.kt`
- HTTP 中继：`plugin/src/main/kotlin/com/blackboxpro/plugin/http/ModRelayClient.kt`
- 命令入口：`plugin/src/main/kotlin/com/blackboxpro/plugin/command/BlackBoxCommand.kt`
- 测试框架：`plugin/src/main/kotlin/com/blackboxpro/plugin/command/testframework/`

### 现代客户端

- Fabric 1.21.11 入口：`mod/1.21.11/fabric/src/main/kotlin/com/blackboxpro/fabric/BlackBoxProFabric.kt`
- NeoForge 1.21.11 入口：`mod/1.21.11/neoforge/src/main/kotlin/com/blackboxpro/neoforge/BlackBoxProNeoForge.kt`
- Fabric 1.21.1 入口：`mod/1.21.1/fabric/src/main/kotlin/com/blackboxpro/fabric/BlackBoxProFabric.kt`
- NeoForge 1.21.1 入口：`mod/1.21.1/neoforge/src/main/kotlin/com/blackboxpro/neoforge/BlackBoxProNeoForge.kt`
- 共享调度桥：`mod/*/runtime/src/main/kotlin/com/blackboxpro/runtime/dispatcher/RuntimeCommandDispatcherBootstrap.kt`

### Forge 1.12.2 客户端

- 入口：`mod/1.12.2/forge/src/main/kotlin/com/blackboxpro/forge/BlackBoxProForge.kt`
- 1.12.2 直接绑定 `RuntimeCommandDispatcher` 与 `RuntimeResponseSender`，没有现代端那层 bootstrap 包装。

## 源码真源清单

- 版本与现代端默认依赖：根 `gradle.properties`
- `1.21.1` 依赖版本：`mod/1.21.1/gradle.properties`
- 插件配置字段：`plugin/src/main/resources/config.yml`
- Action ID 与参数：`common/.../ActionCatalog.kt`
- HTTP 路由常量：`common/.../HttpEndpoints.kt`
- 插件测试分类与跳过规则：`plugin/.../BlackBoxTestCatalog.kt`

## 目录层面的注意点

- 仓库里没有 `plugin/channel/` 这类当前实现目录，旧开发文档提到的 Plugin Message Channel 服务端目录已经不是现状。
- 现代端和 1.12.2 端都大量保留同名 action，但实现路径、API 映射和打包方式不同。
- `docs/testing/` 中很多文件来自旧的 macOS 测试环境，路径、端口、产物名和当前仓库未必一致。
