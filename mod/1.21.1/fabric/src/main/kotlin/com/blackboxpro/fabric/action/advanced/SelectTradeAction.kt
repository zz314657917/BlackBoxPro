package com.blackboxpro.fabric.action.advanced

import com.blackboxpro.fabric.action.ActionExecutor
import com.blackboxpro.fabric.action.ActionResult
import com.blackboxpro.fabric.util.requireInt
import com.google.gson.JsonObject
import net.minecraft.client.MinecraftClient
import net.minecraft.network.packet.c2s.play.SelectMerchantTradeC2SPacket

class SelectTradeAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val selectedSlot = params.requireInt("selectedSlot")

        val networkHandler = MinecraftClient.getInstance().networkHandler
            ?: return ActionResult.fail("Not connected to server")

        networkHandler.sendPacket(SelectMerchantTradeC2SPacket(selectedSlot))
        return ActionResult.ok("Selected trade slot $selectedSlot")
    }
}
