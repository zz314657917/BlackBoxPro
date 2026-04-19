package com.blackboxpro.plugin.command.testframework

import com.blackboxpro.plugin.api.BlackBoxApi
import com.blackboxpro.plugin.api.action.ScreenshotActions
import com.blackboxpro.common.protocol.ResponseMessage
import com.google.gson.JsonObject
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import taboolib.common.platform.function.submit
import java.util.concurrent.CompletableFuture

class BlackBoxTestContext(
    val player: Player,
    val sender: CommandSender,
    val testId: String,
    val loaderProfile: BlackBoxLoaderProfile,
    val fixtureManager: BlackBoxFixtureManager = BlackBoxFixtureManager(player, loaderProfile)
) {

    fun sendAction(actionId: String, params: JsonObject = JsonObject(), timeoutMs: Long = 5000L): CompletableFuture<ResponseMessage> =
        BlackBoxApi.sendAsync(player, actionId, params, timeoutMs = timeoutMs)

    fun screenshot(prefix: String): CompletableFuture<Unit> =
        ScreenshotActions.screenshot(player, testId, prefix, player.name)
            .thenApply { }
            .exceptionally { }

    fun delay(ms: Long): CompletableFuture<Unit> {
        val future = CompletableFuture<Unit>()
        val ticks = (ms / 50).coerceAtLeast(1)
        submit(async = true, delay = ticks) { future.complete(Unit) }
        return future
    }

    fun mainThread(block: () -> Unit): CompletableFuture<Unit> {
        val future = CompletableFuture<Unit>()
        submit(async = false) {
            try { block() } catch (e: Exception) { future.completeExceptionally(e); return@submit }
            future.complete(Unit)
        }
        return future
    }
}
