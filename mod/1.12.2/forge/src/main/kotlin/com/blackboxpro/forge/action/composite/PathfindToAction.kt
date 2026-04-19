package com.blackboxpro.forge.action.composite

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.blackboxpro.common.runtime.config.RuntimeBlackBoxConfig
import com.blackboxpro.forge.util.getDoubleOrDefault
import com.blackboxpro.forge.util.requireDouble
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.network.play.client.CPacketPlayer
import kotlin.math.sqrt

/**
 * 直线传送式移动（绕过物理引擎）。
 * 注意：此 Action 直接 setPosition + sendPacket，不经过碰撞/重力检测。
 * 推荐使用 navigate_to（A* 寻路 + InjectedMovementInput 物理引擎驱动）替代。
 */
class PathfindToAction : ActionExecutor {

    override fun execute(params: JsonObject): ActionResult {
        val targetX = params.requireDouble("x")
        val targetY = params.requireDouble("y")
        val targetZ = params.requireDouble("z")
        val speed = params.getDoubleOrDefault("speed", 1.0)

        val mc = Minecraft.getMinecraft()
        val player = mc.player
            ?: return ActionResult.fail("Player not available")

        val pathConfig = RuntimeBlackBoxConfig.current.pathfinding
        val dx = targetX - player.posX
        val dy = targetY - player.posY
        val dz = targetZ - player.posZ
        val distance = sqrt(dx * dx + dy * dy + dz * dz)

        if (distance > pathConfig.maxDistance) {
            return ActionResult.fail("Target too far: ${"%.1f".format(distance)} > ${pathConfig.maxDistance}")
        }

        if (distance < pathConfig.arrivalThreshold) {
            return ActionResult.ok("Already at target")
        }

        val stepSize = pathConfig.stepSize * speed
        val maxSteps = ((distance / stepSize) * 2).toInt().coerceAtLeast(10)

        moveStep(targetX, targetY, targetZ, speed, maxSteps)

        return ActionResult.ok("Pathfinding to ($targetX, $targetY, $targetZ), distance=${"%.1f".format(distance)}")
    }

    private fun moveStep(targetX: Double, targetY: Double, targetZ: Double, speed: Double, remainingSteps: Int) {
        if (remainingSteps <= 0) return

        val mc = Minecraft.getMinecraft()
        val player = mc.player ?: return
        val connection = mc.connection ?: return

        val pathConfig = RuntimeBlackBoxConfig.current.pathfinding
        val stepSize = pathConfig.stepSize * speed

        val dx = targetX - player.posX
        val dy = targetY - player.posY
        val dz = targetZ - player.posZ
        val distance = sqrt(dx * dx + dy * dy + dz * dz)

        if (distance < pathConfig.arrivalThreshold) return

        val ratio = (stepSize / distance).coerceAtMost(1.0)
        val newX = player.posX + dx * ratio
        val newY = player.posY + dy * ratio
        val newZ = player.posZ + dz * ratio

        player.setPosition(newX, newY, newZ)
        connection.sendPacket(CPacketPlayer.Position(newX, newY, newZ, player.onGround))

        TickScheduler.schedule(1) {
            moveStep(targetX, targetY, targetZ, speed, remainingSteps - 1)
        }
    }
}
