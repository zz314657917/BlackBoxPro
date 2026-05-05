package com.blackboxpro.forge.action.client

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.blackboxpro.forge.util.ScreenMouseHelper
import com.blackboxpro.forge.util.getIntOrDefault
import com.google.gson.JsonObject

class ClickMouseAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val button = params.getIntOrDefault("button", 0)
        val clickCount = params.getIntOrDefault("clickCount", 1)
        val state = ScreenMouseHelper.clickMouse(button, clickCount)
        val data = state.toJson().apply {
            addProperty("button", button)
            addProperty("clickCount", clickCount.coerceAtLeast(1))
        }
        return ActionResult.ok("Clicked mouse button $button x${clickCount.coerceAtLeast(1)}", data)
    }
}
