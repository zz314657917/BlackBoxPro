package com.blackboxpro.forge

import com.blackboxpro.common.protocol.ResponseMessage
import com.blackboxpro.common.runtime.config.RuntimeBlackBoxConfig
import com.blackboxpro.common.runtime.dispatcher.RuntimeCommandDispatcher
import com.blackboxpro.common.runtime.dispatcher.RuntimeResponseSender
import com.blackboxpro.forge.action.composite.TickScheduler
import com.blackboxpro.forge.dispatcher.ActionRegistry
import com.blackboxpro.forge.dispatcher.CommandDispatcher
import com.blackboxpro.forge.http.ModHttpServer
import com.blackboxpro.forge.http.ResponseFutureRegistry
import com.blackboxpro.forge.util.ChatHistoryBuffer
import com.blackboxpro.runtime.bindings.ForgeBindings
import com.google.gson.Gson
import net.minecraft.client.Minecraft
import net.minecraftforge.client.event.ClientChatReceivedEvent
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.event.FMLInitializationEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import org.apache.logging.log4j.LogManager

@Mod(
    modid = BlackBoxProForge.MOD_ID,
    name = "BlackBoxPro Forge",
    version = BlackBoxProForge.VERSION,
    clientSideOnly = true,
    modLanguageAdapter = "com.blackboxpro.forge.KotlinAdapter"
)
object BlackBoxProForge {

    const val MOD_ID = "blackboxpro"
    const val VERSION = "1.0.0"

    private val logger = LogManager.getLogger("BlackBoxProForge")
    private val gson = Gson()

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

    @Mod.EventHandler
    fun init(event: FMLInitializationEvent) {
        ForgeBindings.init()

        RuntimeResponseSender.bind(object : RuntimeResponseSender.Sender {
            override fun sendResponse(id: String, status: String, message: String?, data: com.google.gson.JsonObject?) {
                ResponseFutureRegistry.onResponseJson(gson.toJson(ResponseMessage(id, status, message, data)))
            }
        })

        loadRuntimeConfig()
        ActionRegistry.registerAll()

        RuntimeCommandDispatcher.bind(
            loggerSupplier = ForgeBindings,
            mainThreadExecutor = object : RuntimeCommandDispatcher.MainThreadExecutor {
                override fun execute(task: () -> Unit) {
                    Minecraft.getMinecraft().addScheduledTask(task)
                }
            },
            actionResolver = object : RuntimeCommandDispatcher.ActionResolver {
                override fun find(actionId: String) = ActionRegistry.find(actionId)
            }
        )

        MinecraftForge.EVENT_BUS.register(CommandDispatcher)
        MinecraftForge.EVENT_BUS.register(TickScheduler)
        MinecraftForge.EVENT_BUS.register(this)

        ModHttpServer.start()
        Runtime.getRuntime().addShutdownHook(Thread { ModHttpServer.stop() })

        logger.info("BlackBoxProForge v{} loaded. {} actions registered.", VERSION, ActionRegistry.size())
    }

    @SubscribeEvent
    fun onChatReceived(event: ClientChatReceivedEvent) {
        val typeName = when (event.type.ordinal) {
            0 -> "CHAT"
            1 -> "SYSTEM"
            2 -> "ACTION_BAR"
            else -> "UNKNOWN"
        }
        ChatHistoryBuffer.addMessage(event.message, typeName)
    }
}
