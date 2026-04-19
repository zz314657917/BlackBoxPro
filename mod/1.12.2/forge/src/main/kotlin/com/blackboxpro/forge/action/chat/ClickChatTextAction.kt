package com.blackboxpro.forge.action.chat

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.blackboxpro.forge.util.ChatHistoryBuffer
import com.blackboxpro.forge.util.ChatStyleHelper
import com.blackboxpro.forge.util.getBooleanOrDefault
import com.blackboxpro.forge.util.getIntOrDefault
import com.blackboxpro.forge.util.requireString
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.network.play.client.CPacketChatMessage
import net.minecraft.util.text.event.ClickEvent

class ClickChatTextAction : ActionExecutor {

    override fun execute(params: JsonObject): ActionResult {
        val match = params.requireString("match")
        val index = params.getIntOrDefault("index", 0)
        val shouldExecute = params.getBooleanOrDefault("execute", true)

        val entry = ChatHistoryBuffer.getEntry(index)
            ?: return ActionResult.fail("Chat entry not found at index $index")
        val styleMatch = ChatStyleHelper.findStyleMatch(entry.raw, match)
            ?: return ActionResult.fail("Text not found in chat entry: $match")
        val clickEvent = styleMatch.style.clickEvent
            ?: return ActionResult.fail("Matched text has no click event")

        val data = ChatStyleHelper.clickEventToJson(clickEvent).apply {
            addProperty("matched", styleMatch.matchedText)
        }
        if (!shouldExecute) {
            data.addProperty("executed", false)
            return ActionResult.ok("Click event resolved", data)
        }

        val executed = executeClickEvent(clickEvent)
        data.addProperty("executed", executed)
        return ActionResult.ok(
            if (executed) "Chat click executed" else "Chat click resolved without execution",
            data
        )
    }

    private fun executeClickEvent(clickEvent: ClickEvent): Boolean {
        val connection = Minecraft.getMinecraft().connection ?: return false
        return when (clickEvent.action) {
            ClickEvent.Action.RUN_COMMAND -> {
                val message = clickEvent.value
                if (message.length > 256) {
                    false
                } else {
                    connection.sendPacket(CPacketChatMessage(message))
                    true
                }
            }

            else -> false
        }
    }
}
