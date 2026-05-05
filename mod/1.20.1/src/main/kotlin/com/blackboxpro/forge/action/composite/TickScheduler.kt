package com.blackboxpro.forge.action.composite

import com.blackboxpro.common.runtime.scheduler.RuntimeTickScheduler
import net.minecraftforge.client.event.ClientPlayerNetworkEvent
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.TickEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import org.slf4j.LoggerFactory

object TickScheduler {

    private val logger = LoggerFactory.getLogger("BlackBoxPro-TickScheduler")

    fun init() {
        MinecraftForge.EVENT_BUS.register(this)
    }

    @SubscribeEvent
    fun onClientTick(event: TickEvent.ClientTickEvent) {
        if (event.phase == TickEvent.Phase.END) {
            RuntimeTickScheduler.tick()
        }
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
