package com.blackboxpro.neoforge.action.composite

import com.blackboxpro.neoforge.action.ActionExecutor
import com.blackboxpro.neoforge.action.ActionResult
import com.blackboxpro.neoforge.util.calculateYawPitch
import com.blackboxpro.neoforge.util.requireDouble
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket

class LookAtAction : ActionExecutor {

    override fun execute(params: JsonObject): ActionResult {
        val targetX = params.requireDouble("x")
        val targetY = params.requireDouble("y")
        val targetZ = params.requireDouble("z")

        val client = Minecraft.getInstance()
        val player = client.player
            ?: return ActionResult.fail("Player not available")
        val networkHandler = client.connection
            ?: return ActionResult.fail("Not connected to server")

        val (yaw, pitch) = calculateYawPitch(
            targetX - player.x,
            targetY - player.eyeY,
            targetZ - player.z
        )

        player.yRot = yaw
        player.xRot = pitch

        networkHandler.send(
            ServerboundMovePlayerPacket.Rot(yaw, pitch, player.onGround(), player.horizontalCollision)
        )

        return ActionResult.ok("Looking at ($targetX, $targetY, $targetZ) yaw=$yaw pitch=$pitch")
    }
}
