package com.blackboxpro.forge.action.query

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.blackboxpro.forge.util.requireInt
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.level.LightLayer

/**
 * 鏌ヨ鎸囧畾鍧愭爣鐨勬柟鍧楃姸鎬併€?
 * Action ID: "query_block_state"
 */
class QueryBlockStateAction : ActionExecutor {

    override fun execute(params: JsonObject): ActionResult {
        val client = Minecraft.getInstance()
        val world = client.level
            ?: return ActionResult.fail("World not available")

        val x = params.requireInt("x")
        val y = params.requireInt("y")
        val z = params.requireInt("z")
        val pos = BlockPos(x, y, z)

        val state = world.getBlockState(pos)

        val data = JsonObject().apply {
            addProperty("blockId", BuiltInRegistries.BLOCK.getKey(state.block).toString())
            addProperty("isAir", state.isAir)

            val properties = JsonObject()
            state.values.forEach { (property, value) ->
                properties.addProperty(property.name, value.toString())
            }
            add("properties", properties)

            addProperty("lightLevel", world.getMaxLocalRawBrightness(pos))
            addProperty("blockLight", world.getBrightness(LightLayer.BLOCK, pos))
            addProperty("skyLight", world.getBrightness(LightLayer.SKY, pos))

            val biome = world.getBiome(pos)
            addProperty("biome", biome.unwrapKey().map { it.location().toString() }.orElse("unknown"))

            addProperty("hardness", state.getDestroySpeed(world, pos))
            addProperty("x", x)
            addProperty("y", y)
            addProperty("z", z)
        }

        return ActionResult.ok("Block state queried at ($x, $y, $z)", data)
    }
}

