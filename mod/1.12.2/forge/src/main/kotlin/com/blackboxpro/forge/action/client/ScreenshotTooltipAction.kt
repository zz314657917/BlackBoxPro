package com.blackboxpro.forge.action.client

import com.blackboxpro.common.runtime.config.RuntimeBlackBoxConfig
import com.blackboxpro.common.runtime.dispatcher.RuntimeResponseSender
import com.blackboxpro.common.runtime.scheduler.RuntimeTickScheduler
import com.blackboxpro.common.runtime.screenshot.RuntimeScreenshotBridge
import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.blackboxpro.forge.util.ContainerTooltipHelper
import com.blackboxpro.forge.util.requireInt
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.client.util.ITooltipFlag
import java.nio.file.Files
import java.nio.file.Path

/**
 * 在容器界面中悬停指定槽位，手动渲染 tooltip 到帧缓冲，然后截图。
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

        val mc = Minecraft.getMinecraft()
        val screen = mc.currentScreen as? GuiContainer
            ?: return ActionResult.fail("No container screen open")

        val hoverState = try {
            ContainerTooltipHelper.hoverSlot(windowId, slotIndex)
        } catch (e: Exception) {
            return ActionResult.fail(e.message ?: "Failed to hover slot")
        }

        if (!hoverState.visible || hoverState.snapshot == null) {
            return ActionResult.fail("Slot $slotIndex is empty, no tooltip to render")
        }

        val container = mc.player?.openContainer ?: return ActionResult.fail("No container open")
        val slot = container.inventorySlots[slotIndex]
        val player = mc.player ?: return ActionResult.fail("Player not available")
        val tooltipLines = slot.stack.getTooltip(player, ITooltipFlag.TooltipFlags.NORMAL)

        val mouseX = hoverState.mouseX.toInt()
        val mouseY = hoverState.mouseY.toInt()

        RuntimeTickScheduler.schedule(1) {
            try {
                val currentScreen = mc.currentScreen as? GuiContainer
                if (currentScreen == null) {
                    RuntimeResponseSender.sendResponse(commandId, "failure", "Screen closed before screenshot")
                    return@schedule
                }

                // ScaledResolution 构造函数会更新 MC 内部的缩放分辨率缓存，确保 drawScreen 使用正确的缩放
                ScaledResolution(mc)
                currentScreen.drawScreen(mouseX, mouseY, mc.renderPartialTicks)
                currentScreen.drawHoveringText(tooltipLines, mouseX, mouseY)

                captureScreenshot(commandId, testId, prefix, hoverState)
            } catch (e: Exception) {
                RuntimeResponseSender.sendResponse(commandId, "failure", "Render failed: ${e.message}")
            }
        }

        return ActionResult.async()
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

    companion object {
        private val INDEX_PATTERN = Regex("^(\\d+).*\\.png$")
        private val SANITIZE_PATTERN = Regex("[^a-zA-Z0-9_\\-.]")
    }
}
