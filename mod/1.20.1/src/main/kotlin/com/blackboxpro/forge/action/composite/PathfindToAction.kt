package com.blackboxpro.forge.action.composite

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.blackboxpro.common.runtime.config.RuntimeBlackBoxConfig
import com.blackboxpro.common.runtime.scheduler.RuntimeTickScheduler
import com.blackboxpro.forge.util.getDoubleOrDefault
import com.blackboxpro.forge.util.requireDouble
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket
import kotlin.math.sqrt

class PathfindToAction : ActionExecutor {

    override fun execute(params: JsonObject): ActionResult {
        val targetX = params.requireDouble("x")
        val targetY = params.requireDouble("y")
        val targetZ = params.requireDouble("z")
        val speed = params.getDoubleOrDefault("speed", 1.0)

        val client = Minecraft.getInstance()
        val player = client.player
            ?: return ActionResult.fail("Player not available")

        val pathConfig = RuntimeBlackBoxConfig.current.pathfinding
        val dx = targetX - player.x
        val dy = targetY - player.y
        val dz = targetZ - player.z
        val distance = sqrt(dx * dx + dy * dy + dz * dz)

        if (distance > pathConfig.maxDistance) {
            return ActionResult.fail("Target too far: ${"%.1f".format(distance)} > ${pathConfig.maxDistance}")
        }

        if (distance < pathConfig.arrivalThreshold) {
            return ActionResult.ok("Already at target")
        }

        // 璁＄畻鏈€澶ф鏁帮細璺濈 / 姝ラ暱 * 2 浣滀负瀹夊叏涓婇檺
        val stepSize = pathConfig.stepSize * speed
        val maxSteps = ((distance / stepSize) * 2).toInt().coerceAtLeast(10)

        moveStep(targetX, targetY, targetZ, speed, maxSteps)

        return ActionResult.ok("Pathfinding to ($targetX, $targetY, $targetZ), distance=${"%.1f".format(distance)}")
    }

    private fun moveStep(targetX: Double, targetY: Double, targetZ: Double, speed: Double, remainingSteps: Int) {
        if (remainingSteps <= 0) return

        val client = Minecraft.getInstance()
        val player = client.player ?: return
        val networkHandler = client.connection ?: return

        val pathConfig = RuntimeBlackBoxConfig.current.pathfinding
        val stepSize = pathConfig.stepSize * speed

        val dx = targetX - player.x
        val dy = targetY - player.y
        val dz = targetZ - player.z
        val distance = sqrt(dx * dx + dy * dy + dz * dz)

        if (distance < pathConfig.arrivalThreshold) return

        // 闃叉瓒婅繃鐩爣鐐?
        val ratio = (stepSize / distance).coerceAtMost(1.0)
        val newX = player.x + dx * ratio
        val newY = player.y + dy * ratio
        val newZ = player.z + dz * ratio

        player.setPos(newX, newY, newZ)
        networkHandler.send(
            ServerboundMovePlayerPacket.Pos(newX, newY, newZ, player.onGround())
        )

        RuntimeTickScheduler.schedule(1) {
            moveStep(targetX, targetY, targetZ, speed, remainingSteps - 1)
        }
    }
}

