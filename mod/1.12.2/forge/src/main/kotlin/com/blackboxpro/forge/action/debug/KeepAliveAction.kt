package com.blackboxpro.forge.action.debug

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.blackboxpro.forge.util.requireLong
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.network.play.client.CPacketKeepAlive

class KeepAliveAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val id = params.requireLong("id")

        val connection = Minecraft.getMinecraft().connection
            ?: return ActionResult.fail("Not connected to server")

        connection.sendPacket(CPacketKeepAlive(id))
        return ActionResult.ok()
    }
}
