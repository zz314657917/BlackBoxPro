package com.blackboxpro.forge.action.query

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.blackboxpro.forge.util.ContainerTooltipHelper
import com.blackboxpro.forge.util.getBooleanOrDefault
import com.google.gson.JsonObject

class QueryTooltipStateAction : ActionExecutor {

    override fun execute(params: JsonObject): ActionResult {
        val advanced = params.getBooleanOrDefault("advanced", false)
        val data = ContainerTooltipHelper.queryCurrentTooltip(advanced).toQueryJson()
        return ActionResult.ok("Tooltip state queried", data)
    }
}
