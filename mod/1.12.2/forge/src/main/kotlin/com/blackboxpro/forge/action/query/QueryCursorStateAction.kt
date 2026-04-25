package com.blackboxpro.forge.action.query

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.blackboxpro.forge.util.ScreenMouseHelper
import com.google.gson.JsonObject

class QueryCursorStateAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val data = ScreenMouseHelper.queryCursorState().toJson()
        return ActionResult.ok("Cursor state queried", data)
    }
}
