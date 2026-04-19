package com.blackboxpro.neoforge.util

import net.minecraft.client.player.Input
import net.minecraft.client.player.LocalPlayer

/**
 * 自定义 Input 子类，用于注入模拟键盘输入。
 */
class InjectedInput : Input() {

    private var original: Input? = null
    private var installedPlayer: LocalPlayer? = null

    var forward: Boolean = false
    var backward: Boolean = false
    var leftPressed: Boolean = false
    var rightPressed: Boolean = false
    var jump: Boolean = false
    var shift: Boolean = false
    var sprinting: Boolean = false

    override fun tick(isSneaking: Boolean, sneakingSpeedMultiplier: Float) {
        up = forward
        down = backward
        left = leftPressed
        right = rightPressed
        jumping = jump
        shiftKeyDown = shift

        forwardImpulse = getMovementMultiplier(forward, backward)
        leftImpulse = getMovementMultiplier(this.left, this.right)
        if (isSneaking) {
            forwardImpulse *= sneakingSpeedMultiplier
            leftImpulse *= sneakingSpeedMultiplier
        }
    }

    fun install(player: LocalPlayer) {
        original = player.input
        installedPlayer = player
        player.input = this
    }

    fun uninstall(player: LocalPlayer? = null) {
        val target = player ?: installedPlayer
        original?.let { orig ->
            if (target != null && target.input === this) {
                target.input = orig
            }
        }
        original = null
        installedPlayer = null
    }

    fun reset() {
        forward = false
        backward = false
        left = false
        right = false
        jump = false
        shift = false
        sprinting = false
    }

    companion object {
        private fun getMovementMultiplier(positive: Boolean, negative: Boolean): Float =
            if (positive == negative) 0.0f else if (positive) 1.0f else -1.0f
    }
}
