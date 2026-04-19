package com.blackboxpro.fabric.action.client

import com.blackboxpro.common.runtime.action.ActionResult
import com.blackboxpro.common.runtime.config.RuntimeBlackBoxConfig
import com.blackboxpro.common.runtime.dispatcher.RuntimeResponseSender
import com.blackboxpro.common.runtime.scheduler.RuntimeTickScheduler
import com.blackboxpro.common.runtime.screenshot.RuntimeScreenshotBridge
import com.blackboxpro.fabric.action.ActionExecutor
import com.blackboxpro.fabric.util.ContainerTooltipHelper
import com.blackboxpro.fabric.util.requireInt
import com.google.gson.JsonObject
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.client.gui.tooltip.HoveredTooltipPositioner
import net.minecraft.client.gui.tooltip.TooltipComponent
import net.minecraft.item.Item
import net.minecraft.item.tooltip.TooltipType
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 在容器界面中悬停指定槽位，通过 afterRender 注入 tooltip 绘制到 GuiRenderState，
 * 然后在 Mixin 注入的 post-guiRenderer 时机截图。
 *
 * 流程：
 * 1. hoverSlot 移动光标 + 获取 tooltip 数据
 * 2. afterRender 回调中用 drawTooltipImmediately() 将 tooltip 记录到 GuiRenderState
 * 3. 设置 pendingScreenshot 标记
 * 4. GameRendererMixin 在 guiRenderer.render() 之后检测标记，执行截图
 *
 * Action ID: "screenshot_tooltip"
 */
class ScreenshotTooltipAction : ActionExecutor {

    override fun execute(params: JsonObject): ActionResult =
        ActionResult.fail("screenshot_tooltip requires commandId")

    override fun execute(params: JsonObject, commandId: String): ActionResult {
        val windowId = params.requireInt("windowId")
        val slotIndex = params.requireInt("slot")
        val testId = params.get("testId")?.asString ?: "default"
        val prefix = params.get("prefix")?.asString ?: "tooltip"

        val client = MinecraftClient.getInstance()
        val screen = client.currentScreen as? HandledScreen<*>
            ?: return ActionResult.fail("No container screen open")

        // 1. hover 移动光标 + 获取 tooltip 数据
        val hoverState = try {
            ContainerTooltipHelper.hoverSlot(windowId, slotIndex)
        } catch (e: Exception) {
            return ActionResult.fail(e.message ?: "Failed to hover slot")
        }

        if (!hoverState.visible || hoverState.snapshot == null) {
            return ActionResult.fail("Slot $slotIndex is empty, no tooltip to render")
        }

        // 2. 获取 tooltip 文本行
        val handler = screen.screenHandler
        val slot = handler.slots[slotIndex]
        val player = client.player ?: return ActionResult.fail("Player not available")
        val tooltipLines = client.world?.let { world ->
            slot.stack.getTooltip(Item.TooltipContext.create(world), player, TooltipType.BASIC)
        } ?: Screen.getTooltipFromItem(client, slot.stack)

        // 转换为 TooltipComponent 列表
        val components = tooltipLines.map { TooltipComponent.of(it.asOrderedText()) }

        val mouseX = hoverState.mouseX.toInt()
        val mouseY = hoverState.mouseY.toInt()

        // 3. 注册 afterRender 回调：用 drawTooltipImmediately 直接记录到 GuiRenderState
        val fired = AtomicBoolean(false)
        ScreenEvents.afterRender(screen).register(ScreenEvents.AfterRender { _, drawContext, _, _, _ ->
            if (!fired.compareAndSet(false, true)) return@AfterRender

            try {
                // drawTooltipImmediately 直接写入 GuiRenderState，不走延迟机制
                drawContext.drawTooltipImmediately(
                    client.textRenderer,
                    components,
                    mouseX, mouseY,
                    HoveredTooltipPositioner.INSTANCE,
                    null
                )

                // 4. 设置截图标记，Mixin 注入点会在 guiRenderer.render() 后执行截图
                pendingScreenshot = PendingScreenshot(commandId, testId, prefix, hoverState)
            } catch (e: Exception) {
                RuntimeResponseSender.sendResponse(commandId, "failure", "Render failed: ${e.message}")
            }
        })

        // 超时保护：如果 screen 在下一帧渲染前关闭，回调不会触发
        RuntimeTickScheduler.schedule(20) {
            if (fired.compareAndSet(false, true)) {
                RuntimeResponseSender.sendResponse(commandId, "failure", "Render timeout: screen closed before tooltip could be rendered")
            }
        }

        return ActionResult.async()
    }

    companion object {
        @Volatile
        private var pendingScreenshot: PendingScreenshot? = null

        /**
         * 由 GameRendererMixin 在 guiRenderer.render() 之后调用。
         * 此时主帧缓冲包含完整的 screen + tooltip。
         */
        fun onPostGuiRender() {
            val pending = pendingScreenshot ?: return
            pendingScreenshot = null

            try {
                captureScreenshot(pending.commandId, pending.testId, pending.prefix, pending.hoverState)
            } catch (e: Exception) {
                RuntimeResponseSender.sendResponse(pending.commandId, "failure", "Screenshot failed: ${e.message}")
            }
        }

        private fun captureScreenshot(
            commandId: String,
            testId: String,
            prefix: String,
            hoverState: ContainerTooltipHelper.TooltipState
        ) {
            val playerName = RuntimeScreenshotBridge.currentPlayerName() ?: "unknown"
            val baseDirectory = RuntimeScreenshotBridge.gameDirectory() ?: run {
                RuntimeResponseSender.sendResponse(commandId, "failure", "Game directory not available")
                return
            }
            val screenshotConfig = RuntimeBlackBoxConfig.current.screenshot
            val directory = baseDirectory
                .resolve(screenshotConfig.rootDirectory)
                .resolve(sanitize(playerName))
                .resolve(sanitize(testId))

            val index = nextIndex(directory)
            val indexStr = index.toString().padStart(3, '0')
            val fileName = "${indexStr}_${sanitize(prefix)}"

            RuntimeScreenshotBridge.captureAsync(directory, fileName) { result ->
                result.fold(
                    onSuccess = { screenshot ->
                        val data = hoverState.toQueryJson().apply {
                            val relativePath = baseDirectory.relativize(screenshot.filePath)
                            addProperty("filePath", relativePath.toString().replace('\\', '/'))
                            addProperty("width", screenshot.width)
                            addProperty("height", screenshot.height)
                            addProperty("fileSize", screenshot.fileSize)
                            addProperty("index", index)
                        }
                        RuntimeResponseSender.sendResponse(commandId, "success", "Tooltip screenshot saved: $fileName.png", data)
                    },
                    onFailure = { error ->
                        RuntimeResponseSender.sendResponse(commandId, "failure", "Screenshot failed: ${error.message}")
                    }
                )
            }
        }

        private fun nextIndex(directory: Path): Int {
            if (!Files.exists(directory)) return 1
            val files = directory.toFile().listFiles() ?: return 1
            return (files.mapNotNull { INDEX_PATTERN.matchEntire(it.name)?.groupValues?.get(1)?.toIntOrNull() }
                .maxOrNull() ?: 0) + 1
        }

        private fun sanitize(name: String): String = name.replace(SANITIZE_PATTERN, "_")

        private val INDEX_PATTERN = Regex("^(\\d+).*\\.png$")
        private val SANITIZE_PATTERN = Regex("[^a-zA-Z0-9_\\-.]")
    }

    private data class PendingScreenshot(
        val commandId: String,
        val testId: String,
        val prefix: String,
        val hoverState: ContainerTooltipHelper.TooltipState
    )
}
