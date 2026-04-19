package com.blackboxpro.common.action.client

import com.blackboxpro.common.runtime.action.ActionExecutor
import com.blackboxpro.common.runtime.action.ActionResult
import com.blackboxpro.common.runtime.config.RuntimeBlackBoxConfig
import com.blackboxpro.common.runtime.dispatcher.RuntimeResponseSender
import com.blackboxpro.common.runtime.screenshot.RuntimeScreenshotBridge
import com.google.gson.JsonObject
import java.nio.file.Files
import java.nio.file.Path

class ScreenshotAction : ActionExecutor {

    override fun execute(params: JsonObject): ActionResult =
        ActionResult.fail("ScreenshotAction requires commandId, use execute(params, commandId)")

    override fun execute(params: JsonObject, commandId: String): ActionResult {
        val playerName = params.getStringOrNull("playerName")
            ?: RuntimeScreenshotBridge.currentPlayerName()
            ?: return ActionResult.fail("Player not available")
        val baseDirectory = RuntimeScreenshotBridge.gameDirectory()
            ?: return ActionResult.fail("Game directory not available")
        val testId = params.getStringOrNull("testId") ?: "default"
        val prefix = params.getStringOrNull("prefix")

        val screenshotConfig = RuntimeBlackBoxConfig.current.screenshot
        val directory = baseDirectory
            .resolve(screenshotConfig.rootDirectory)
            .resolve(sanitize(playerName))
            .resolve(sanitize(testId))

        val index = nextIndex(directory)
        if (index > screenshotConfig.maxPerTest) {
            return ActionResult.fail("Screenshot index overflow (max ${screenshotConfig.maxPerTest}) for testId=$testId")
        }
        val indexStr = index.toString().padStart(3, '0')
        val fileName = prefix?.let { "${indexStr}_${sanitize(it)}" } ?: indexStr

        RuntimeScreenshotBridge.captureAsync(directory, fileName) { result ->
            result.fold(
                onSuccess = { screenshot ->
                    val data = JsonObject().apply {
                        val relativePath = baseDirectory.relativize(screenshot.filePath)
                        addProperty("filePath", relativePath.toString().replace('\\', '/'))
                        addProperty("width", screenshot.width)
                        addProperty("height", screenshot.height)
                        addProperty("fileSize", screenshot.fileSize)
                        addProperty("index", index)
                    }
                    RuntimeResponseSender.sendResponse(commandId, "success", "Screenshot saved: $fileName.png", data)
                },
                onFailure = { error ->
                    RuntimeResponseSender.sendResponse(commandId, "failure", "Screenshot failed: ${error.message}")
                }
            )
        }

        return ActionResult.async()
    }

    private fun JsonObject.getStringOrNull(key: String): String? =
        get(key)?.takeIf { !it.isJsonNull }?.asString

    private fun nextIndex(directory: Path): Int {
        if (!Files.exists(directory)) return 1
        val files = directory.toFile().listFiles() ?: return 1
        return (files.mapNotNull { INDEX_PATTERN.matchEntire(it.name)?.groupValues?.get(1)?.toIntOrNull() }
            .maxOrNull() ?: 0) + 1
    }

    private fun sanitize(name: String): String =
        name.replace(SANITIZE_PATTERN, "_")

    companion object {
        private val INDEX_PATTERN = Regex("^(\\d+).*\\.png$")
        private val SANITIZE_PATTERN = Regex("[^a-zA-Z0-9_\\-.]")
    }
}
