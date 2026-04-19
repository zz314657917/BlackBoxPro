package com.blackboxpro.forge.action.block

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.blackboxpro.forge.util.*
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.network.play.client.CPacketPlayerTryUseItemOnBlock
import net.minecraft.util.math.BlockPos

class PlaceBlockAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val x = params.requireInt("x")
        val y = params.requireInt("y")
        val z = params.requireInt("z")
        val face = DirectionUtil.fromString(params.requireString("face"))
        val hand = HandUtil.fromString(params.getStringOrNull("hand") ?: "main_hand")
        val cursorX = params.getDoubleOrDefault("cursorX", 0.5).toFloat()
        val cursorY = params.getDoubleOrDefault("cursorY", 0.5).toFloat()
        val cursorZ = params.getDoubleOrDefault("cursorZ", 0.5).toFloat()

        val connection = Minecraft.getMinecraft().connection
            ?: return ActionResult.fail("Not connected to server")

        connection.sendPacket(
            CPacketPlayerTryUseItemOnBlock(
                BlockPos(x, y, z),
                face,
                hand,
                cursorX,
                cursorY,
                cursorZ
            )
        )
        return ActionResult.ok("Placed block at $x, $y, $z")
    }
}
