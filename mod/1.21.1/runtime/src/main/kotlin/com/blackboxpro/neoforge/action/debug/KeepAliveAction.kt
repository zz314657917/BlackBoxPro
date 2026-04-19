package com.blackboxpro.neoforge.action.debug

import com.blackboxpro.neoforge.action.ActionExecutor
import com.blackboxpro.neoforge.action.ActionResult
import com.blackboxpro.neoforge.util.requireLong
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.network.protocol.common.ServerboundKeepAlivePacket

class KeepAliveAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val id = params.requireLong("id")

        val networkHandler = Minecraft.getInstance().connection
            ?: return ActionResult.fail("Not connected to server")

        networkHandler.send(ServerboundKeepAlivePacket(id))
        return ActionResult.ok()
    }
}
