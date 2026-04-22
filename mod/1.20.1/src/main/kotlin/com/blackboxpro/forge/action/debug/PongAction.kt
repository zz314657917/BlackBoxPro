package com.blackboxpro.forge.action.debug

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.blackboxpro.forge.util.requireInt
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.network.protocol.game.ServerboundPongPacket

class PongAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val parameter = params.requireInt("parameter")

        val networkHandler = Minecraft.getInstance().connection
            ?: return ActionResult.fail("Not connected to server")

        networkHandler.send(ServerboundPongPacket(parameter))
        return ActionResult.ok()
    }
}

