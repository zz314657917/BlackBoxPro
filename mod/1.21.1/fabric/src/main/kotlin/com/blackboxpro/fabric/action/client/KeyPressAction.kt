package com.blackboxpro.fabric.action.client

import com.blackboxpro.fabric.action.ActionExecutor
import com.blackboxpro.fabric.action.ActionResult
import com.blackboxpro.fabric.util.ScreenKeyboardHelper
import com.blackboxpro.runtime.util.RuntimeJsonUtil
import com.google.gson.JsonObject

class KeyPressAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val key = RuntimeJsonUtil.getStringOrNull(params, "key")
        val keyCode = if (params.has("keyCode") && !params.get("keyCode").isJsonNull) {
            params.get("keyCode").asInt
        } else {
            null
        }
        val char = RuntimeJsonUtil.getStringOrNull(params, "char")
        val pressTicks = RuntimeJsonUtil.getIntOrDefault(params, "pressTicks", 1)

        val state = ScreenKeyboardHelper.pressKey(key, keyCode, char, pressTicks)
        return ActionResult.ok("Pressed key ${key ?: keyCode ?: char}", state.toJson())
    }
}
