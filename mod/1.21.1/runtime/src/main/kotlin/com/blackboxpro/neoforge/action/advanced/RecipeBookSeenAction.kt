package com.blackboxpro.neoforge.action.advanced

import com.blackboxpro.neoforge.action.ActionExecutor
import com.blackboxpro.neoforge.action.ActionResult
import com.google.gson.JsonObject

class RecipeBookSeenAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult =
        ActionResult.fail("Recipe book seen action is not supported in 1.21.1 runtime")
}
