package com.blackboxpro.neoforge.action.container

import com.blackboxpro.neoforge.action.ActionExecutor
import com.blackboxpro.neoforge.action.ActionResult
import com.google.gson.JsonObject

class PickItemAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult = ActionResult.fail("Pick item action is not supported in 1.21.1 runtime")
}
