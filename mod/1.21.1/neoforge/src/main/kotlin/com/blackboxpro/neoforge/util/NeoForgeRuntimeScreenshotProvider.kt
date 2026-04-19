package com.blackboxpro.neoforge.util

import com.blackboxpro.common.runtime.screenshot.RuntimeScreenshotBridge
import net.minecraft.client.Minecraft
import java.nio.file.Path

object NeoForgeRuntimeScreenshotProvider : RuntimeScreenshotBridge.Provider {

    override fun currentPlayerName(): String? =
        Minecraft.getInstance().player?.gameProfile?.name

    override fun gameDirectory(): Path? =
        Minecraft.getInstance().gameDirectory.toPath()

    override fun captureAsync(
        directory: Path,
        fileName: String,
        callback: (Result<RuntimeScreenshotBridge.CaptureResult>) -> Unit
    ) {
        ScreenshotHelper.captureAsync(directory, fileName) { result ->
            callback(
                result.map {
                    RuntimeScreenshotBridge.CaptureResult(
                        filePath = it.filePath,
                        width = it.width,
                        height = it.height,
                        fileSize = it.fileSize
                    )
                }
            )
        }
    }
}
