package com.blackboxpro.forge.action.advanced

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.blackboxpro.forge.util.requireInt
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.network.play.client.CPacketUpdateSign
import net.minecraft.util.math.BlockPos
import net.minecraft.util.text.TextComponentString

class UpdateSignAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val x = params.requireInt("x")
        val y = params.requireInt("y")
        val z = params.requireInt("z")
        val linesArray = params.getAsJsonArray("lines")
            ?: return ActionResult.fail("Missing required field: lines")

        val connection = Minecraft.getMinecraft().connection
            ?: return ActionResult.fail("Not connected to server")

        val lines = Array(4) { i ->
            val text = if (i < linesArray.size()) linesArray[i].asString else ""
            TextComponentString(text)
        }

        val pos = BlockPos(x, y, z)
        connection.sendPacket(CPacketUpdateSign(pos, lines))

        return ActionResult.ok("Updated sign at ($x, $y, $z)")
    }
}
