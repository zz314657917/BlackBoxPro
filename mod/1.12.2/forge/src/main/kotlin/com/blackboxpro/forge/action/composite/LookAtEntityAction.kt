package com.blackboxpro.forge.action.composite

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.blackboxpro.forge.util.calculateYawPitch
import com.blackboxpro.forge.util.requireInt
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.network.play.client.CPacketPlayer

class LookAtEntityAction : ActionExecutor {

    override fun execute(params: JsonObject): ActionResult {
        val entityId = params.requireInt("entityId")

        val mc = Minecraft.getMinecraft()
        val player = mc.player
            ?: return ActionResult.fail("Player not available")
        val world = mc.world
            ?: return ActionResult.fail("World not available")
        val connection = mc.connection
            ?: return ActionResult.fail("Not connected to server")

        val entity = world.getEntityByID(entityId)
            ?: return ActionResult.fail("Entity not found: $entityId")

        val (yaw, pitch) = calculateYawPitch(
            entity.posX - player.posX,
            (entity.posY + entity.getEyeHeight()) - (player.posY + player.getEyeHeight()),
            entity.posZ - player.posZ
        )

        player.rotationYaw = yaw
        player.rotationPitch = pitch

        connection.sendPacket(CPacketPlayer.Rotation(yaw, pitch, player.onGround))

        return ActionResult.ok("Looking at entity $entityId yaw=$yaw pitch=$pitch")
    }
}
