package com.blackboxpro.forge.action.movement

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.blackboxpro.forge.util.getBooleanOrDefault
import com.blackboxpro.forge.util.requireDouble
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.network.play.client.CPacketPlayer

class PlayerLookAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val yaw = params.requireDouble("yaw").toFloat()
        val pitch = params.requireDouble("pitch").toFloat().coerceIn(-90f, 90f)
        val onGround = params.getBooleanOrDefault("onGround", true)

        val mc = Minecraft.getMinecraft()
        val player = mc.player
            ?: return ActionResult.fail("Player not available")
        val connection = mc.connection
            ?: return ActionResult.fail("Not connected to server")

        player.rotationYaw = yaw
        player.rotationPitch = pitch
        connection.sendPacket(CPacketPlayer.Rotation(yaw, pitch, onGround))
        return ActionResult.ok()
    }
}
