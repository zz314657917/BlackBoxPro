package com.blackboxpro.neoforge.action.client

import com.blackboxpro.neoforge.action.ActionExecutor
import com.blackboxpro.neoforge.action.ActionResult
import com.blackboxpro.neoforge.util.ScreenMouseHelper
import com.blackboxpro.runtime.util.RuntimeJsonUtil
import com.google.gson.JsonObject

class MoveMouseAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val x = RuntimeJsonUtil.requireDouble(params, "x")
        val y = RuntimeJsonUtil.requireDouble(params, "y")
        val state = ScreenMouseHelper.moveMouse(x, y)
        val data = state.toJson().apply {
            addProperty("x", x)
            addProperty("y", y)
        }
        return ActionResult.ok("Moved mouse to ($x, $y)", data)
    }
}
