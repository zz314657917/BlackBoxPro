package com.blackboxpro.fabric.action.query

import com.blackboxpro.fabric.action.ActionExecutor
import com.blackboxpro.fabric.action.ActionResult
import com.blackboxpro.fabric.util.ScreenMouseHelper
import com.google.gson.JsonObject

class QueryCursorStateAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val data = ScreenMouseHelper.queryCursorState().toJson()
        return ActionResult.ok("Cursor state queried", data)
    }
}
