package com.blackboxpro.neoforge.action.composite

import com.blackboxpro.common.runtime.scheduler.RuntimeTickScheduler
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent
import net.neoforged.neoforge.client.event.ClientTickEvent
import net.neoforged.neoforge.common.NeoForge
import org.slf4j.LoggerFactory

object TickScheduler {

    private val logger = LoggerFactory.getLogger("BlackBoxPro-TickScheduler")

    fun init() {
        NeoForge.EVENT_BUS.register(this)
    }

    @SubscribeEvent
    fun onClientTick(event: ClientTickEvent.Post) {
        RuntimeTickScheduler.tick()
    }

    @SubscribeEvent
    fun onDisconnect(event: ClientPlayerNetworkEvent.LoggingOut) {
        val count = RuntimeTickScheduler.clear()
        if (count > 0) {
            logger.info("Cleared {} scheduled tasks on disconnect", count)
        }
    }

    fun schedule(delayTicks: Int, task: () -> Unit) {
        RuntimeTickScheduler.schedule(delayTicks, task)
    }

    fun clear() {
        RuntimeTickScheduler.clear()
    }
}
