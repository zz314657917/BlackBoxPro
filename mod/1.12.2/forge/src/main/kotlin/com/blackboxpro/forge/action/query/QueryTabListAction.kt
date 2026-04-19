package com.blackboxpro.forge.action.query

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.blackboxpro.forge.util.getIntOrDefault
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft

class QueryTabListAction : ActionExecutor {

    override fun execute(params: JsonObject): ActionResult {
        val mc = Minecraft.getMinecraft()
        val connection = mc.connection
            ?: return ActionResult.fail("Not connected to server")

        val limit = params.getIntOrDefault("limit", 100).coerceIn(1, 200)

        val players = connection.playerInfoMap
            .sortedBy { it.responseTime }
            .take(limit)

        val arr = JsonArray()
        players.forEach { entry ->
            arr.add(JsonObject().apply {
                addProperty("name", entry.gameProfile.name)
                addProperty("uuid", entry.gameProfile.id.toString())
                addProperty("latency", entry.responseTime)
                addProperty("gameMode", entry.gameType?.getName() ?: "unknown")
                entry.displayName?.let { addProperty("displayName", it.unformattedText) }
                entry.playerTeam?.let { addProperty("team", it.name) }
            })
        }

        val data = JsonObject().apply {
            add("players", arr)
            addProperty("count", arr.size())
            addProperty("total", connection.playerInfoMap.size)
        }

        return ActionResult.ok("Tab list queried", data)
    }
}
