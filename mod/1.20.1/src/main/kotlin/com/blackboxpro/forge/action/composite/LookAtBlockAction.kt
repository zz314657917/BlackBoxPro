package com.blackboxpro.forge.action.composite

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.blackboxpro.forge.util.BlockFaceUtil
import com.blackboxpro.forge.util.calculateYawPitch
import com.blackboxpro.forge.util.getStringOrNull
import com.blackboxpro.forge.util.requireInt
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket

/**
 * 鐬勫噯鎸囧畾鏂瑰潡鐨勬寚瀹氶潰銆?
 * Action ID: "look_at_block"
 */
class LookAtBlockAction : ActionExecutor {

    override fun execute(params: JsonObject): ActionResult {
        val x = params.requireInt("x")
        val y = params.requireInt("y")
        val z = params.requireInt("z")
        val face = params.getStringOrNull("face") ?: "top"

        val client = Minecraft.getInstance()
        val player = client.player
            ?: return ActionResult.fail("Player not available")
        val connection = client.connection
            ?: return ActionResult.fail("Not connected to server")

        val center = BlockFaceUtil.calculateFaceCenter(x, y, z, face)
        val (yaw, pitch) = calculateYawPitch(
            center.x - player.x,
            center.y - player.eyeY,
            center.z - player.z
        )

        player.yRot = yaw
        player.xRot = pitch

        connection.send(
            ServerboundMovePlayerPacket.Rot(yaw, pitch, player.onGround())
        )

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

