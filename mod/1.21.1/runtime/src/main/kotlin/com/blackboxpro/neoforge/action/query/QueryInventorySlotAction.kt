package com.blackboxpro.neoforge.action.query

import com.blackboxpro.neoforge.action.ActionExecutor
import com.blackboxpro.neoforge.action.ActionResult
import com.blackboxpro.neoforge.util.ItemStackSerializer
import com.blackboxpro.neoforge.util.getIntOrDefault
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft

/**
 * 读取背包指定槽位物品数据。
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
