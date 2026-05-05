package com.blackboxpro.neoforge.action.client

import com.blackboxpro.neoforge.action.ActionExecutor
import com.blackboxpro.neoforge.action.ActionResult
import com.blackboxpro.neoforge.util.ScreenKeyboardHelper
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
