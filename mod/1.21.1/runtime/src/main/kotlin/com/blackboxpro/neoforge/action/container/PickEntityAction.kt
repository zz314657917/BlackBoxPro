package com.blackboxpro.neoforge.action.container

import com.blackboxpro.neoforge.action.ActionExecutor
import com.blackboxpro.neoforge.action.ActionResult
import com.google.gson.JsonObject

class PickEntityAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult = ActionResult.fail("Pick entity action is not supported in 1.21.1 runtime")
}
