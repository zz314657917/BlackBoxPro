package com.blackboxpro.forge.action.chat

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.blackboxpro.forge.util.requireString
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.network.play.client.CPacketChatMessage

class ChatMessageAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val message = params.requireString("message")

        if (message.length > 256) {
            return ActionResult.fail("Message too long: ${message.length} > 256")
        }

        val connection = Minecraft.getMinecraft().connection
            ?: return ActionResult.fail("Not connected to server")

        connection.sendPacket(CPacketChatMessage(message))
        return ActionResult.ok("Sent chat message")
    }
}
