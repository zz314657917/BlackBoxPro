package com.blackboxpro.forge.dispatcher

import com.blackboxpro.forge.http.ResponseFutureRegistry
import com.blackboxpro.runtime.bindings.LoggerSupplierBinding
import com.blackboxpro.runtime.bindings.SharedSlf4jLoggerSupplier
import com.blackboxpro.runtime.dispatcher.RuntimeCommandDispatcherBootstrap
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraftforge.client.event.ClientPlayerNetworkEvent
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.TickEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import java.util.concurrent.ConcurrentLinkedQueue

object CommandDispatcher {

    private val logger = SharedSlf4jLoggerSupplier.getLogger("BlackBoxPro-Dispatcher")
    private val pendingMainThreadTasks = ConcurrentLinkedQueue<() -> Unit>()

    fun init() {
        RuntimeCommandDispatcherBootstrap.bind(
            loggerSupplier = LoggerSupplierBinding,
            executeOnMainThread = { task ->
                pendingMainThreadTasks.add(task)
            },
            actionResolver = ActionRegistry::find,
            sendResponseJson = ResponseFutureRegistry::onResponseJson
        )

        MinecraftForge.EVENT_BUS.register(this)
    }

    @SubscribeEvent
    fun onClientTick(event: TickEvent.ClientTickEvent) {
        if (event.phase == TickEvent.Phase.END) {
            drainMainThreadTasks()
            RuntimeCommandDispatcherBootstrap.tick()
        }
    }

    @SubscribeEvent
    fun onDisconnect(event: ClientPlayerNetworkEvent.LoggingOut) {
        val queuedCount = clearPendingMainThreadTasks()
        val count = RuntimeCommandDispatcherBootstrap.clear()
        if (queuedCount > 0) {
            logger.info("Cleared {} queued main-thread tasks on disconnect", queuedCount)
        }
        if (count > 0) {
            logger.info("Cleared {} delayed commands on disconnect", count)
        }
    }

    fun dispatch(message: CommandMessage) {
        RuntimeCommandDispatcherBootstrap.dispatch(message)
    }

    internal fun sendResponse(
        id: String,
        status: String,
        message: String? = null,
        data: JsonObject? = null
    ) {
        RuntimeCommandDispatcherBootstrap.sendResponse(id, status, message, data)
    }

    private fun drainMainThreadTasks() {
        while (true) {
            val task = pendingMainThreadTasks.poll() ?: break
            task()
        }
    }

    private fun clearPendingMainThreadTasks(): Int {
        var count = 0
        while (pendingMainThreadTasks.poll() != null) {
            count++
        }
        return count
    }
}
