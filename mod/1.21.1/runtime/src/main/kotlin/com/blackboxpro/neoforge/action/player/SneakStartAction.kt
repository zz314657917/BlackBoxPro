package com.blackboxpro.neoforge.action.player

import com.blackboxpro.neoforge.action.ActionExecutor
import com.blackboxpro.neoforge.action.ActionResult
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket

class SneakStartAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val networkHandler = Minecraft.getInstance().connection
            ?: return ActionResult.fail("Not connected to server")

        networkHandler.send(ServerboundPlayerInputPacket(0.0f, 0.0f, false, true))
        return ActionResult.ok("Started sneaking")
    }
}
