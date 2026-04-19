package com.blackboxpro.forge.action.composite

import com.blackboxpro.common.runtime.scheduler.RuntimeTickScheduler
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import net.minecraftforge.fml.common.network.FMLNetworkEvent
import org.apache.logging.log4j.LogManager

object TickScheduler {

    private val logger = LogManager.getLogger("BlackBoxPro-TickScheduler")

    @SubscribeEvent
    fun onClientTick(event: TickEvent.ClientTickEvent) {
        if (event.phase != TickEvent.Phase.END) return
        RuntimeTickScheduler.tick()
    }

    @SubscribeEvent
    fun onDisconnect(event: FMLNetworkEvent.ClientDisconnectionFromServerEvent) {
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
