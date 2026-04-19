package com.blackboxpro.forge.action.query

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.blackboxpro.forge.util.ChatHistoryBuffer
import com.blackboxpro.forge.util.getIntOrDefault
import com.blackboxpro.forge.util.getLongOrDefault
import com.blackboxpro.forge.util.getStringOrNull
import com.google.gson.JsonObject

class QueryChatHistoryAction : ActionExecutor {

    override fun execute(params: JsonObject): ActionResult {
        val count = params.getIntOrDefault("count", 10)
        val filter = params.getStringOrNull("filter")
        val since = if (params.has("since")) params.getLongOrDefault("since", 0L) else null

        val data = ChatHistoryBuffer.query(count, filter, since)
        return ActionResult.ok("Chat history queried", data)
    }
}
