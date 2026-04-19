package com.blackboxpro.forge.action.container

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.blackboxpro.forge.util.requireInt
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.network.play.client.CPacketHeldItemChange

class SetCarriedItemAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val slot = params.requireInt("slot")

        if (slot !in 0..8) {
            return ActionResult.fail("Slot must be between 0 and 8, got: $slot")
        }

        val mc = Minecraft.getMinecraft()
        val connection = mc.connection
            ?: return ActionResult.fail("Not connected to server")

        mc.player?.inventory?.currentItem = slot
        connection.sendPacket(CPacketHeldItemChange(slot))
        return ActionResult.ok("Selected hotbar slot $slot")
    }
}
