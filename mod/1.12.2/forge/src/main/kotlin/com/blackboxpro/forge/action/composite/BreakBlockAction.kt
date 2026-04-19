package com.blackboxpro.forge.action.composite

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.blackboxpro.common.runtime.config.RuntimeBlackBoxConfig
import com.blackboxpro.forge.dispatcher.ActionRegistry
import com.blackboxpro.forge.util.requireInt
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.util.math.BlockPos
import kotlin.math.ceil

class BreakBlockAction : ActionExecutor {

    override fun execute(params: JsonObject): ActionResult {
        val x = params.requireInt("x")
        val y = params.requireInt("y")
        val z = params.requireInt("z")

        val mc = Minecraft.getMinecraft()
        mc.player ?: return ActionResult.fail("Player not available")

        val breakTicks = calculateBreakTicks(mc, x, y, z)
        if (breakTicks < 0) {
            return ActionResult.fail("Block at ($x, $y, $z) is unbreakable")
        }

        // Step 1: look at block center
        val lookAt = ActionRegistry.find("look_at")
            ?: return ActionResult.fail("look_at action not registered")
        val lookResult = lookAt.execute(JsonObject().apply {
            addProperty("x", x + 0.5)
            addProperty("y", y + 0.5)
            addProperty("z", z + 0.5)
        })
        if (!lookResult.success) return ActionResult.fail("Failed to look at block: ${lookResult.message}")

        // Step 2: dig_start
        val digStart = ActionRegistry.find("dig_start")
            ?: return ActionResult.fail("dig_start action not registered")
        val digResult = digStart.execute(JsonObject().apply {
            addProperty("x", x)
            addProperty("y", y)
            addProperty("z", z)
            addProperty("face", "top")
        })
        if (!digResult.success) return ActionResult.fail("Failed to start digging: ${digResult.message}")

        // Step 3: schedule dig_finish
        TickScheduler.schedule(breakTicks) {
            val world = Minecraft.getMinecraft().world ?: return@schedule
            val pos = BlockPos(x, y, z)
            if (world.isAirBlock(pos)) return@schedule

            val digFinish = ActionRegistry.find("dig_finish") ?: return@schedule
            digFinish.execute(JsonObject().apply {
                addProperty("x", x)
                addProperty("y", y)
                addProperty("z", z)
                addProperty("face", "top")
            })
        }

        return ActionResult.ok("Breaking block at ($x, $y, $z), estimated $breakTicks ticks")
    }

    private fun calculateBreakTicks(mc: Minecraft, x: Int, y: Int, z: Int): Int {
        val defaultTicks = RuntimeBlackBoxConfig.current.execution.defaultBreakTicks
        val world = mc.world ?: return defaultTicks
        val player = mc.player ?: return defaultTicks
        val pos = BlockPos(x, y, z)
        val state = world.getBlockState(pos)

        val hardness = state.getBlockHardness(world, pos)
        if (hardness < 0) return -1
        if (hardness == 0f) return 1

        val speed = player.getDigSpeed(state)
        val canHarvest = net.minecraftforge.common.ForgeHooks.canHarvestBlock(state.block, player, world, pos)
        val divisor = if (canHarvest) 30.0f else 100.0f
        return if (speed > 0) {
            ceil(hardness * divisor / speed).toInt().coerceAtLeast(1)
        } else {
            defaultTicks
        }
    }
}
