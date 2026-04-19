package com.blackboxpro.neoforge.action.debug

import com.blackboxpro.neoforge.action.ActionExecutor
import com.blackboxpro.neoforge.action.ActionResult
import com.blackboxpro.neoforge.util.requireInt
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.network.protocol.common.ServerboundPongPacket

class PongAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val parameter = params.requireInt("parameter")

        val networkHandler = Minecraft.getInstance().connection
            ?: return ActionResult.fail("Not connected to server")

        networkHandler.send(ServerboundPongPacket(parameter))
        return ActionResult.ok()
    }
}
