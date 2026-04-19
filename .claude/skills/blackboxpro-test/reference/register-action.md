# 第三方 Mod 注册自定义测试 Action

本文档面向**第三方 mod 开发者**：你的 mod 以 BlackBoxPro 为前置依赖，希望将自己的业务 Action 注册到 BBP 的执行引擎中，通过 HTTP API 调用。

## 架构概览

```
BlackBoxPro (前置 mod)
├── common                    ← ActionExecutor 接口 + RuntimeActionRegistry 实现
├── mod/{version}/{loader}    ← 各平台 ActionRegistry.registerExternal() 公开 API
└── HTTP Server (:38081)      ← 自动路由到已注册的 action

你的 Mod (依赖 BBP)
├── 实现 ActionExecutor
└── 在初始化时调用 ActionRegistry.registerExternal("your_action", YourAction())
```

核心：`registerExternal()` 绕过 BBP 的 frozen 保护，允许第三方在 BBP 初始化完成后追加 action。

## 快速开始

### 1. 声明前置依赖

**Fabric (fabric.mod.json)**：
```json
{
  "depends": {
    "blackboxpro": "*"
  }
}
```

**NeoForge (mods.toml)**：
```toml
[[dependencies.yourmod]]
modId = "blackboxpro"
mandatory = true
```

**Forge 1.12.2 (@Mod)**：
```kotlin
@Mod(modid = "yourmod", dependencies = "required-after:blackboxpro")
```

> 声明 `required-after` / `depends` 确保 BBP 先于你的 mod 初始化。

### 2. 实现 ActionExecutor

```kotlin
package com.example.yourmod.action

import com.blackboxpro.common.runtime.action.ActionExecutor
import com.blackboxpro.common.runtime.action.ActionResult
import com.google.gson.JsonObject

class MyBusinessAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val target = params.get("target")?.asString
            ?: return ActionResult.fail("Missing required field: target")

        // 你的业务逻辑
        val result = doSomething(target)

        return ActionResult.ok(
            message = "Business action completed",
            data = JsonObject().apply {
                addProperty("result", result)
            }
        )
    }
}
```

### 3. 注册到 BBP

在你的 mod 初始化时调用 `registerExternal`：

**Fabric**：
```kotlin
import com.blackboxpro.fabric.dispatcher.ActionRegistry

object YourMod : ClientModInitializer {
    override fun onInitializeClient() {
        // BBP 已先于你的 mod 初始化（fabric.mod.json depends 保证）
        ActionRegistry.registerExternal("my_business_action", MyBusinessAction())
        ActionRegistry.registerExternal("my_query_action", MyQueryAction())
    }
}
```

**NeoForge**：
```kotlin
import com.blackboxpro.neoforge.dispatcher.ActionRegistry

@Mod("yourmod")
class YourMod {
    init {
        ActionRegistry.registerExternal("my_business_action", MyBusinessAction())
    }
}
```

**Forge 1.12.2**：
```kotlin
import com.blackboxpro.forge.dispatcher.ActionRegistry

@Mod(modid = "yourmod", dependencies = "required-after:blackboxpro")
object YourMod {
    @Mod.EventHandler
    fun init(event: FMLInitializationEvent) {
        ActionRegistry.registerExternal("my_business_action", MyBusinessAction())
    }
}
```

### 4. 通过 HTTP 调用

注册后立即可用：

```bash
curl -s --max-time 8 -X POST http://localhost:38081/execute \
  -H "Content-Type: application/json" \
  -d '{"id":"biz1","action":"my_business_action","params":{"target":"hello"}}'
```

## API 参考

### ActionExecutor 接口

```kotlin
// 位于 common 模块：com.blackboxpro.common.runtime.action
interface ActionExecutor {
    fun execute(params: JsonObject): ActionResult
    // 异步版本（可选重写）
    fun execute(params: JsonObject, commandId: String): ActionResult = execute(params)
}
```

### ActionResult

```kotlin
data class ActionResult(
    val success: Boolean,
    val message: String? = null,
    val data: JsonObject? = null,
    val async: Boolean = false
) {
    companion object {
        fun ok(message: String? = null, data: JsonObject? = null): ActionResult
        fun fail(message: String): ActionResult
        fun async(): ActionResult  // 异步模式，需自行通过 RuntimeResponseSender 发送响应
    }
}
```

### registerExternal

```kotlin
// 各平台 ActionRegistry 均提供，内部委托给 common 的 RuntimeActionRegistry
fun registerExternal(actionId: String, executor: ActionExecutor)
```

- 可在 BBP frozen 后调用（即你的 mod 初始化时）
- 注册后立即生效，HTTP 请求可路由到你的 action
- actionId 冲突时会覆盖已有 executor（日志会 warn）

### 参数读取工具

BBP 各平台模块提供了 JsonObject 扩展函数（包路径 `com.blackboxpro.{loader}.util`）：

```kotlin
params.requireString("key")              // 必填，缺失抛异常
params.getStringOrNull("key")            // 可选
params.getIntOrDefault("key", 0)         // 带默认值
params.getBooleanOrDefault("key", false)
params.getDoubleOrDefault("key", 0.0)
```

## 异步 Action

长耗时操作使用异步模式，BBP 不自动发响应，由你的代码控制：

```kotlin
import com.blackboxpro.common.runtime.dispatcher.RuntimeResponseSender

class MyAsyncAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult =
        ActionResult.fail("Requires commandId")

    override fun execute(params: JsonObject, commandId: String): ActionResult {
        Thread {
            val result = longRunningTask()
            RuntimeResponseSender.sendResponse(
                id = commandId,
                status = if (result) "success" else "failure",
                message = "Async task done"
            )
        }.start()
        return ActionResult.async()
    }
}
```

## 线程安全

- `execute()` 在 BBP 的 HTTP 线程池中被调用
- 操作 Minecraft 状态必须调度到客户端主线程：
  - Fabric: `MinecraftClient.getInstance().execute { }`
  - NeoForge: `Minecraft.getInstance().execute { }`
  - Forge 1.12.2: `Minecraft.getMinecraft().addScheduledTask { }`

## 注意事项

- action ID 建议使用 `yourmod_` 前缀避免冲突（如 `mymod_check_quest`）
- 不要在 action 中崩溃——异常会导致 HTTP 500，不会影响 BBP 其他功能
- 你的 action 不会出现在 BBP 内置的 `run_test` 全量测试中（仅通过 HTTP 手动调用）
