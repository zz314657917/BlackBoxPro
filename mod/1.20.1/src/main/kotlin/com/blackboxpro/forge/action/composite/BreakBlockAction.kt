package com.blackboxpro.forge.action.composite

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.blackboxpro.common.runtime.config.RuntimeBlackBoxConfig
import com.blackboxpro.common.runtime.scheduler.RuntimeTickScheduler
import com.blackboxpro.forge.dispatcher.ActionRegistry
import com.blackboxpro.forge.util.requireInt
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import kotlin.math.ceil

class BreakBlockAction : ActionExecutor {

    override fun execute(params: JsonObject): ActionResult {
        val x = params.requireInt("x")
        val y = params.requireInt("y")
        val z = params.requireInt("z")

        val client = Minecraft.getInstance()
        client.player ?: return ActionResult.fail("Player not available")

        // 妫€鏌ユ柟鍧楁槸鍚﹀彲鐮村潖
        val breakTicks = calculateBreakTicks(client, x, y, z)
        if (breakTicks < 0) {
            return ActionResult.fail("Block at ($x, $y, $z) is unbreakable")
        }

        // Step 1: 鐪嬪悜鏂瑰潡涓績
        val lookAt = ActionRegistry.find("look_at")
            ?: return ActionResult.fail("look_at action not registered")
        val lookResult = lookAt.execute(JsonObject().apply {
            addProperty("x", x + 0.5)
            addProperty("y", y + 0.5)
            addProperty("z", z + 0.5)
        })
        if (!lookResult.success) {
            return ActionResult.fail("Failed to look at block: ${lookResult.message}")
        }

        // Step 2: 寮€濮嬫寲鎺?
        val digStart = ActionRegistry.find("dig_start")
            ?: return ActionResult.fail("dig_start action not registered")
        val digResult = digStart.execute(JsonObject().apply {
            addProperty("x", x)
            addProperty("y", y)
            addProperty("z", z)
            addProperty("face", "top")
            addProperty("sequence", 0)
        })
        if (!digResult.success) {
            return ActionResult.fail("Failed to start digging: ${digResult.message}")
        }

        // Step 3: 寤惰繜鍚庡畬鎴愭寲鎺?
        RuntimeTickScheduler.schedule(breakTicks) {
            // 鍥炶皟鏃舵鏌ユ柟鍧楁槸鍚︿粛鐒跺瓨鍦?
            val world = Minecraft.getInstance().level ?: return@schedule
            val pos = BlockPos(x, y, z)
            if (world.getBlockState(pos).isAir) return@schedule

            val digFinish = ActionRegistry.find("dig_finish") ?: return@schedule
            digFinish.execute(JsonObject().apply {
                addProperty("x", x)
                addProperty("y", y)
                addProperty("z", z)
                addProperty("face", "top")
                addProperty("sequence", 1)
            })
        }

        return ActionResult.ok("Breaking block at ($x, $y, $z), estimated $breakTicks ticks")
    }

    /**
     * 璁＄畻鎸栨帢鏂瑰潡鎵€闇€鐨?tick 鏁般€?
     * @return tick 鏁帮紝-1 琛ㄧず涓嶅彲鐮村潖
     */
    private fun calculateBreakTicks(client: Minecraft, x: Int, y: Int, z: Int): Int {
        val defaultTicks = RuntimeBlackBoxConfig.current.execution.defaultBreakTicks
        val world = client.level ?: return defaultTicks
        val player = client.player ?: return defaultTicks
        val pos = BlockPos(x, y, z)
        val state = world.getBlockState(pos)

        val hardness = state.getDestroySpeed(world, pos)
        if (hardness < 0) return -1 // 涓嶅彲鐮村潖
        if (hardness == 0f) return 1

        val speed = player.getDestroySpeed(state)
        return if (speed > 0) {
            ceil(hardness * 30.0f / speed).toInt().coerceAtLeast(1)
        } else {
            defaultTicks
        }
    }
}

