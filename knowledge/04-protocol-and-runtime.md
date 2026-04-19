# BlackBoxPro 协议与运行时

## 当前真实通信主链

### 插件到客户端

1. 插件调用 `BlackBoxApi.sendAsync(...)`
2. `BlackBoxApi` 组装 `CommandMessage`
3. `ModRelayClient.forward(...)` 把 JSON 发到客户端 `POST /execute`
4. 客户端 `ExecuteHandler` 注册等待中的 `ResponseFutureRegistry`
5. 客户端 `CommandDispatcher` 把命令交给共享运行时
6. `RuntimeCommandDispatcher` 在主线程执行具体 action
7. 结果经 `RuntimeResponseSender` 回到 HTTP 响应

### 插件本地特判

插件侧 `ExecuteHandler` 里有两个本地特判 action：

- `run_test`
  直接触发插件测试框架，不会转发给客户端。
- `stop_server`
  直接在 Bukkit 控制台执行 `stop`。

## 协议模型

### 指令

`common/src/main/kotlin/com/blackboxpro/common/protocol/CommandMessage.kt`

字段：

- `id`
- `action`
- `params`
- `delay`
- `target`

说明：

- `target` 主要由插件侧填充为玩家名。
- `delay` 单位是毫秒，但共享运行时会换算成 tick，并受 `maxDelayTicks` 限制。

### 响应

`common/src/main/kotlin/com/blackboxpro/common/protocol/ResponseMessage.kt`

字段：

- `id`
- `status`
- `message`
- `data`

约定：

- 成功状态用 `success`
- 失败状态用 `failure`

## HTTP 端点

`common/src/main/kotlin/com/blackboxpro/common/protocol/HttpEndpoints.kt`

- `POST /execute`
- `GET /status`
- `/actions` 常量已定义，但当前未实现

## 插件 HTTP 状态返回

插件 `GET /status` 会返回：

- `version`
- `mode`
- `httpPort`
- `modHttpAddress`
- `ready`

这里的 `ready` 只是插件侧 HTTP 服务可用，不代表客户端一定在线。

## 客户端 HTTP 状态返回

客户端 `GET /status` 会返回：

- `status`
- `version`
- `platform`
- `httpPort`
- `actions`
- `ready`

这里的 `ready` 是基于 `client.player != null && world/level != null` 的就绪状态。

## 共享运行时默认配置

`RuntimeBlackBoxConfig.current` 默认快照包含：

- 网络：`httpPort=38081`、`responseTimeoutMs=10000`
- 执行：`maxDelayTicks=6000`、`maxBatchSize=100`、`defaultBreakTicks=20`
- 安全：默认启用，但 `allowedActions` 为空、`blockedActions` 为空，因此实际默认放行全部 action
- 截图：`screenshots/blackboxpro`
- 寻路与导航：最大距离、步长、阈值等默认值

已确认现状：

- 当前仓库中没有找到 `RuntimeBlackBoxConfig.update(...)` 的调用。
- 这意味着客户端默认使用代码内置快照，而不是外部配置文件覆盖后的值。

## 调度与安全

### 延迟执行

- `delay > 0` 的 action 会进入 `RuntimeCommandDispatcher` 的延迟队列。
- 延迟值会从毫秒换算成 tick。

### 主线程执行

- 共享运行时最终都会通过各 Loader 的主线程执行器回到 Minecraft 主线程。
- 现代端通过 `RuntimeCommandDispatcherBootstrap` 注入执行器。
- `1.12.2` 端直接绑定 `Minecraft.getMinecraft().addScheduledTask(...)`。

### 安全规则

- `blockedActions` 命中则直接失败。
- `allowedActions` 非空时，只允许白名单里的 action。
- `batch` 内子 action 也会复用同一套安全规则。

## 截图动作的特殊性

- `ScreenshotAction` 是异步 action。
- 截图成功时会回传：
  - `filePath`
  - `width`
  - `height`
  - `fileSize`
  - `index`
- 文件目录结构默认是：
  `screenshots/blackboxpro/<playerName>/<testId>/`
