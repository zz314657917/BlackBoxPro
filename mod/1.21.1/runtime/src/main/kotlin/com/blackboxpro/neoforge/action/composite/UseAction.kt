package com.blackboxpro.neoforge.action.composite

import com.blackboxpro.neoforge.action.ActionExecutor
import com.blackboxpro.neoforge.action.ActionResult
import com.blackboxpro.neoforge.dispatcher.ActionRegistry
import com.blackboxpro.neoforge.util.getIntOrDefault
import com.blackboxpro.neoforge.util.getStringOrNull
import com.google.gson.JsonObject

class UseAction : ActionExecutor {

    override fun execute(params: JsonObject): ActionResult {
        val slot = if (params.has("slot")) params.getIntOrDefault("slot", -1) else -1
        val hand = params.getStringOrNull("hand") ?: "main_hand"

        if (slot >= 0) {
            val setCarried = ActionRegistry.find("set_carried_item")
                ?: return ActionResult.fail("set_carried_item action not registered")
            val setResult = setCarried.execute(JsonObject().apply {
                addProperty("slot", slot)
            })
            if (!setResult.success) return ActionResult.fail("Failed to select slot $slot: ${setResult.message}")
        }

        val useItem = ActionRegistry.find("use_item")
            ?: return ActionResult.fail("use_item action not registered")
        val useResult = useItem.execute(JsonObject().apply {
            addProperty("hand", hand)
        })
        if (!useResult.success) return ActionResult.fail("Failed to use item: ${useResult.message}")

        return ActionResult.ok("Used item" + if (slot >= 0) " (slot=$slot)" else "")
    }
}
