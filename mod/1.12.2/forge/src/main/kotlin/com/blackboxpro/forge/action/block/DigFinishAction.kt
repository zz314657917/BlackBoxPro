package com.blackboxpro.forge.action.block

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.blackboxpro.forge.util.DirectionUtil
import com.blackboxpro.forge.util.requireInt
import com.blackboxpro.forge.util.requireString
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.network.play.client.CPacketPlayerDigging
import net.minecraft.util.math.BlockPos

class DigFinishAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val x = params.requireInt("x")
        val y = params.requireInt("y")
        val z = params.requireInt("z")
        val face = DirectionUtil.fromString(params.requireString("face"))

        val connection = Minecraft.getMinecraft().connection
            ?: return ActionResult.fail("Not connected to server")

        connection.sendPacket(
            CPacketPlayerDigging(
                CPacketPlayerDigging.Action.STOP_DESTROY_BLOCK,
                BlockPos(x, y, z),
                face
            )
        )
        return ActionResult.ok("Finished digging block at $x, $y, $z")
    }
}
