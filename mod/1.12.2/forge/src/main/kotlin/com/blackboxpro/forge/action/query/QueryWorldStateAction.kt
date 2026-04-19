package com.blackboxpro.forge.action.query

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft

class QueryWorldStateAction : ActionExecutor {

    override fun execute(params: JsonObject): ActionResult {
        val mc = Minecraft.getMinecraft()
        val world = mc.world
            ?: return ActionResult.fail("World not available")

        val data = JsonObject().apply {
            addProperty("timeOfDay", world.worldTime % 24000)
            addProperty("worldTime", world.totalWorldTime)
            addProperty("raining", world.isRaining)
            addProperty("thundering", world.isThundering)
            addProperty("dimension", dimensionToString(world.provider.dimension))
            addProperty("hasSkyLight", world.provider.hasSkyLight())
            addProperty("hasCeiling", world.provider.dimension == -1)
            addProperty("difficulty", world.difficulty.name.lowercase())
            addProperty("seaLevel", world.seaLevel)
        }

        return ActionResult.ok("World state queried", data)
    }

    private fun dimensionToString(dim: Int): String = when (dim) {
        0 -> "minecraft:overworld"
        -1 -> "minecraft:the_nether"
        1 -> "minecraft:the_end"
        else -> "unknown:dim_$dim"
    }
}
