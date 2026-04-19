package com.blackboxpro.neoforge.action.advanced

import com.blackboxpro.neoforge.action.ActionExecutor
import com.blackboxpro.neoforge.action.ActionResult
import com.blackboxpro.neoforge.util.requireInt
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.network.protocol.game.ServerboundSelectTradePacket

class SelectTradeAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val selectedSlot = params.requireInt("selectedSlot")
        if (selectedSlot < 0) {
            return ActionResult.fail("selectedSlot must be >= 0, got: $selectedSlot")
        }

        val networkHandler = Minecraft.getInstance().connection
            ?: return ActionResult.fail("Not connected to server")

        networkHandler.send(ServerboundSelectTradePacket(selectedSlot))
        return ActionResult.ok("Selected trade slot $selectedSlot")
    }
}
