package com.blackboxpro.plugin.api.action

import com.blackboxpro.plugin.api.BlackBoxApi
import com.blackboxpro.common.protocol.ResponseMessage
import com.google.gson.JsonObject
import org.bukkit.entity.Player
import java.util.concurrent.CompletableFuture

/**
 * 聊天与命令行为快捷 API。
 */
object ChatActions {

    fun chatMessage(player: Player, message: String): CompletableFuture<ResponseMessage> =
        BlackBoxApi.sendAsync(player, "chat_message", JsonObject().apply {
            addProperty("message", message)
        })

    fun chatCommand(player: Player, command: String): CompletableFuture<ResponseMessage> =
        BlackBoxApi.sendAsync(player, "chat_command", JsonObject().apply {
            addProperty("command", command)
        })
}
