package com.blackboxpro.plugin.api.action

import com.blackboxpro.plugin.api.BlackBoxApi
import com.blackboxpro.common.protocol.ResponseMessage
import com.google.gson.JsonObject
import org.bukkit.entity.Player
import java.util.concurrent.CompletableFuture

/**
 * 截图行为快捷 API。
 */
object ScreenshotActions {

    /**
     * 触发客户端截图。
     *
     * @param player 目标玩家
     * @param testId 测试会话 ID，用于目录隔离
     * @param prefix 可选的文件名前缀
     * @param playerName 可选的玩家名覆盖（默认使用客户端当前玩家名）
     */
    fun screenshot(
        player: Player,
        testId: String = "default",
        prefix: String? = null,
        playerName: String? = null
    ): CompletableFuture<ResponseMessage> =
        BlackBoxApi.sendAsync(player, "screenshot", JsonObject().apply {
            addProperty("testId", testId)
            if (prefix != null) addProperty("prefix", prefix)
            if (playerName != null) addProperty("playerName", playerName)
        })
}
