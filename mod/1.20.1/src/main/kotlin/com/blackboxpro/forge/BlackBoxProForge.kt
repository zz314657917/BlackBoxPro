package com.blackboxpro.forge

import com.blackboxpro.common.runtime.config.RuntimeBlackBoxConfig
import com.blackboxpro.common.runtime.screenshot.RuntimeScreenshotBridge
import com.blackboxpro.forge.action.composite.TickScheduler
import com.blackboxpro.forge.dispatcher.ActionRegistry
import com.blackboxpro.forge.dispatcher.CommandDispatcher
import com.blackboxpro.forge.http.ModHttpServer
import com.blackboxpro.forge.util.ChatHistoryBuffer
import com.blackboxpro.forge.util.ForgeRuntimeScreenshotProvider
import com.blackboxpro.runtime.bindings.ScreenRenderBridge
import net.minecraft.client.Minecraft
import net.minecraftforge.client.event.ClientChatReceivedEvent
import net.minecraftforge.client.event.ScreenEvent
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.ModList
import net.minecraftforge.fml.common.Mod
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean

@Mod("blackboxpro")
class BlackBoxProForge {

    private val logger = LoggerFactory.getLogger("BlackBoxProForge")

    private fun loadRuntimeConfig() {
        val current = RuntimeBlackBoxConfig.current
        val httpPort = System.getProperty("blackboxpro.httpPort")?.toIntOrNull()
            ?: current.network.httpPort
        val responseTimeoutMs = System.getProperty("blackboxpro.responseTimeoutMs")?.toLongOrNull()
            ?: current.network.responseTimeoutMs

        RuntimeBlackBoxConfig.update(
            current.copy(
                network = current.network.copy(
                    httpPort = httpPort,
                    responseTimeoutMs = responseTimeoutMs
                )
            )
        )

        logger.info(
            "Loaded runtime config: httpPort={}, responseTimeoutMs={}",
            httpPort,
            responseTimeoutMs
        )
    }

    init {
        RuntimeScreenshotBridge.bind(ForgeRuntimeScreenshotProvider)
        ScreenRenderBridge.bind { callback ->
            val fired = AtomicBoolean(false)
            val expectedScreen = Minecraft.getInstance().screen
            val listener = object {
                @SubscribeEvent
                fun onRenderPost(event: ScreenEvent.Render.Post) {
                    if (expectedScreen != null && event.screen !== expectedScreen) return
                    if (fired.compareAndSet(false, true)) {
                        callback.onAfterRender(event.guiGraphics)
                        MinecraftForge.EVENT_BUS.unregister(this)
                    }
                }
            }
            MinecraftForge.EVENT_BUS.register(listener)
        }

        loadRuntimeConfig()
        ActionRegistry.registerAll()
        CommandDispatcher.init()
        TickScheduler.init()

        ModHttpServer.start()
        Runtime.getRuntime().addShutdownHook(Thread { ModHttpServer.stop() })

        MinecraftForge.EVENT_BUS.register(ChatEventListener)
        logger.info("BlackBoxProForge v{} loaded. {} actions registered.", VERSION, ActionRegistry.size())
    }

    companion object {
        val VERSION: String by lazy {
            try {
                ModList.get()
                    .getModContainerById("blackboxpro")
                    .map { it.modInfo.version.toString() }
                    .orElse("unknown")
            } catch (_: Exception) {
                "unknown"
            }
        }
    }

    private object ChatEventListener {
        @SubscribeEvent
        fun onChat(event: ClientChatReceivedEvent) {
            val type = if (event.isSystem) "SYSTEM" else "CHAT"
            ChatHistoryBuffer.addMessage(event.message, type)
        }
    }
}
