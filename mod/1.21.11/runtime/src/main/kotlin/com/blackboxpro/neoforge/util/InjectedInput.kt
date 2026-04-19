package com.blackboxpro.neoforge.util

import net.minecraft.client.player.ClientInput
import net.minecraft.client.player.LocalPlayer
import net.minecraft.world.entity.player.Input
import net.minecraft.world.phys.Vec2

/**
 * 自定义 ClientInput 子类，用于注入模拟键盘输入。
 * 替换 KeyboardInput 后，每 tick 的 tick() 会从注入的 [keyPresses] 计算 moveVector，
 * 而不是从真实按键状态读取。
 *
 * 使用方式：
 * 1. [install] 安装到玩家，保存原始 ClientInput
 * 2. 每 tick 通过 [forward]/[sprint]/[jump] 等属性设置期望输入
 * 3. 移动完成后调用 [uninstall] 恢复原始 KeyboardInput
 */
class InjectedInput : ClientInput() {

    /** 原始 ClientInput（通常是 KeyboardInput），卸载时恢复 */
    private var original: ClientInput? = null

    /** 安装时的 player 引用，用于卸载时恢复 */
    private var installedPlayer: LocalPlayer? = null

    var forward: Boolean = false
    var backward: Boolean = false
    var left: Boolean = false
    var right: Boolean = false
    var jump: Boolean = false
    var shift: Boolean = false
    var sprinting: Boolean = false

    override fun tick() {
        // 从注入的布尔值构建 Input (Mojang 映射)
        keyPresses = Input(forward, backward, left, right, jump, shift, sprinting)

        // 计算 moveVector（与 KeyboardInput.tick() 逻辑一致）
        val forwardValue = getMovementMultiplier(forward, backward)
        val sidewaysValue = getMovementMultiplier(left, right)
        moveVector = Vec2(sidewaysValue, forwardValue).normalized()
    }

    /** 安装到玩家，替换原始 ClientInput。若当前已有其他 InjectedInput，先卸载以恢复原始 input。 */
    fun install(player: LocalPlayer) {
        val currentInput = player.input
        if (currentInput is InjectedInput && currentInput !== this) {
            currentInput.uninstall(player)
        }
        original = player.input
        installedPlayer = player
        player.input = this
    }

    /** 卸载，恢复原始 ClientInput。优先使用安装时保存的 player 引用。 */
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

    /** 重置所有输入为默认值 */
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
