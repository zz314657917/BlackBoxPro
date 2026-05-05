package com.blackboxpro.forge.action.advanced

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.blackboxpro.forge.util.requireInt
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.network.protocol.game.ServerboundSelectTradePacket

class SelectTradeAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val selectedSlot = params.requireInt("selectedSlot")

        val networkHandler = Minecraft.getInstance().connection
            ?: return ActionResult.fail("Not connected to server")

        networkHandler.send(ServerboundSelectTradePacket(selectedSlot))
        return ActionResult.ok("Selected trade slot $selectedSlot")
    }
}

