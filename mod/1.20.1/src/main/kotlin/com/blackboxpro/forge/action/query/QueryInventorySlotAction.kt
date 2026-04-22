package com.blackboxpro.forge.action.query

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.blackboxpro.forge.util.ItemStackSerializer
import com.blackboxpro.forge.util.getIntOrDefault
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft

/**
 * 璇诲彇鑳屽寘鎸囧畾妲戒綅鐗╁搧鏁版嵁銆?
 * Action ID: "query_inventory_slot"
 */
class QueryInventorySlotAction : ActionExecutor {

    override fun execute(params: JsonObject): ActionResult {
        val client = Minecraft.getInstance()
        val player = client.player
            ?: return ActionResult.fail("Player not available")

        val inventory = player.inventory
        val slot = params.getIntOrDefault("slot", -1).let {
            if (it == -1) inventory.selected else it
        }

        if (slot < 0 || slot > 40) {
            return ActionResult.fail("Invalid slot: $slot (expected 0-40)")
        }

        val stack = inventory.getItem(slot)
        val data = ItemStackSerializer.serialize(stack).apply {
            addProperty("slot", slot)
        }

        return ActionResult.ok("Inventory slot $slot queried", data)
    }
}

