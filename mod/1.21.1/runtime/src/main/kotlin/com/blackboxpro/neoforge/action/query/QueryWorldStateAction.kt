package com.blackboxpro.neoforge.action.query

import com.blackboxpro.neoforge.action.ActionExecutor
import com.blackboxpro.neoforge.action.ActionResult
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft

/**
 * 查询世界全局状态。
 * Action ID: "query_world_state"
 */
class QueryWorldStateAction : ActionExecutor {

    override fun execute(params: JsonObject): ActionResult {
        val client = Minecraft.getInstance()
        val world = client.level
            ?: return ActionResult.fail("World not available")

        val data = JsonObject().apply {
            addProperty("timeOfDay", world.dayTime)
            addProperty("worldTime", world.gameTime)
            addProperty("raining", world.isRaining)
            addProperty("thundering", world.isThundering)
            addProperty("dimension", world.dimension().location().toString())
            addProperty("hasSkyLight", world.dimensionType().hasSkyLight())
            addProperty("hasCeiling", world.dimensionType().hasCeiling())
            addProperty("difficulty", world.levelData.difficulty.key)
            addProperty("seaLevel", world.seaLevel)
        }

        return ActionResult.ok("World state queried", data)
    }
}
