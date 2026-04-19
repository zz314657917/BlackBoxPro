package com.blackboxpro.common.runtime.action

import com.google.gson.JsonObject

interface ActionExecutor {
    fun execute(params: JsonObject): ActionResult

    /**
     *带 commandId 的执行入口，供异步 Action 自行发送响应。
     * 默认忽略 commandId，直接委托给 [execute]。
     */
    fun execute(params: JsonObject, commandId: String): ActionResult = execute(params)
}

data class ActionResult(
    val success: Boolean,
    val message: String? = null,
    val data: JsonObject? = null,
    val async: Boolean = false
) {
    companion object {
        fun ok(message: String? = null, data: JsonObject? = null) =
            ActionResult(true, message, data)

        fun fail(message: String) =
            ActionResult(false, message)

        /** 标记该 Action 将自行发送响应，CommandDispatcher 不应再发送 */
        fun async() = ActionResult(success = true, async = true)
    }
}
