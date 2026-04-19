package com.blackboxpro.neoforge.action.container

import com.blackboxpro.neoforge.action.ActionExecutor
import com.blackboxpro.neoforge.action.ActionResult
import com.blackboxpro.neoforge.util.requireInt
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket
import org.tabooproject.reflex.Reflex.Companion.setProperty

class SetCarriedItemAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val slot = params.requireInt("slot")

        if (slot !in 0..8) {
            return ActionResult.fail("Slot must be between 0 and 8, got: $slot")
        }

        val handler = Minecraft.getInstance().connection
            ?: return ActionResult.fail("Not connected to server")
        val player = Minecraft.getInstance().player

        player?.inventory?.setProperty("selected", slot)
        handler.send(ServerboundSetCarriedItemPacket(slot))
        return ActionResult.ok("Selected hotbar slot $slot")
    }
}
