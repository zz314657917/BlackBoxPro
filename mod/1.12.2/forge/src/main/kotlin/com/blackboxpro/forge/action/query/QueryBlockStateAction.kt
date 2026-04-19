package com.blackboxpro.forge.action.query

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.blackboxpro.forge.util.requireInt
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.util.math.BlockPos
import net.minecraft.world.EnumSkyBlock

class QueryBlockStateAction : ActionExecutor {

    override fun execute(params: JsonObject): ActionResult {
        val mc = Minecraft.getMinecraft()
        val world = mc.world
            ?: return ActionResult.fail("World not available")

        val x = params.requireInt("x")
        val y = params.requireInt("y")
        val z = params.requireInt("z")
        val pos = BlockPos(x, y, z)

        val state = world.getBlockState(pos)

        val data = JsonObject().apply {
            addProperty("blockId", state.block.registryName?.toString() ?: "unknown")
            addProperty("isAir", state.block.isAir(state, world, pos))

            val properties = JsonObject()
            for (entry in state.properties.entries) {
                @Suppress("UNCHECKED_CAST")
                val prop = entry.key as net.minecraft.block.properties.IProperty<Comparable<Any>>
                val value = entry.value as Comparable<Any>
                properties.addProperty(prop.getName(), prop.getName(value))
            }
            add("properties", properties)

            addProperty("lightLevel", world.getLight(pos))
            addProperty("blockLight", world.getLightFor(EnumSkyBlock.BLOCK, pos))
            addProperty("skyLight", world.getLightFor(EnumSkyBlock.SKY, pos))

            val biome = world.getBiome(pos)
            addProperty("biome", biome.registryName?.toString() ?: "unknown")

            addProperty("hardness", state.getBlockHardness(world, pos))
            addProperty("x", x)
            addProperty("y", y)
            addProperty("z", z)
        }

        return ActionResult.ok("Block state queried at ($x, $y, $z)", data)
    }
}
