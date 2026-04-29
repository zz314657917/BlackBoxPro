package com.blackboxpro.forge.action.block

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.blackboxpro.forge.util.DirectionUtil
import com.blackboxpro.forge.util.getIntOrDefault
import com.blackboxpro.forge.util.requireInt
import com.blackboxpro.forge.util.requireString
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket
import net.minecraft.core.BlockPos

class DigFinishAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val x = params.requireInt("x")
        val y = params.requireInt("y")
        val z = params.requireInt("z")
        val face = DirectionUtil.fromString(params.requireString("face"))
        val sequence = params.getIntOrDefault("sequence", 0)

        val player = Minecraft.getInstance().connection
            ?: return ActionResult.fail("Not connected to server")

        player.send(
            ServerboundPlayerActionPacket(
                ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK,
                BlockPos(x, y, z),
                face,
                sequence
            )
        )
        return ActionResult.ok("Finished digging block at $x, $y, $z")
    }
}

