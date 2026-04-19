package com.blackboxpro.neoforge.action.composite

import com.blackboxpro.neoforge.action.ActionExecutor
import com.blackboxpro.neoforge.action.ActionResult
import com.blackboxpro.neoforge.util.calculateYawPitch
import com.blackboxpro.neoforge.util.requireInt
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket

class LookAtEntityAction : ActionExecutor {

    override fun execute(params: JsonObject): ActionResult {
        val entityId = params.requireInt("entityId")

        val client = Minecraft.getInstance()
        val player = client.player
            ?: return ActionResult.fail("Player not available")
        val world = client.level
            ?: return ActionResult.fail("World not available")
        val networkHandler = client.connection
            ?: return ActionResult.fail("Not connected to server")

        val entity = world.getEntity(entityId)
            ?: return ActionResult.fail("Entity not found: $entityId")

        val (yaw, pitch) = calculateYawPitch(
            entity.x - player.x,
            entity.eyeY - player.eyeY,
            entity.z - player.z
        )

        player.yRot = yaw
        player.xRot = pitch

        networkHandler.send(
            ServerboundMovePlayerPacket.Rot(yaw, pitch, player.onGround(), player.horizontalCollision)
        )

        return ActionResult.ok("Looking at entity $entityId yaw=$yaw pitch=$pitch")
    }
}
