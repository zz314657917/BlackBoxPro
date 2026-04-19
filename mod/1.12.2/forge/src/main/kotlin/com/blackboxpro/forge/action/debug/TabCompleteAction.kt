package com.blackboxpro.forge.action.debug

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.blackboxpro.forge.util.getIntOrDefault
import com.blackboxpro.forge.util.requireString
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.network.play.client.CPacketTabComplete
import net.minecraft.util.math.BlockPos

class TabCompleteAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val text = params.requireString("text")

        val connection = Minecraft.getMinecraft().connection
            ?: return ActionResult.fail("Not connected to server")

        val hasTarget = params.has("x") && params.has("y") && params.has("z")
        val targetBlock = if (hasTarget) {
            BlockPos(
                params.getIntOrDefault("x", 0),
                params.getIntOrDefault("y", 0),
                params.getIntOrDefault("z", 0)
            )
        } else {
            null
        }

        connection.sendPacket(CPacketTabComplete(text, targetBlock, hasTarget))
        return ActionResult.ok()
    }
}
