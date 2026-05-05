package com.blackboxpro.forge.action.container

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.blackboxpro.forge.util.requireInt
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket

class ClickButtonAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val windowId = params.requireInt("windowId")
        val buttonId = params.requireInt("buttonId")

        val handler = Minecraft.getInstance().connection
            ?: return ActionResult.fail("Not connected to server")

        handler.send(ServerboundContainerButtonClickPacket(windowId, buttonId))
        return ActionResult.ok("Clicked button $buttonId in window $windowId")
    }
}

