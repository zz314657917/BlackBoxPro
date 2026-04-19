package com.blackboxpro.forge.action.query

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.blackboxpro.forge.util.getStringOrNull
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft

class QueryScoreboardAction : ActionExecutor {

    override fun execute(params: JsonObject): ActionResult {
        val mc = Minecraft.getMinecraft()
        val world = mc.world
            ?: return ActionResult.fail("World not available")
        val player = mc.player
            ?: return ActionResult.fail("Player not available")

        val scoreboard = world.scoreboard
        val objectiveName = params.getStringOrNull("objective")

        val data = JsonObject()

        // 列出所有 objective
        val objectives = JsonArray()
        scoreboard.scoreObjectives.forEach { obj ->
            objectives.add(JsonObject().apply {
                addProperty("name", obj.name)
                addProperty("displayName", obj.displayName)
                addProperty("criteria", obj.criteria.name)
                addProperty("renderType", obj.renderType.name.lowercase())
            })
        }
        data.add("objectives", objectives)

        // sidebar 显示
        val sidebarObj = scoreboard.getObjectiveInDisplaySlot(1) // 1 = sidebar
        if (sidebarObj != null) {
            val sidebar = JsonObject().apply {
                addProperty("objectiveName", sidebarObj.name)
                addProperty("displayName", sidebarObj.displayName)
            }
            val scores = JsonArray()
            scoreboard.getSortedScores(sidebarObj).forEach { score ->
                scores.add(JsonObject().apply {
                    addProperty("name", score.playerName)
                    addProperty("value", score.scorePoints)
                })
            }
            sidebar.add("scores", scores)
            data.add("sidebar", sidebar)
        }

        // 指定 objective 的分数
        if (objectiveName != null) {
            val obj = scoreboard.getObjective(objectiveName)
            if (obj != null) {
                val scores = JsonArray()
                scoreboard.getSortedScores(obj).forEach { score ->
                    scores.add(JsonObject().apply {
                        addProperty("name", score.playerName)
                        addProperty("value", score.scorePoints)
                    })
                }
                data.add("queriedScores", scores)
            }
        }

        // 玩家所在队伍
        val team = scoreboard.getPlayersTeam(player.name)
        if (team != null) {
            data.add("playerTeam", JsonObject().apply {
                addProperty("name", team.name)
                addProperty("displayName", team.displayName)
                addProperty("prefix", team.prefix)
                addProperty("suffix", team.suffix)
                addProperty("color", team.color.friendlyName.uppercase())
            })
        }

        return ActionResult.ok("Scoreboard queried", data)
    }
}
