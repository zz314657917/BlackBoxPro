package com.blackboxpro.neoforge.action.advanced

import com.blackboxpro.neoforge.action.ActionExecutor
import com.blackboxpro.neoforge.action.ActionResult
import com.blackboxpro.neoforge.util.getBooleanOrDefault
import com.blackboxpro.neoforge.util.requireInt
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.network.protocol.game.ServerboundSignUpdatePacket
import net.minecraft.core.BlockPos

class UpdateSignAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val x = params.requireInt("x")
        val y = params.requireInt("y")
        val z = params.requireInt("z")
        val isFrontText = params.getBooleanOrDefault("isFrontText", true)
        val linesArray = params.getAsJsonArray("lines")
            ?: return ActionResult.fail("Missing required field: lines")

        val lines = (0 until 4).map { i ->
            if (i < linesArray.size()) linesArray[i].asString else ""
        }

        val networkHandler = Minecraft.getInstance().connection
            ?: return ActionResult.fail("Not connected to server")

        networkHandler.send(
            ServerboundSignUpdatePacket(BlockPos(x, y, z), isFrontText, lines[0], lines[1], lines[2], lines[3])
        )
        return ActionResult.ok("Updated sign at $x, $y, $z")
    }
}
