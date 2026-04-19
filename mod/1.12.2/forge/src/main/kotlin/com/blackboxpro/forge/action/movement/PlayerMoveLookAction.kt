package com.blackboxpro.forge.action.movement

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.blackboxpro.forge.action.composite.TickScheduler
import com.blackboxpro.common.runtime.config.RuntimeBlackBoxConfig
import com.blackboxpro.forge.util.InjectedMovementInput
import com.blackboxpro.forge.util.getDoubleOrDefault
import com.blackboxpro.forge.util.getIntOrDefault
import com.blackboxpro.forge.util.requireDouble
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
        val pitch = params.requireDouble("pitch").toFloat().coerceIn(-90f, 90f)
        val speed = params.getDoubleOrDefault("speed", 1.0).coerceIn(0.1, 2.0)
        val timeout = params.getIntOrDefault("timeout", 200)

        val mc = Minecraft.getMinecraft()
        val player = mc.player
            ?: return ActionResult.fail("Player not available")

        val pathConfig = RuntimeBlackBoxConfig.current.pathfinding
        val dx = x - player.posX
        val dy = y - player.posY
        val dz = z - player.posZ
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
        val injected = InjectedMovementInput()
        val mc = Minecraft.getMinecraft()
        val player = mc.player ?: return
        injected.install(player)

        var ticksElapsed = 0

        fun tick() {
            val p = Minecraft.getMinecraft().player
            if (p == null) {
                injected.reset()
                injected.uninstall()
                return
            }
            if (p.movementInput !== injected) {
                injected.reset()
                return
            }
            val threshold = RuntimeBlackBoxConfig.current.pathfinding.arrivalThreshold

            ticksElapsed++

            val dx = targetX - p.posX
            val dy = targetY - p.posY
            val dz = targetZ - p.posZ
            val totalDist = sqrt(dx * dx + dy * dy + dz * dz)

            if (totalDist < threshold || ticksElapsed >= timeout) {
                injected.reset()
                injected.uninstall(p)
                p.isSprinting = false
                return
            }

            // yaw 朝向目标，pitch 由参数指定
            val yaw = (-atan2(dx, dz) * 180.0 / Math.PI).toFloat()
            p.rotationYaw = yaw
            p.rotationPitch = pitch

            // 注入输入
            injected.forward = true
            injected.jumping = dy > 0.5 && p.onGround
            p.isSprinting = speed > 1.0

            TickScheduler.schedule(1) { tick() }
        }

        TickScheduler.schedule(1) { tick() }
    }
}
