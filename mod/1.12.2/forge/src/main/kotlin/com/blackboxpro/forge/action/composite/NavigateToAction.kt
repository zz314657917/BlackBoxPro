package com.blackboxpro.forge.action.composite

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.blackboxpro.common.runtime.config.RuntimeBlackBoxConfig
import com.blackboxpro.forge.util.getBooleanOrDefault
import com.blackboxpro.forge.util.getDoubleOrDefault
import com.blackboxpro.forge.util.getIntOrDefault
import com.blackboxpro.forge.util.requireDouble
import com.blackboxpro.forge.util.pathfinding.NavigationController
import com.blackboxpro.forge.util.pathfinding.Pathfinder
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.util.math.BlockPos
import kotlin.math.floor
import kotlin.math.sqrt

class NavigateToAction : ActionExecutor {

    override fun execute(params: JsonObject): ActionResult {
        val targetX = params.requireDouble("x")
        val targetY = params.requireDouble("y")
        val targetZ = params.requireDouble("z")
        val speed = params.getDoubleOrDefault("speed", 1.0).coerceIn(0.1, 2.0)
        val navConfig = RuntimeBlackBoxConfig.current.navigation
        val timeout = params.getIntOrDefault("timeout", navConfig.defaultTimeout)
        val allowJump = params.getBooleanOrDefault("allowJump", true)

        val mc = Minecraft.getMinecraft()
        val player = mc.player
            ?: return ActionResult.fail("Player not available")
        val world = mc.world
            ?: return ActionResult.fail("World not available")

        val dx = targetX - player.posX
        val dy = targetY - player.posY
        val dz = targetZ - player.posZ
        val distance = sqrt(dx * dx + dy * dy + dz * dz)

        if (distance > navConfig.maxDistance) {
            return ActionResult.fail("Target too far: ${"%.1f".format(distance)} > ${navConfig.maxDistance}")
        }

        if (distance < navConfig.arrivalThreshold) {
            return ActionResult.ok("Already at target")
        }

        val start = BlockPos(player.posX.toInt(), player.posY.toInt(), player.posZ.toInt())
        val goal = BlockPos(floor(targetX).toInt(), floor(targetY).toInt(), floor(targetZ).toInt())

        val config = if (allowJump) navConfig else navConfig.copy(jumpCost = 999.0)

        val path = Pathfinder.findPath(start, goal, world, config)
        if (path.isEmpty()) {
            return ActionResult.fail("No path found to ($targetX, $targetY, $targetZ)")
        }

        val controller = NavigationController(path, speed, timeout, navConfig)
        controller.start()

        val estimatedTicks = (path.size / (navConfig.stepSize * speed)).toInt()
        val data = JsonObject().apply {
            addProperty("pathLength", path.size)
            addProperty("estimatedTicks", estimatedTicks)
            addProperty("startX", player.posX)
            addProperty("startY", player.posY)
            addProperty("startZ", player.posZ)
        }

        return ActionResult.ok("Navigating to ($targetX, $targetY, $targetZ), path length=${path.size}", data)
    }
}
