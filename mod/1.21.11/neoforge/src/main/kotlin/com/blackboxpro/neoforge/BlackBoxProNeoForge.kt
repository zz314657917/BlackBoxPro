package com.blackboxpro.neoforge

import com.blackboxpro.neoforge.action.composite.TickScheduler
import com.blackboxpro.neoforge.dispatcher.ActionRegistry
import com.blackboxpro.neoforge.dispatcher.CommandDispatcher
import com.blackboxpro.neoforge.http.ModHttpServer
import com.blackboxpro.neoforge.util.ChatHistoryBuffer
import com.blackboxpro.neoforge.util.NeoForgeRuntimeScreenshotProvider
import com.blackboxpro.common.runtime.screenshot.RuntimeScreenshotBridge
import com.blackboxpro.runtime.bindings.ScreenRenderBridge
import net.neoforged.bus.api.IEventBus
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.Mod
import net.neoforged.neoforge.client.event.ClientChatReceivedEvent
import net.neoforged.neoforge.client.event.ScreenEvent
import net.neoforged.neoforge.common.NeoForge
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean

@Mod("blackboxpro")
class BlackBoxProNeoForge(modBus: IEventBus) {

    init {
        val logger = LoggerFactory.getLogger("BlackBoxProNeoForge")

        // 1. 加载配置

        // 2. 绑定运行时桥接
        RuntimeScreenshotBridge.bind(NeoForgeRuntimeScreenshotProvider)
        ScreenRenderBridge.bind { callback ->
            val fired = AtomicBoolean(false)
            // 记录注册时的 screen 实例，防止 screen 切换后在错误的 screen 上触发
            val expectedScreen = net.minecraft.client.Minecraft.getInstance().screen
            val listener = object {
                @SubscribeEvent
                fun onRenderPost(event: ScreenEvent.Render.Post) {
                    if (expectedScreen != null && event.screen !== expectedScreen) return
                    if (fired.compareAndSet(false, true)) {
                        callback.onAfterRender(event.guiGraphics)
                        NeoForge.EVENT_BUS.unregister(this)
                    }
                }
            }
            NeoForge.EVENT_BUS.register(listener)
        }

        // 3. 注册所有行为执行器
        ActionRegistry.registerAll()

        // 4. 初始化调度器
        CommandDispatcher.init()

        // 5. 初始化 Tick 调度器（复合行为用）
        TickScheduler.init()

        // 6. 启动 HTTP Server
        ModHttpServer.start()
        Runtime.getRuntime().addShutdownHook(Thread { ModHttpServer.stop() })

        // 7. 注册聊天消息监听器（供 query_chat_history 使用）
        NeoForge.EVENT_BUS.register(ChatEventListener)

        logger.info("BlackBoxProNeoForge v{} loaded. {} actions registered.", VERSION, ActionRegistry.size())
    }

    companion object {
        val VERSION: String by lazy {
            try {
                net.neoforged.fml.ModList.get()
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
