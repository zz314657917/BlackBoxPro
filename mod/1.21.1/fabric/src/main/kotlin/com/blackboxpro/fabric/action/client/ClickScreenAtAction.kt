package com.blackboxpro.fabric.action.client

import com.blackboxpro.fabric.action.ActionExecutor
import com.blackboxpro.fabric.action.ActionResult
import com.blackboxpro.fabric.util.ScreenMouseHelper
import com.blackboxpro.runtime.util.RuntimeJsonUtil
import com.google.gson.JsonObject

class ClickScreenAtAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val x = RuntimeJsonUtil.requireDouble(params, "x")
        val y = RuntimeJsonUtil.requireDouble(params, "y")
        val button = RuntimeJsonUtil.getIntOrDefault(params, "button", 0)
        val clickCount = RuntimeJsonUtil.getIntOrDefault(params, "clickCount", 1)
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
