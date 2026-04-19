package com.blackboxpro.forge.action.chat

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.blackboxpro.forge.util.requireString
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.network.play.client.CPacketChatMessage

class ChatCommandAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val command = params.requireString("command").removePrefix("/")
        val fullCommand = "/$command"

        if (fullCommand.length > 256) {
            return ActionResult.fail("Command too long: ${fullCommand.length} > 256")
        }

        val connection = Minecraft.getMinecraft().connection
            ?: return ActionResult.fail("Not connected to server")

        connection.sendPacket(CPacketChatMessage(fullCommand))
        return ActionResult.ok("Sent command: $fullCommand")
    }
}
