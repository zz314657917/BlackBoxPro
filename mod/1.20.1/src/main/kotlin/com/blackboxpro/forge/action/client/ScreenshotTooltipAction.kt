package com.blackboxpro.forge.action.client

import com.blackboxpro.common.runtime.action.ActionResult
import com.blackboxpro.common.runtime.config.RuntimeBlackBoxConfig
import com.blackboxpro.common.runtime.dispatcher.RuntimeResponseSender
import com.blackboxpro.common.runtime.scheduler.RuntimeTickScheduler
import com.blackboxpro.common.runtime.screenshot.RuntimeScreenshotBridge
import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.util.ContainerTooltipHelper
import com.blackboxpro.forge.util.requireInt
import com.blackboxpro.runtime.bindings.ScreenRenderBridge
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.world.item.TooltipFlag
import java.nio.file.Files
import java.nio.file.Path
import java.util.Optional
import java.util.concurrent.atomic.AtomicBoolean

class ScreenshotTooltipAction : ActionExecutor {

    override fun execute(params: JsonObject): ActionResult =
        ActionResult.fail("screenshot_tooltip requires commandId")

    override fun execute(params: JsonObject, commandId: String): ActionResult {
        val windowId = params.requireInt("windowId")
        val slotIndex = params.requireInt("slot")
        val testId = params.get("testId")?.asString ?: "default"
        val prefix = params.get("prefix")?.asString ?: "tooltip"

        val client = Minecraft.getInstance()
        val screen = client.screen as? AbstractContainerScreen<*>
            ?: return ActionResult.fail("No container screen open")

        val hoverState = try {
            ContainerTooltipHelper.hoverSlot(windowId, slotIndex)
        } catch (e: Exception) {
            return ActionResult.fail(e.message ?: "Failed to hover slot")
        }

        if (!hoverState.visible || hoverState.snapshot == null) {
            return ActionResult.fail("Slot $slotIndex is empty, no tooltip to render")
        }

        val slot = screen.menu.slots[slotIndex]
        val player = client.player ?: return ActionResult.fail("Player not available")
        val tooltipLines = slot.item.getTooltipLines(player, TooltipFlag.Default.NORMAL)
        val mouseX = hoverState.mouseX.toInt()
        val mouseY = hoverState.mouseY.toInt()

        val fired = AtomicBoolean(false)
        ScreenRenderBridge.registerOneShotAfterRender { drawContextAny ->
            if (!fired.compareAndSet(false, true)) return@registerOneShotAfterRender
            val guiGraphics = drawContextAny as GuiGraphics
            try {
                guiGraphics.renderTooltip(client.font, tooltipLines, Optional.empty(), mouseX, mouseY)
                captureScreenshot(commandId, testId, prefix, hoverState)
            } catch (e: Exception) {
                RuntimeResponseSender.sendResponse(commandId, "failure", "Render failed: ${e.message}")
            }
        }

        RuntimeTickScheduler.schedule(20) {
            if (fired.compareAndSet(false, true)) {
                RuntimeResponseSender.sendResponse(commandId, "failure", "Render timeout: screen closed before tooltip could be rendered")
            }
        }

        return ActionResult.async()
    }

    companion object {
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
}
