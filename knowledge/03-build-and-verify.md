# BlackBoxPro 构建与验证

## 构建前提

- 现代端与根构建使用 JDK 21。
- `1.12.2` 模块的 `Gradle` 运行 JVM 需要 17 或 21，但编译目标仍是 Java 8。
- 根、插件、`1.21.1`、`1.12.2` 的 `gradle.properties` 都设置了 `localhost:7890` 代理；无代理环境可能需要先处理依赖下载问题。

## 根项目常用任务

### 推荐入口

- `.\gradlew buildAll`
  构建 `common`、`1.21.11`、`1.21.1`、`1.12.2` 和 `plugin`，并把产物收集到根 `build/libs/`。
- `.\gradlew plugin_build`
  单独构建服务端插件。
- `.\gradlew forge1122_build`
  单独构建 `1.12.2` 独立工程。

### 现代端分开构建

- `.\gradlew mod2111_build`
  构建 `1.21.11 runtime/fabric/neoforge`。
- `.\gradlew mod1211_build`
  构建 `1.21.1 runtime/fabric/neoforge` 的 class/resources。
- `.\gradlew mod1211_pack_neoforge`
  额外打包 `1.21.1` NeoForge JAR；根 `buildAll` 实际依赖的是这个任务。

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
- Fabric 1.21.1：`mod/1.21.1/fabric/build/libs/BlackBoxPro-fabric-1.21.1-<version>.jar`
- NeoForge 1.21.1：`mod/1.21.1/neoforge/build/libs/BlackBoxPro-neoforge-1.21.1-<version>.jar`
- Forge 1.12.2：`mod/1.12.2/forge/build/libs/BlackBoxPro-forge-1.12.2-<version>.jar`

## 运行时端口

- 插件 HTTP：默认 `38080`
- 客户端 Mod HTTP：默认 `38081`
- Minecraft 服务器端口由实际服务端决定，历史文档里常见 `25565`

## 最短验证路径

### 插件

1. 构建 `plugin_build`
2. 启动服务端并加载插件
3. 执行 `/blackbox status`
4. 请求 `GET http://localhost:38080/status`

### 客户端 Mod

1. 启动对应客户端
2. 请求 `GET http://localhost:38081/status`
3. 观察 `ready` 字段和 `actions` 数量
4. 用 `POST /execute` 发送一个低风险查询 action，例如 `query_player_state`

### 插件中继链路

1. 插件配置 `test-mode: dual`
2. `mod-http-address` 指向在线客户端
3. 调 `POST http://localhost:38080/execute`
4. 观察插件是否转发到 Mod 并拿到响应

## 已确认但尚未实现的接口

- `common/.../HttpEndpoints.kt` 里定义了 `/actions`
- 当前插件与客户端 HTTP Server 都没有注册这个路由
- 如果后续要做动作枚举接口，需要同时补服务端与客户端实现
