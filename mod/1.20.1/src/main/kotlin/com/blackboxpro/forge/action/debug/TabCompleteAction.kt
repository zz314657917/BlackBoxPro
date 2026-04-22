package com.blackboxpro.forge.action.debug

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.blackboxpro.forge.util.requireInt
import com.blackboxpro.forge.util.requireString
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.network.protocol.game.ServerboundCommandSuggestionPacket

class TabCompleteAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val transactionId = params.requireInt("transactionId")
        val text = params.requireString("text")

        val networkHandler = Minecraft.getInstance().connection
            ?: return ActionResult.fail("Not connected to server")

        networkHandler.send(ServerboundCommandSuggestionPacket(transactionId, text))
        return ActionResult.ok()
    }
}

