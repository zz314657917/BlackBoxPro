package com.blackboxpro.forge.dispatcher

import com.blackboxpro.common.runtime.config.RuntimeBlackBoxConfig
import com.blackboxpro.forge.http.ResponseFutureRegistry
import com.google.gson.Gson
import net.minecraft.client.Minecraft
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import net.minecraftforge.fml.common.network.FMLNetworkEvent
import org.apache.logging.log4j.LogManager

object CommandDispatcher {

    private val logger = LogManager.getLogger("BlackBoxPro-Dispatcher")
    private val gson = Gson()

    private data class DelayedCommand(
        val message: CommandMessage,
        var remainingTicks: Int
    )

    private val delayedQueue = ArrayDeque<DelayedCommand>()

    @SubscribeEvent
    fun onClientTick(event: TickEvent.ClientTickEvent) {
        if (event.phase != TickEvent.Phase.END) return
        val iterator = delayedQueue.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            entry.remainingTicks--
            if (entry.remainingTicks <= 0) {
                iterator.remove()
                executeOnMainThread(entry.message)
            }
        }
    }

    @SubscribeEvent
    fun onDisconnect(event: FMLNetworkEvent.ClientDisconnectionFromServerEvent) {
        val count = delayedQueue.size
        delayedQueue.clear()
        if (count > 0) {
            logger.info("Cleared {} delayed commands on disconnect", count)
        }
    }

    fun dispatch(message: CommandMessage) {
        logger.info("Dispatching action: {} (id={})", message.action, message.id)

        if (!isActionAllowed(message.action)) {
            sendResponse(message.id, "failure", "Action blocked by safety config: ${message.action}")
            return
        }

        if (message.delay > 0) {
            val maxDelay = RuntimeBlackBoxConfig.current.execution.maxDelayTicks
            val ticks = (message.delay / 50).toInt().coerceAtLeast(1).coerceAtMost(maxDelay)
            logger.debug("Delaying action {} for {} ticks", message.action, ticks)
            Minecraft.getMinecraft().addScheduledTask {
                delayedQueue.addLast(DelayedCommand(message, ticks))
            }
        } else {
            executeOnMainThread(message)
        }
    }

    private fun executeOnMainThread(message: CommandMessage) {
        Minecraft.getMinecraft().addScheduledTask {
            val executor = ActionRegistry.find(message.action)
            if (executor == null) {
                logger.warn("Unknown action: {}", message.action)
                sendResponse(message.id, "failure", "Unknown action: ${message.action}")
                return@addScheduledTask
            }

            try {
                val result = executor.execute(message.params, message.id)
                if (!result.async) {
                    sendResponse(
                        id = message.id,
                        status = if (result.success) "success" else "failure",
                        message = result.message,
                        data = result.data
                    )
                }
            } catch (e: IllegalArgumentException) {
                logger.warn("Invalid params for action {}: {}", message.action, e.message)
                sendResponse(message.id, "failure", "Invalid params: ${e.message}")
            } catch (e: Throwable) {
                logger.error("Action {} threw exception", message.action, e)
                sendResponse(message.id, "failure", "Exception: ${e.message}")
            }
        }
    }

    private fun isActionAllowed(actionId: String): Boolean {
        val config = RuntimeBlackBoxConfig.current.safety
        if (!config.enabled) return true
        if (actionId in config.blockedActions) return false
        if (config.allowedActions.isEmpty()) return true
        return actionId in config.allowedActions
    }

    private fun sendResponse(
        id: String,
        status: String,
        message: String? = null,
        data: com.google.gson.JsonObject? = null
    ) {
        val response = ResponseMessage(id, status, message, data)
        val json = gson.toJson(response)
        ResponseFutureRegistry.onResponseJson(json)
    }
}
