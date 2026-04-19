package com.blackboxpro.neoforge.action.movement

import com.blackboxpro.neoforge.action.ActionExecutor
import com.blackboxpro.neoforge.action.ActionResult
import com.blackboxpro.neoforge.util.requireBoolean
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.network.protocol.game.ServerboundPaddleBoatPacket

class PaddleBoatAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val leftPaddling = params.requireBoolean("leftPaddling")
        val rightPaddling = params.requireBoolean("rightPaddling")

        val networkHandler = Minecraft.getInstance().connection
            ?: return ActionResult.fail("Not connected to server")

        networkHandler.send(ServerboundPaddleBoatPacket(leftPaddling, rightPaddling))
        return ActionResult.ok()
    }
}
