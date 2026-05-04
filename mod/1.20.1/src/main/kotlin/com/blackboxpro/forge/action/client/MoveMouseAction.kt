package com.blackboxpro.forge.action.client

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.blackboxpro.forge.util.ScreenMouseHelper
import com.blackboxpro.forge.util.requireDouble
import com.google.gson.JsonObject

class MoveMouseAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val x = params.requireDouble("x")
        val y = params.requireDouble("y")
        val state = ScreenMouseHelper.moveMouse(x, y)
        val data = state.toJson().apply {
            addProperty("x", x)
            addProperty("y", y)
        }
        return ActionResult.ok("Moved mouse to ($x, $y)", data)
    }
}
