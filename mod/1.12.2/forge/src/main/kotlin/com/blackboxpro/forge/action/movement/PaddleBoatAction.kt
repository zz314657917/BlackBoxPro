package com.blackboxpro.forge.action.movement

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.blackboxpro.forge.util.requireBoolean
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.network.play.client.CPacketSteerBoat

class PaddleBoatAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val leftPaddling = params.requireBoolean("leftPaddling")
        val rightPaddling = params.requireBoolean("rightPaddling")

        val connection = Minecraft.getMinecraft().connection
            ?: return ActionResult.fail("Not connected to server")

        connection.sendPacket(CPacketSteerBoat(leftPaddling, rightPaddling))
        return ActionResult.ok()
    }
}
