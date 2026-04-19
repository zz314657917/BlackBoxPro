package com.blackboxpro.forge.action.composite

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.blackboxpro.forge.util.BlockFaceUtil
import com.blackboxpro.forge.util.calculateYawPitch
import com.blackboxpro.forge.util.getStringOrNull
import com.blackboxpro.forge.util.requireInt
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.network.play.client.CPacketPlayer

class LookAtBlockAction : ActionExecutor {

    override fun execute(params: JsonObject): ActionResult {
        val x = params.requireInt("x")
        val y = params.requireInt("y")
        val z = params.requireInt("z")
        val face = params.getStringOrNull("face") ?: "top"

        val mc = Minecraft.getMinecraft()
        val player = mc.player
            ?: return ActionResult.fail("Player not available")
        val connection = mc.connection
            ?: return ActionResult.fail("Not connected to server")

        val center = BlockFaceUtil.calculateFaceCenter(x, y, z, face)
        val (yaw, pitch) = calculateYawPitch(
            center.x - player.posX,
            center.y - (player.posY + player.getEyeHeight()),
            center.z - player.posZ
        )

        player.rotationYaw = yaw
        player.rotationPitch = pitch

        connection.sendPacket(CPacketPlayer.Rotation(yaw, pitch, player.onGround))

        val data = JsonObject().apply {
            addProperty("yaw", yaw)
            addProperty("pitch", pitch)
            addProperty("face", face)
            addProperty("targetX", center.x)
            addProperty("targetY", center.y)
            addProperty("targetZ", center.z)
        }

        return ActionResult.ok("Looking at block ($x, $y, $z) face=$face", data)
    }
}
