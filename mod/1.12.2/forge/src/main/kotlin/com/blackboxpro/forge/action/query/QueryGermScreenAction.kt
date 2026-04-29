package com.blackboxpro.forge.action.query

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.blackboxpro.forge.util.GermScreenProbeHelper
import com.blackboxpro.forge.util.getBooleanOrDefault
import com.blackboxpro.forge.util.getIntOrDefault
import com.google.gson.JsonObject

class QueryGermScreenAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val options = GermScreenProbeHelper.ProbeOptions(
            maxDepth = params.getIntOrDefault("maxDepth", 4),
            maxComponents = params.getIntOrDefault("maxComponents", 200),
            includeFields = params.getBooleanOrDefault("includeFields", false)
        )
        val data = GermScreenProbeHelper.query(options)
        return ActionResult.ok("Germ screen queried", data)
    }
}
