package com.blackboxpro.forge.action.container

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.blackboxpro.forge.util.requireInt
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.world.item.BundleItem

class BundleSelectedSlotAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val slotId = params.requireInt("slotId")
        val selectedIndex = params.requireInt("selectedIndex")

        val player = Minecraft.getInstance().player
            ?: return ActionResult.fail("Player not available")
        val menu = player.containerMenu
        if (slotId !in 0 until menu.slots.size) {
            return ActionResult.fail("Slot index $slotId out of range [0, ${menu.slots.size})")
        }

        val stack = menu.slots[slotId].item
        if (stack.item !is BundleItem) {
            return ActionResult.fail("Slot $slotId does not contain a bundle")
        }

        return ActionResult.ok(
            "Bundle slot selection is client-local in Forge 1.20.1; no packet sent (slot=$slotId, selectedIndex=$selectedIndex)"
        )
    }
}

