package com.blackboxpro.forge.util.pathfinding

import com.blackboxpro.common.runtime.scheduler.RuntimeTickScheduler
import com.blackboxpro.common.runtime.config.RuntimeNavigationConfig
import com.blackboxpro.forge.util.InjectedInput
import net.minecraft.client.Minecraft
import org.slf4j.LoggerFactory
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * 璺緞璺熼殢鎺у埗鍣ㄣ€?
 * 姣?tick 閫氳繃 [InjectedInput] 娉ㄥ叆 forward 杈撳叆 + yaw 杞悜锛岄┍鍔ㄧ帺瀹舵部 A* 璺緞绉诲姩锛?
 * MC 鐗╃悊寮曟搸鑷姩澶勭悊纰版挒/閲嶅姏/閫熷害銆?
 */
class NavigationController(
    private val path: List<PathNode>,
    private val speed: Double,
    private val timeout: Int,
    private val config: RuntimeNavigationConfig
) {
    private val logger = LoggerFactory.getLogger("BlackBoxPro-Navigation")
    private var currentIndex = 1 // 璺宠繃璧风偣
    private var ticksElapsed = 0
    private var active = false
    private var injected: InjectedInput? = null

    fun start() {
        if (path.size < 2) return
        val player = Minecraft.getInstance().player ?: return
        val input = InjectedInput()
        input.install(player)
        injected = input
        active = true
        RuntimeTickScheduler.schedule(1) { tick() }
    }

    fun stop() {
        if (!active) return
        active = false
        injected?.let { input ->
            input.reset()
            input.uninstall()
            Minecraft.getInstance().player?.isSprinting = false
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
        val client = Minecraft.getInstance()
        val player = client.player ?: run { stop(); return }
        val input = injected ?: run { stop(); return }

        if (currentIndex >= path.size) {
            logger.debug("Navigation completed in {} ticks", ticksElapsed)
            stop()
            return
        }

        var target = path[currentIndex]
        val targetX = target.x + 0.5
        val targetZ = target.z + 0.5

        var dx = targetX - player.x
        var dz = targetZ - player.z
        val horizontalDist = sqrt(dx * dx + dz * dz)

        // 鍒拌揪褰撳墠璺緞鐐?鈫?鍓嶈繘鍒颁笅涓€鑺傜偣
        if (horizontalDist < config.nodeArrivalThreshold) {
            currentIndex++
            if (currentIndex >= path.size) {
                logger.debug("Navigation completed in {} ticks", ticksElapsed)
                stop()
                return
            }
            target = path[currentIndex]
            dx = target.x + 0.5 - player.x
            dz = target.z + 0.5 - player.z
        }

        // 杞悜鐩爣鑺傜偣
        val yaw = (-atan2(dx, dz) * 180.0 / Math.PI).toFloat()
        player.yRot = yaw

        // 娉ㄥ叆杈撳叆
        input.forward = true
        input.jump = target.jumpRequired && player.onGround() && target.y > player.y.toInt()
        input.sprinting = speed > 1.0
        player.isSprinting = speed > 1.0

        RuntimeTickScheduler.schedule(1) { tick() }
    }
}

