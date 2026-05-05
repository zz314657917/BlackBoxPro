package com.blackboxpro.forge.action.debug

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.blackboxpro.forge.util.requireLong
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.network.protocol.game.ServerboundKeepAlivePacket

class KeepAliveAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val id = params.requireLong("id")

        val networkHandler = Minecraft.getInstance().connection
            ?: return ActionResult.fail("Not connected to server")

        networkHandler.send(ServerboundKeepAlivePacket(id))
        return ActionResult.ok()
    }
}

