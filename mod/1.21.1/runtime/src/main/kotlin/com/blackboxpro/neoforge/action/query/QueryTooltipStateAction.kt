package com.blackboxpro.neoforge.action.query

import com.blackboxpro.neoforge.action.ActionExecutor
import com.blackboxpro.neoforge.action.ActionResult
import com.blackboxpro.neoforge.util.ContainerTooltipHelper
import com.blackboxpro.neoforge.util.getBooleanOrDefault
import com.google.gson.JsonObject

class QueryTooltipStateAction : ActionExecutor {

    override fun execute(params: JsonObject): ActionResult {
        val advanced = params.getBooleanOrDefault("advanced", false)
        val data = ContainerTooltipHelper.queryCurrentTooltip(advanced).toQueryJson()
        return ActionResult.ok("Tooltip state queried", data)
    }
}
