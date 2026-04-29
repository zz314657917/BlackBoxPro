package com.blackboxpro.forge.action.query

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.blackboxpro.forge.util.getStringOrNull
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.world.scores.Score

class QueryScoreboardAction : ActionExecutor {

    override fun execute(params: JsonObject): ActionResult {
        val client = Minecraft.getInstance()
        val world = client.level
            ?: return ActionResult.fail("World not available")
        val player = client.player
            ?: return ActionResult.fail("Player not available")

        val scoreboard = world.scoreboard
        val objectiveName = params.getStringOrNull("objective")

        val data = JsonObject()
        val objectives = JsonArray()
        scoreboard.objectives.forEach { obj ->
            objectives.add(JsonObject().apply {
                addProperty("name", obj.name)
                addProperty("displayName", obj.displayName.string)
                addProperty("criteria", obj.criteria.name)
                addProperty("renderType", obj.renderType.name.lowercase())
            })
        }
        data.add("objectives", objectives)

        val sidebarObj = scoreboard.getDisplayObjective(1)
        if (sidebarObj != null) {
            val sidebar = JsonObject().apply {
                addProperty("objectiveName", sidebarObj.name)
                addProperty("displayName", sidebarObj.displayName.string)
            }
            val scores = JsonArray()
            scoreboard.getPlayerScores(sidebarObj)
                .sortedWith(Score.SCORE_COMPARATOR)
                .forEach { entry ->
                    scores.add(JsonObject().apply {
                        addProperty("name", entry.owner)
                        addProperty("value", entry.score)
                    })
                }
            sidebar.add("scores", scores)
            data.add("sidebar", sidebar)
        }

        if (objectiveName != null) {
            val obj = scoreboard.getObjective(objectiveName)
            if (obj != null) {
                val scores = JsonArray()
                scoreboard.getPlayerScores(obj)
                    .sortedWith(Score.SCORE_COMPARATOR)
                    .forEach { entry ->
                        scores.add(JsonObject().apply {
                            addProperty("name", entry.owner)
                            addProperty("value", entry.score)
                        })
                    }
                data.add("queriedScores", scores)
            }
        }

        val team = scoreboard.getPlayersTeam(player.scoreboardName)
        if (team != null) {
            data.add("playerTeam", JsonObject().apply {
                addProperty("name", team.name)
                addProperty("displayName", team.displayName.string)
                addProperty("prefix", team.playerPrefix.string)
                addProperty("suffix", team.playerSuffix.string)
                addProperty("color", team.color.name)
            })
        }

        return ActionResult.ok("Scoreboard queried", data)
    }
}
