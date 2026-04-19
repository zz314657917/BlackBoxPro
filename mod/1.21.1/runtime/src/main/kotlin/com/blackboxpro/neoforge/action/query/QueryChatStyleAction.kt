package com.blackboxpro.neoforge.action.query

import com.blackboxpro.neoforge.action.ActionExecutor
import com.blackboxpro.neoforge.action.ActionResult
import com.blackboxpro.neoforge.util.ChatHistoryBuffer
import com.blackboxpro.neoforge.util.ChatStyleHelper
import com.blackboxpro.neoforge.util.getIntOrDefault
import com.blackboxpro.neoforge.util.requireString
import com.google.gson.JsonObject

class QueryChatStyleAction : ActionExecutor {

    override fun execute(params: JsonObject): ActionResult {
        val match = params.requireString("match")
        val index = params.getIntOrDefault("index", 0)

        val entry = ChatHistoryBuffer.getEntry(index)
            ?: return ActionResult.fail("Chat entry not found at index $index")
        val styleMatch = ChatStyleHelper.findStyleMatch(entry.raw, match)
            ?: return ActionResult.fail("Text not found in chat entry: $match")

        val data = ChatStyleHelper.styleToJson(styleMatch.style).apply {
            addProperty("matched", styleMatch.matchedText)
            addProperty("timestamp", entry.timestamp)
            addProperty("type", entry.type)
        }
        return ActionResult.ok("Chat style queried", data)
    }
}
