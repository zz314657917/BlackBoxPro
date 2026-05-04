package com.blackboxpro.neoforge.action.query

import com.blackboxpro.neoforge.action.ActionExecutor
import com.blackboxpro.neoforge.action.ActionResult
import com.blackboxpro.neoforge.util.ScreenMouseHelper
import com.google.gson.JsonObject

class QueryCursorStateAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val data = ScreenMouseHelper.queryCursorState().toJson()
        return ActionResult.ok("Cursor state queried", data)
    }
}
