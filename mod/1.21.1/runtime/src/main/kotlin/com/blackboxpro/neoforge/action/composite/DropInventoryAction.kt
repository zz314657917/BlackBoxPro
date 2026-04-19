package com.blackboxpro.neoforge.action.composite

import com.blackboxpro.neoforge.action.ActionExecutor
import com.blackboxpro.neoforge.action.ActionResult
import com.blackboxpro.neoforge.dispatcher.ActionRegistry
import com.blackboxpro.neoforge.util.requireInt
import com.google.gson.JsonObject

class DropInventoryAction : ActionExecutor {

    override fun execute(params: JsonObject): ActionResult {
        val slot = params.requireInt("slot")

        val setCarried = ActionRegistry.find("set_carried_item")
            ?: return ActionResult.fail("set_carried_item action not registered")
        val setResult = setCarried.execute(JsonObject().apply {
            addProperty("slot", slot)
        })
        if (!setResult.success) {
            return ActionResult.fail("Failed to select slot $slot: ${setResult.message}")
        }

        val drop = ActionRegistry.find("drop_item")
            ?: return ActionResult.fail("drop_item action not registered")
        val dropResult = drop.execute(JsonObject())
        if (!dropResult.success) {
            return ActionResult.fail("Failed to drop item: ${dropResult.message}")
        }

        return ActionResult.ok("Dropped item from slot $slot")
    }
}
