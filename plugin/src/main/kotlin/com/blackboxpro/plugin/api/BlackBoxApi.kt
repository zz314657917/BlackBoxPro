package com.blackboxpro.plugin.api

import com.blackboxpro.common.protocol.CommandMessage
import com.blackboxpro.common.protocol.ResponseMessage
import com.blackboxpro.plugin.config.BlackBoxSettings
import com.blackboxpro.plugin.http.ModRelayClient
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.function.Consumer

object BlackBoxApi {

    private val executor = Executors.newCachedThreadPool { r ->
        Thread(r, "BlackBoxPro-Http").apply { isDaemon = true }
    }

    fun send(player: Player, action: String, params: JsonObject = JsonObject(), delay: Long = 0L) {
        sendAsync(player, action, params, delay)
    }

    fun send(player: Player, action: String, params: JsonObject = JsonObject(), delay: Long = 0L, callback: Consumer<ResponseMessage>) {
        sendAsync(player, action, params, delay).thenAccept { callback.accept(it) }
    }

    fun sendAsync(
        player: Player,
        action: String,
        params: JsonObject = JsonObject(),
        delay: Long = 0L,
        timeoutMs: Long = BlackBoxSettings.responseTimeoutMs
    ): CompletableFuture<ResponseMessage> {
        val command = CommandMessage(
            id = UUID.randomUUID().toString(),
            action = action,
            params = params,
            delay = delay,
            target = player.name
        )
        return CompletableFuture.supplyAsync({
            ModRelayClient.forward(command, timeoutMs)
        }, executor)
    }

    fun shutdown() {
        executor.shutdownNow()
    }

    fun sendBatch(player: Player, actions: List<Pair<String, JsonObject>>) {
        val batchParams = JsonObject().apply {
            add("actions", JsonArray().apply {
                actions.forEach { (action, params) ->
                    add(JsonObject().apply {
                        addProperty("action", action)
                        add("params", params)
                    })
                }
            })
        }
        send(player, "batch", batchParams)
    }
}
