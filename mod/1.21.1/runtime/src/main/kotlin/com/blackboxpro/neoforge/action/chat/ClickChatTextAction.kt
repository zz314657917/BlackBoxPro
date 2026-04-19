package com.blackboxpro.neoforge.action.chat

import com.blackboxpro.neoforge.action.ActionExecutor
import com.blackboxpro.neoforge.action.ActionResult
import com.blackboxpro.neoforge.util.ChatHistoryBuffer
import com.blackboxpro.neoforge.util.ChatStyleHelper
import com.blackboxpro.neoforge.util.getBooleanOrDefault
import com.blackboxpro.neoforge.util.getIntOrDefault
import com.blackboxpro.neoforge.util.requireString
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.ClickEvent

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
        val client = Minecraft.getInstance()
        val connection = client.connection ?: return false
        return when (clickEvent.action) {
            ClickEvent.Action.RUN_COMMAND -> {
                val command = clickEvent.value
                if (command.startsWith("/")) {
                    connection.sendCommand(command.removePrefix("/"))
                } else {
                    connection.sendChat(command)
                }
                true
            }

            ClickEvent.Action.COPY_TO_CLIPBOARD -> {
                client.keyboardHandler.setClipboard(clickEvent.value)
                true
            }

            else -> false
        }
    }
}
