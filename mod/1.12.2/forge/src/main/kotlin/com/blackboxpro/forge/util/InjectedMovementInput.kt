package com.blackboxpro.forge.util

import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.util.MovementInput

/**
 * 自定义 MovementInput 子类，用于注入模拟键盘输入。
 * 替换 MovementInputFromOptions 后，每 tick 的 [updatePlayerMoveState] 会从注入的布尔值计算移动向量，
 * 而不是从真实按键状态读取。
 *
 * 使用方式：
 * 1. [install] 安装到玩家，保存原始 MovementInput
 * 2. 每 tick 通过 [forward]/[sprint]/[jumping] 等属性设置期望输入
 * 3. 移动完成后调用 [uninstall] 恢复原始 MovementInputFromOptions
 */
class InjectedMovementInput : MovementInput() {

    /** 原始 MovementInput（通常是 MovementInputFromOptions），卸载时恢复 */
    private var original: MovementInput? = null

    /** 安装时的 player 引用，用于卸载时恢复 */
    private var installedPlayer: EntityPlayerSP? = null

    var forward: Boolean = false
    var backward: Boolean = false
    var left: Boolean = false
    var right: Boolean = false
    var jumping: Boolean = false
    var sneaking: Boolean = false

    override fun updatePlayerMoveState() {
        // 从注入的布尔值计算移动向量（与 MovementInputFromOptions 逻辑一致）
        moveForward = 0.0f
        moveStrafe = 0.0f

        if (forward) moveForward++
        if (backward) moveForward--
        if (left) moveStrafe++
        if (right) moveStrafe--

        jump = jumping
        sneak = sneaking

        forwardKeyDown = forward
        backKeyDown = backward
        leftKeyDown = left
        rightKeyDown = right

        if (sneak) {
            moveStrafe *= 0.3f
            moveForward *= 0.3f
        }
    }

    /** 安装到玩家，替换原始 MovementInput。若当前已有其他 InjectedMovementInput，先卸载以恢复原始 input。 */
    fun install(player: EntityPlayerSP) {
        val currentInput = player.movementInput
        if (currentInput is InjectedMovementInput && currentInput !== this) {
            currentInput.uninstall(player)
        }
        original = player.movementInput
        installedPlayer = player
        player.movementInput = this
    }

    /** 卸载，恢复原始 MovementInput。优先使用安装时保存的 player 引用。 */
    fun uninstall(player: EntityPlayerSP? = null) {
        val target = player ?: installedPlayer
        original?.let { orig ->
            if (target != null && target.movementInput === this) {
                target.movementInput = orig
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
        jumping = false
        sneaking = false
    }
}
