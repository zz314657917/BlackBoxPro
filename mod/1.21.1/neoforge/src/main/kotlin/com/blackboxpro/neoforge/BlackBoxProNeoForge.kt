package com.blackboxpro.neoforge

import com.blackboxpro.neoforge.action.composite.TickScheduler
import com.blackboxpro.neoforge.dispatcher.ActionRegistry
import com.blackboxpro.neoforge.dispatcher.CommandDispatcher
import com.blackboxpro.neoforge.http.ModHttpServer
import com.blackboxpro.neoforge.util.ChatHistoryBuffer
import com.blackboxpro.neoforge.util.NeoForgeRuntimeScreenshotProvider
import com.blackboxpro.common.runtime.screenshot.RuntimeScreenshotBridge
import net.neoforged.bus.api.IEventBus
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.Mod
import net.neoforged.neoforge.client.event.ClientChatReceivedEvent
import net.neoforged.neoforge.common.NeoForge
import org.slf4j.LoggerFactory

@Mod("blackboxpro")
class BlackBoxProNeoForge(modBus: IEventBus) {

    init {
        val logger = LoggerFactory.getLogger("BlackBoxProNeoForge")

        // 1. 加载配置

        // 2. 绑定运行时桥接
        RuntimeScreenshotBridge.bind(NeoForgeRuntimeScreenshotProvider)

        // 3. 注册所有行为执行器
        ActionRegistry.registerAll()

        // 3. 初始化调度器
        CommandDispatcher.init()

        // 4. 初始化 Tick 调度器（复合行为用）
        TickScheduler.init()

        // 5. 启动 HTTP Server
        ModHttpServer.start()

        // 6. 注册聊天消息监听器（供 query_chat_history 使用）
        NeoForge.EVENT_BUS.register(ChatEventListener)

        logger.info("BlackBoxProNeoForge v{} loaded. {} actions registered.", VERSION, ActionRegistry.size())
    }

    companion object {
        const val VERSION = "1.0.0"
    }

    private object ChatEventListener {
        @SubscribeEvent
        fun onSystemChat(event: ClientChatReceivedEvent.System) {
            val type = if (event.isOverlay()) "ACTION_BAR" else "SYSTEM"
            ChatHistoryBuffer.addMessage(event.message, type)
        }

        @SubscribeEvent
        fun onPlayerChat(event: ClientChatReceivedEvent.Player) {
            ChatHistoryBuffer.addMessage(event.message, "CHAT")
        }
    }
}
