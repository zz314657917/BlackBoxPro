package com.blackboxpro.forge.action.client

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.blackboxpro.forge.util.ScreenMouseHelper
import com.blackboxpro.forge.util.getIntOrDefault
import com.blackboxpro.forge.util.requireDouble
import com.google.gson.JsonObject

class ClickScreenAtAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val x = params.requireDouble("x")
        val y = params.requireDouble("y")
        val button = params.getIntOrDefault("button", 0)
        val clickCount = params.getIntOrDefault("clickCount", 1)
        val state = ScreenMouseHelper.clickScreenAt(x, y, button, clickCount)
        val data = state.toJson().apply {
            addProperty("x", x)
            addProperty("y", y)
            addProperty("button", button)
            addProperty("clickCount", clickCount.coerceAtLeast(1))
        }
        return ActionResult.ok("Clicked screen at ($x, $y) with button $button x${clickCount.coerceAtLeast(1)}", data)
    }
}
