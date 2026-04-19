package com.blackboxpro.neoforge.action.debug

import com.blackboxpro.neoforge.action.ActionExecutor
import com.blackboxpro.neoforge.action.ActionResult
import com.blackboxpro.neoforge.util.requireInt
import com.blackboxpro.neoforge.util.requireString
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
