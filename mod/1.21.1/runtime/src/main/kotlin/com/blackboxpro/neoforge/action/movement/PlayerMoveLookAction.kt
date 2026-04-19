package com.blackboxpro.neoforge.action.movement

import com.blackboxpro.neoforge.action.ActionExecutor
import com.blackboxpro.neoforge.action.ActionResult
import com.blackboxpro.common.runtime.scheduler.RuntimeTickScheduler
import com.blackboxpro.common.runtime.config.RuntimeBlackBoxConfig
import com.blackboxpro.neoforge.util.InjectedInput
import com.blackboxpro.neoforge.util.getDoubleOrDefault
import com.blackboxpro.neoforge.util.getIntOrDefault
import com.blackboxpro.neoforge.util.requireDouble
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * 模拟键盘驱动移动 + 设置 pitch。
 * yaw 自动朝向目标，pitch 由参数指定。
 */
class PlayerMoveLookAction : ActionExecutor {

    override fun execute(params: JsonObject): ActionResult {
        val x = params.requireDouble("x")
        val y = params.requireDouble("y")
        val z = params.requireDouble("z")
        val pitch = params.requireDouble("pitch").toFloat()
        val speed = params.getDoubleOrDefault("speed", 1.0).coerceIn(0.1, 2.0)
        val timeout = params.getIntOrDefault("timeout", 200)

        val client = Minecraft.getInstance()
        val player = client.player
            ?: return ActionResult.fail("Player not available")

        val pathConfig = RuntimeBlackBoxConfig.current.pathfinding
        val dx = x - player.x
        val dy = y - player.y
        val dz = z - player.z
        val distance = sqrt(dx * dx + dy * dy + dz * dz)

        if (distance > pathConfig.maxDistance) {
            return ActionResult.fail("Target too far: ${"%.1f".format(distance)} > ${pathConfig.maxDistance}")
        }

        if (distance < pathConfig.arrivalThreshold) {
            return ActionResult.ok("Already at target")
        }

        startMovement(x, y, z, pitch, speed, timeout)
        return ActionResult.ok("Moving to (${"%.1f".format(x)}, ${"%.1f".format(y)}, ${"%.1f".format(z)}), distance=${"%.1f".format(distance)}")
    }

    private fun startMovement(targetX: Double, targetY: Double, targetZ: Double, pitch: Float, speed: Double, timeout: Int) {
        val injected = InjectedInput()
        val client = Minecraft.getInstance()
        val player = client.player ?: return
        injected.install(player)

        var ticksElapsed = 0

        fun tick() {
            val p = Minecraft.getInstance().player
            if (p == null) {
                injected.reset()
                injected.uninstall()
                return
            }
            val threshold = RuntimeBlackBoxConfig.current.pathfinding.arrivalThreshold

            ticksElapsed++

            val dx = targetX - p.x
            val dy = targetY - p.y
            val dz = targetZ - p.z
            val totalDist = sqrt(dx * dx + dy * dy + dz * dz)

            if (totalDist < threshold || ticksElapsed >= timeout) {
                injected.reset()
                injected.uninstall(p)
                return
            }

            // yaw 朝向目标，pitch 由参数指定
            val yaw = (-atan2(dx, dz) * 180.0 / Math.PI).toFloat()
            p.yRot = yaw
            p.xRot = pitch

            // 注入输入
            injected.forward = true
            injected.jump = dy > 0.5 && p.onGround()
            injected.sprinting = speed > 1.0
            p.isSprinting = speed > 1.0

            RuntimeTickScheduler.schedule(1) { tick() }
        }

        RuntimeTickScheduler.schedule(1) { tick() }
    }
}
