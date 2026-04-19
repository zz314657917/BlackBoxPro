package com.blackboxpro.common.action.composite

import com.blackboxpro.common.runtime.action.ActionExecutor
import com.blackboxpro.common.runtime.action.ActionResult
import com.blackboxpro.common.runtime.config.RuntimeBlackBoxConfig
import com.google.gson.JsonObject

class WaitAction : ActionExecutor {

    override fun execute(params: JsonObject): ActionResult {
        val maxDelay = RuntimeBlackBoxConfig.current.execution.maxDelayTicks
        val ticks = (params.get("ticks")?.asInt ?: 0).coerceIn(0, maxDelay)
        return ActionResult.ok("Wait $ticks ticks (${ticks * 50}ms). Note: only effective inside batch actions.")
    }
}
