package com.blackboxpro.forge.util

import net.minecraft.client.Minecraft
import net.minecraft.util.ScreenShotHelper
import org.apache.logging.log4j.LogManager
import java.awt.image.BufferedImage
import java.io.File
import java.nio.file.Path
import javax.imageio.ImageIO

object ScreenshotHelper {

    private val logger = LogManager.getLogger("BlackBoxPro-Screenshot")
    private val INDEX_PATTERN = Regex("^(\\d{3}).*\\.png$")
    private val SANITIZE_PATTERN = Regex("[^a-zA-Z0-9_\\-.]")

    data class ScreenshotResult(
        val filePath: Path,
        val width: Int,
        val height: Int,
        val fileSize: Long
    )

    fun capture(directory: Path, fileName: String): ScreenshotResult {
        val mc = Minecraft.getMinecraft()
        val fb = mc.framebuffer
        val width = mc.displayWidth
        val height = mc.displayHeight

        val image: BufferedImage = ScreenShotHelper.createScreenshot(width, height, fb)

        val dir = directory.toFile()
        dir.mkdirs()
        val file = File(dir, "$fileName.png")
        ImageIO.write(image, "png", file)

        val fileSize = file.length()
        logger.info("Screenshot saved: {} ({}x{}, {} bytes)", file.absolutePath, width, height, fileSize)

        return ScreenshotResult(file.toPath(), width, height, fileSize)
    }

    fun nextIndex(directory: Path): Int {
        val dir = directory.toFile()
        if (!dir.exists()) return 1
        val files = dir.listFiles() ?: return 1
        var maxIdx = 0
        for (f in files) {
            val m = INDEX_PATTERN.matchEntire(f.name) ?: continue
            val idx = m.groupValues[1].toIntOrNull() ?: continue
            if (idx > maxIdx) maxIdx = idx
        }
        return maxIdx + 1
    }

    fun sanitize(name: String): String =
        name.replace(SANITIZE_PATTERN, "_")
}
