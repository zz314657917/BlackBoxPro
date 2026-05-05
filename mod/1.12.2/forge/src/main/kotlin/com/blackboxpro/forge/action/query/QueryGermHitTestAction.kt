package com.blackboxpro.forge.action.query

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.blackboxpro.forge.util.GermScreenProbeHelper
import com.blackboxpro.forge.util.getBooleanOrDefault
import com.blackboxpro.forge.util.getDoubleOrDefault
import com.blackboxpro.forge.util.getIntOrDefault
import com.google.gson.JsonObject

class QueryGermHitTestAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val options = GermScreenProbeHelper.ProbeOptions(
            maxDepth = params.getIntOrDefault("maxDepth", 4),
            maxComponents = params.getIntOrDefault("maxComponents", 200),
            includeFields = params.getBooleanOrDefault("includeFields", false),
            hitX = params.getDoubleOrDefault("x", Double.NaN).takeUnless { it.isNaN() },
            hitY = params.getDoubleOrDefault("y", Double.NaN).takeUnless { it.isNaN() }
        )
        val data = GermScreenProbeHelper.hitTest(options)
        return ActionResult.ok("Germ hit-test queried", data)
    }
}
