package com.blackboxpro.neoforge.action.query

import com.blackboxpro.neoforge.action.ActionExecutor
import com.blackboxpro.neoforge.action.ActionResult
import com.blackboxpro.neoforge.util.getIntOrDefault
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft

/**
 * 查询 Tab 列表在线玩家信息。
 * Action ID: "query_tab_list"
 */
class QueryTabListAction : ActionExecutor {

    override fun execute(params: JsonObject): ActionResult {
        val client = Minecraft.getInstance()
        val connection = client.connection
            ?: return ActionResult.fail("Not connected to server")

        val limit = params.getIntOrDefault("limit", 100).coerceIn(1, 200)

        val players = connection.onlinePlayers
            .sortedBy { it.latency }
            .take(limit)

        val arr = JsonArray()
        players.forEach { entry ->
            arr.add(JsonObject().apply {
                addProperty("name", entry.profile.name)
                addProperty("uuid", entry.profile.id.toString())
                addProperty("latency", entry.latency)
                addProperty("gameMode", entry.gameMode?.getName() ?: "unknown")
                entry.tabListDisplayName?.let { addProperty("displayName", it.string) }
                entry.team?.let { addProperty("team", it.name) }
            })
        }

        val data = JsonObject().apply {
            add("players", arr)
            addProperty("count", arr.size())
            addProperty("total", connection.onlinePlayers.size)
        }

        return ActionResult.ok("Tab list queried", data)
    }
}
