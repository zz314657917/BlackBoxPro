package com.blackboxpro.plugin.api.action

import com.blackboxpro.plugin.api.BlackBoxApi
import com.blackboxpro.common.protocol.ResponseMessage
import com.google.gson.JsonObject
import org.bukkit.entity.Player
import java.util.concurrent.CompletableFuture

/**
 * 导航与瞄准行为快捷 API。
 */
object NavigationActions {

    /**
     * 瞄准指定方块的指定面。
     */
    fun lookAtBlock(
        player: Player,
        x: Int, y: Int, z: Int,
        face: String = "top"
    ): CompletableFuture<ResponseMessage> =
        BlackBoxApi.sendAsync(player, "look_at_block", JsonObject().apply {
            addProperty("x", x); addProperty("y", y); addProperty("z", z)
            addProperty("face", face)
        })

    /**
     * AI 寻路移动到目标坐标。
     */
    fun navigateTo(
        player: Player,
        x: Double, y: Double, z: Double,
        speed: Double = 1.0,
        timeout: Int? = null,
        allowJump: Boolean = true
    ): CompletableFuture<ResponseMessage> =
        BlackBoxApi.sendAsync(player, "navigate_to", JsonObject().apply {
            addProperty("x", x); addProperty("y", y); addProperty("z", z)
            addProperty("speed", speed)
            if (timeout != null) addProperty("timeout", timeout)
            addProperty("allowJump", allowJump)
        }, timeoutMs = 30000)
}
