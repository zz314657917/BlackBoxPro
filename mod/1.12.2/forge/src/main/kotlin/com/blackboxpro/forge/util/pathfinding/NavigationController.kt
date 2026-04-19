package com.blackboxpro.forge.util.pathfinding

import com.blackboxpro.forge.action.composite.TickScheduler
import com.blackboxpro.common.runtime.config.RuntimeNavigationConfig
import com.blackboxpro.forge.util.InjectedMovementInput
import net.minecraft.client.Minecraft
import org.apache.logging.log4j.LogManager
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * 路径跟随控制器。
 * 每 tick 通过 [InjectedMovementInput] 注入 forward 输入 + yaw 转向，驱动玩家沿 A* 路径移动，
 * MC 物理引擎自动处理碰撞/重力/速度。
 */
class NavigationController(
    private val path: List<PathNode>,
    private val speed: Double,
    private val timeout: Int,
    private val config: RuntimeNavigationConfig
) {
    private val logger = LogManager.getLogger("BlackBoxPro-Navigation")
    private var currentIndex = 1 // 跳过起点
    private var ticksElapsed = 0
    private var active = false
    private var injected: InjectedMovementInput? = null

    fun start() {
        if (path.size < 2) return
        val player = Minecraft.getMinecraft().player ?: return
        val input = InjectedMovementInput()
        input.install(player)
        injected = input
        active = true
        TickScheduler.schedule(1) { tick() }
    }

    fun stop() {
        if (!active) return
        active = false
        injected?.let { input ->
            input.reset()
            input.uninstall()
            Minecraft.getMinecraft().player?.isSprinting = false
        }
        injected = null
    }

    private fun tick() {
        if (!active) return
        if (ticksElapsed >= timeout) {
            logger.debug("Navigation timed out after {} ticks", ticksElapsed)
            stop()
            return
        }

        ticksElapsed++
        val mc = Minecraft.getMinecraft()
        val player = mc.player ?: run { stop(); return }
        val input = injected ?: run { stop(); return }

        if (currentIndex >= path.size) {
            logger.debug("Navigation completed in {} ticks", ticksElapsed)
            stop()
            return
        }

        var target = path[currentIndex]
        val targetX = target.x + 0.5
        val targetZ = target.z + 0.5

        var dx = targetX - player.posX
        var dz = targetZ - player.posZ
        val horizontalDist = sqrt(dx * dx + dz * dz)

        // 到达当前路径点 → 前进到下一节点
        if (horizontalDist < config.nodeArrivalThreshold) {
            currentIndex++
            if (currentIndex >= path.size) {
                logger.debug("Navigation completed in {} ticks", ticksElapsed)
                stop()
                return
            }
            target = path[currentIndex]
            dx = target.x + 0.5 - player.posX
            dz = target.z + 0.5 - player.posZ
        }

        // 转向目标节点
        val yaw = (-atan2(dx, dz) * 180.0 / Math.PI).toFloat()
        player.rotationYaw = yaw

        // 注入输入
        input.forward = true
        input.jumping = target.jumpRequired && player.onGround && target.y > player.posY.toInt()
        player.isSprinting = speed > 1.0

        TickScheduler.schedule(1) { tick() }
    }
}
