package com.blackboxpro.forge.action.client

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.blackboxpro.forge.util.GermScreenProbeHelper
import com.blackboxpro.forge.util.getBooleanOrDefault
import com.blackboxpro.forge.util.getDoubleOrDefault
import com.blackboxpro.forge.util.getIntOrDefault
import com.blackboxpro.forge.util.getStringOrNull
import com.google.gson.JsonObject

class ClickGermComponentAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val options = GermScreenProbeHelper.ClickOptions(
            maxDepth = params.getIntOrDefault("maxDepth", 4),
            maxComponents = params.getIntOrDefault("maxComponents", 200),
            includeFields = params.getBooleanOrDefault("includeFields", false),
            hitX = params.getDoubleOrDefault("x", Double.NaN).takeUnless { it.isNaN() },
            hitY = params.getDoubleOrDefault("y", Double.NaN).takeUnless { it.isNaN() },
            componentId = params.getStringOrNull("componentId"),
            button = params.getIntOrDefault("button", 0),
            clickCount = params.getIntOrDefault("clickCount", 1),
            fallbackScreenClick = params.getBooleanOrDefault("fallbackScreenClick", false)
        )
        val data = GermScreenProbeHelper.click(options)
        val x = options.hitX?.toString() ?: "cursor"
        val y = options.hitY?.toString() ?: "cursor"
        return ActionResult.ok("Clicked Germ component at ($x, $y)", data)
    }
}
