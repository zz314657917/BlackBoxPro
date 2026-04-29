package com.blackboxpro.forge.action.client

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.blackboxpro.forge.util.ScreenKeyboardHelper
import com.blackboxpro.forge.util.getIntOrDefault
import com.blackboxpro.forge.util.getStringOrNull
import com.google.gson.JsonObject

class KeyPressAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val key = params.getStringOrNull("key")
        val keyCode = if (params.has("keyCode") && !params.get("keyCode").isJsonNull) {
            params.get("keyCode").asInt
        } else {
            null
        }
        val char = params.getStringOrNull("char")
        val pressTicks = params.getIntOrDefault("pressTicks", 1)

        val state = ScreenKeyboardHelper.pressKey(key, keyCode, char, pressTicks)
        return ActionResult.ok("Pressed key ${key ?: keyCode ?: char}", state.toJson())
    }
}
