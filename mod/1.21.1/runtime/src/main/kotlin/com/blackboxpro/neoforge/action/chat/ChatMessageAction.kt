package com.blackboxpro.neoforge.action.chat

import com.blackboxpro.neoforge.action.ActionExecutor
import com.blackboxpro.neoforge.action.ActionResult
import com.blackboxpro.neoforge.util.requireString
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft

class ChatMessageAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val message = params.requireString("message")

        if (message.length > 256) {
            return ActionResult.fail("Message too long: ${message.length} > 256")
        }

        val networkHandler = Minecraft.getInstance().connection
            ?: return ActionResult.fail("Not connected to server")

        networkHandler.sendChat(message)
        return ActionResult.ok("Sent chat message")
    }
}
