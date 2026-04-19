package com.blackboxpro.plugin.api.action

import com.blackboxpro.plugin.api.BlackBoxApi
import com.blackboxpro.common.protocol.ResponseMessage
import com.google.gson.JsonObject
import org.bukkit.entity.Player
import java.util.Base64
import java.util.concurrent.CompletableFuture

/**
 * 调试与特殊操作行为快捷 API。
 */
object DebugActions {

    fun customPayload(player: Player, channel: String, data: ByteArray): CompletableFuture<ResponseMessage> =
        BlackBoxApi.sendAsync(player, "custom_payload", JsonObject().apply {
            addProperty("channel", channel)
            addProperty("data", Base64.getEncoder().encodeToString(data))
        })

    fun tabComplete(player: Player, transactionId: Int, text: String): CompletableFuture<ResponseMessage> =
        BlackBoxApi.sendAsync(player, "tab_complete", JsonObject().apply {
            addProperty("transactionId", transactionId); addProperty("text", text)
        })

    fun keepAlive(player: Player, id: Long): CompletableFuture<ResponseMessage> =
        BlackBoxApi.sendAsync(player, "keep_alive", JsonObject().apply {
            addProperty("id", id)
        })

    fun pong(player: Player, parameter: Int): CompletableFuture<ResponseMessage> =
        BlackBoxApi.sendAsync(player, "pong", JsonObject().apply {
            addProperty("parameter", parameter)
        })

    fun debugSampleSubscription(player: Player, type: String): CompletableFuture<ResponseMessage> =
        BlackBoxApi.sendAsync(player, "debug_sample_subscription", JsonObject().apply {
            addProperty("type", type)
        })

    fun chunkBatchReceived(player: Player, desiredChunksPerTick: Float = 7.0f): CompletableFuture<ResponseMessage> =
        BlackBoxApi.sendAsync(player, "chunk_batch_received", JsonObject().apply {
            addProperty("desiredChunksPerTick", desiredChunksPerTick)
        })
}
