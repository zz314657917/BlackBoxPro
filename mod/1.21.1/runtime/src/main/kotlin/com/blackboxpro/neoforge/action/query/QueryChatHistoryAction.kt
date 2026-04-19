package com.blackboxpro.neoforge.action.query

import com.blackboxpro.neoforge.action.ActionExecutor
import com.blackboxpro.neoforge.action.ActionResult
import com.blackboxpro.neoforge.util.ChatHistoryBuffer
import com.blackboxpro.neoforge.util.getIntOrDefault
import com.blackboxpro.neoforge.util.getLongOrDefault
import com.blackboxpro.neoforge.util.getStringOrNull
import com.google.gson.JsonObject

/**
 * 读取聊天消息历史。
 * Action ID: "query_chat_history"
 */
class QueryChatHistoryAction : ActionExecutor {

    override fun execute(params: JsonObject): ActionResult {
        val count = params.getIntOrDefault("count", 10)
        val filter = params.getStringOrNull("filter")
        val since = if (params.has("since")) params.getLongOrDefault("since", 0L) else null

        val data = ChatHistoryBuffer.query(count, filter, since)
        return ActionResult.ok("Chat history queried", data)
    }
}
