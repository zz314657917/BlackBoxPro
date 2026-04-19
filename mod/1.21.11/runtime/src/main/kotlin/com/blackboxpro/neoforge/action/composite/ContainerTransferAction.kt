package com.blackboxpro.neoforge.action.composite

import com.blackboxpro.neoforge.action.ActionExecutor
import com.blackboxpro.neoforge.action.ActionResult
import com.blackboxpro.neoforge.dispatcher.ActionRegistry
import com.blackboxpro.neoforge.util.requireInt
import com.google.gson.JsonObject

class ContainerTransferAction : ActionExecutor {

    override fun execute(params: JsonObject): ActionResult {
        val windowId = params.requireInt("windowId")
        val stateId = params.requireInt("stateId")
        val slot = params.requireInt("slot")

        // Shift-click: mode=1 (QUICK_MOVE), button=0
        val clickSlot = ActionRegistry.find("click_slot")
            ?: return ActionResult.fail("click_slot action not registered")
        val result = clickSlot.execute(JsonObject().apply {
            addProperty("windowId", windowId)
            addProperty("stateId", stateId)
            addProperty("slot", slot)
            addProperty("button", 0)
            addProperty("mode", 1)
        })
        if (!result.success) return ActionResult.fail("Failed to transfer slot $slot: ${result.message}")

        return ActionResult.ok("Transferred slot $slot in window $windowId")
    }
}
