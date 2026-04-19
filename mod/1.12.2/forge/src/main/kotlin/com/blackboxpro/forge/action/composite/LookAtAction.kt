package com.blackboxpro.forge.action.composite

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.blackboxpro.forge.util.calculateYawPitch
import com.blackboxpro.forge.util.requireDouble
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.network.play.client.CPacketPlayer

class LookAtAction : ActionExecutor {

    override fun execute(params: JsonObject): ActionResult {
        val targetX = params.requireDouble("x")
        val targetY = params.requireDouble("y")
        val targetZ = params.requireDouble("z")

        val mc = Minecraft.getMinecraft()
        val player = mc.player
            ?: return ActionResult.fail("Player not available")
        val connection = mc.connection
            ?: return ActionResult.fail("Not connected to server")

        val (yaw, pitch) = calculateYawPitch(
            targetX - player.posX,
            targetY - (player.posY + player.getEyeHeight()),
            targetZ - player.posZ
        )

        player.rotationYaw = yaw
        player.rotationPitch = pitch

        connection.sendPacket(CPacketPlayer.Rotation(yaw, pitch, player.onGround))

        return ActionResult.ok("Looking at ($targetX, $targetY, $targetZ) yaw=$yaw pitch=$pitch")
    }
}
