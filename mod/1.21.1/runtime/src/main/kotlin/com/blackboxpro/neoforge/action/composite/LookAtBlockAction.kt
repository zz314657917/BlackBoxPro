package com.blackboxpro.neoforge.action.composite

import com.blackboxpro.neoforge.action.ActionExecutor
import com.blackboxpro.neoforge.action.ActionResult
import com.blackboxpro.neoforge.util.BlockFaceUtil
import com.blackboxpro.neoforge.util.calculateYawPitch
import com.blackboxpro.neoforge.util.getStringOrNull
import com.blackboxpro.neoforge.util.requireInt
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket

/**
 * 瞄准指定方块的指定面。
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
