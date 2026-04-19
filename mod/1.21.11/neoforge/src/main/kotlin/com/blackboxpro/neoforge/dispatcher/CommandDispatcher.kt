package com.blackboxpro.neoforge.dispatcher

import com.blackboxpro.neoforge.http.ResponseFutureRegistry
import com.blackboxpro.runtime.bindings.LoggerSupplierBinding
import com.blackboxpro.runtime.bindings.SharedSlf4jLoggerSupplier
import com.blackboxpro.runtime.dispatcher.RuntimeCommandDispatcherBootstrap
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent
import net.neoforged.neoforge.client.event.ClientTickEvent
import net.neoforged.neoforge.common.NeoForge

object CommandDispatcher {

    private val logger = SharedSlf4jLoggerSupplier.getLogger("BlackBoxPro-Dispatcher")

    fun init() {
        RuntimeCommandDispatcherBootstrap.bind(
            loggerSupplier = LoggerSupplierBinding,
            executeOnMainThread = { task -> Minecraft.getInstance().execute(task) },
            actionResolver = ActionRegistry::find,
            sendResponseJson = ResponseFutureRegistry::onResponseJson
        )

        NeoForge.EVENT_BUS.register(this)
    }

    @SubscribeEvent
    fun onClientTick(event: ClientTickEvent.Post) {
        RuntimeCommandDispatcherBootstrap.tick()
    }

    @SubscribeEvent
    fun onDisconnect(event: ClientPlayerNetworkEvent.LoggingOut) {
        val count = RuntimeCommandDispatcherBootstrap.clear()
        if (count > 0) {
            logger.info("Cleared {} delayed commands on disconnect", count)
        }
    }

    fun dispatch(message: CommandMessage) {
        RuntimeCommandDispatcherBootstrap.dispatch(message)
    }

    /** 供异步 Action 自行发送响应，不要在普通 Action 中调用 */
    internal fun sendResponse(
        id: String,
        status: String,
        message: String? = null,
        data: JsonObject? = null
    ) {
        RuntimeCommandDispatcherBootstrap.sendResponse(id, status, message, data)
    }
}
