package com.blackboxpro.forge.action.container

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.blackboxpro.common.runtime.dispatcher.RuntimeResponseSender
import com.blackboxpro.common.runtime.scheduler.RuntimeTickScheduler
import com.blackboxpro.forge.util.ContainerTooltipHelper
import com.blackboxpro.forge.util.requireInt
import com.google.gson.JsonObject

class HoverSlotAction : ActionExecutor {

    override fun execute(params: JsonObject): ActionResult = execute(params, "")

    override fun execute(params: JsonObject, commandId: String): ActionResult {
        val windowId = params.requireInt("windowId")
        val slotIndex = params.requireInt("slot")
        val durationTicks = params.get("durationTicks")?.asInt ?: 0

        return runCatching {
            val hoverState = ContainerTooltipHelper.hoverSlot(windowId, slotIndex)
            val data = hoverState.toQueryJson().apply {
                addProperty("tooltipVisible", hoverState.visible)
                addProperty("durationTicks", durationTicks)
            }

            if (durationTicks > 0 && commandId.isNotEmpty()) {
                RuntimeTickScheduler.schedule(durationTicks) {
                    RuntimeResponseSender.sendResponse(commandId, "success", "Hovered slot $slotIndex in window $windowId", data)
                }
                ActionResult.async()
            } else {
                ActionResult.ok("Hovered slot $slotIndex in window $windowId", data)
            }
        }.getOrElse {
            ActionResult.fail(it.message ?: "Failed to hover slot $slotIndex in window $windowId")
        }
    }
}
