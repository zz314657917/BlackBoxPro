package com.blackboxpro.forge.action.query

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.blackboxpro.forge.util.ItemStackSerializer
import com.blackboxpro.forge.util.getIntOrDefault
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft

class QueryInventorySlotAction : ActionExecutor {

    override fun execute(params: JsonObject): ActionResult {
        val mc = Minecraft.getMinecraft()
        val player = mc.player
            ?: return ActionResult.fail("Player not available")

        val inventory = player.inventory
        val slot = params.getIntOrDefault("slot", inventory.currentItem)

        if (slot < 0 || slot > 40) {
            return ActionResult.fail("Invalid slot: $slot (expected 0-40)")
        }

        val stack = inventory.getStackInSlot(slot)
        val data = ItemStackSerializer.serialize(stack).apply {
            addProperty("slot", slot)
        }

        return ActionResult.ok("Inventory slot $slot queried", data)
    }
}
