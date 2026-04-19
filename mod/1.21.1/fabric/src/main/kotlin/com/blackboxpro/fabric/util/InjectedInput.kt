package com.blackboxpro.fabric.util

import net.minecraft.client.input.Input
import net.minecraft.client.network.ClientPlayerEntity

/**
 * 自定义 Input 子类，用于注入模拟键盘输入。
 * 替换 KeyboardInput 后，每 tick 会从注入的布尔值更新到 Minecraft 的输入字段。
 */
class InjectedInput : Input() {

    /** 原始 Input（通常是 KeyboardInput），卸载时恢复 */
    private var original: Input? = null

    /** 安装时的 player 引用，用于卸载时恢复 */
    private var installedPlayer: ClientPlayerEntity? = null

    var forward: Boolean = false
    var backward: Boolean = false
    var left: Boolean = false
    var right: Boolean = false
    var sprinting: Boolean = false

    override fun tick(slowDown: Boolean, slowDownFactor: Float) {
        pressingForward = forward
        pressingBack = backward
        pressingLeft = left
        pressingRight = right
        super.tick(slowDown, slowDownFactor)
    }

    /** 安装到玩家，替换原始 Input */
    fun install(player: ClientPlayerEntity) {
        original = player.input
        installedPlayer = player
        player.input = this
    }

    /** 卸载，恢复原始 Input。优先使用安装时保存的 player 引用。 */
    fun uninstall(player: ClientPlayerEntity? = null) {
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
        jumping = false
        sneaking = false
        sprinting = false
    }
}
