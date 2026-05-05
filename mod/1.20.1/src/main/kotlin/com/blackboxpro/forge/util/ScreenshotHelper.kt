package com.blackboxpro.forge.util

import com.mojang.blaze3d.platform.NativeImage
import net.minecraft.client.Minecraft
import net.minecraft.client.Screenshot
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import java.util.function.Consumer

/**
 * 鎴浘宸ュ叿绫汇€?
 */
object ScreenshotHelper {

    private val logger = LoggerFactory.getLogger("BlackBoxPro-Screenshot")
    private val INDEX_PATTERN = Regex("^(\\d{3}).*\\.png$")
    private val SANITIZE_PATTERN = Regex("[^a-zA-Z0-9_\\-.]")

    data class ScreenshotResult(
        val filePath: Path,
        val width: Int,
        val height: Int,
        val fileSize: Long
    )

    /**
     * 鎹曡幏褰撳墠甯х紦鍐插苟淇濆瓨涓?PNG銆傚繀椤诲湪涓荤嚎绋嬭皟鐢ㄣ€?
     */
    fun captureAsync(directory: Path, fileName: String, callback: Consumer<Result<ScreenshotResult>>) {
        try {
            Files.createDirectories(directory)
            val framebuffer = Minecraft.getInstance().mainRenderTarget
            val image: NativeImage = Screenshot.takeScreenshot(framebuffer)
            image.use { nativeImage ->
                val filePath = directory.resolve("$fileName.png")
                nativeImage.writeToFile(filePath)
                val fileSize = Files.size(filePath)
                val width = nativeImage.getWidth()
                val height = nativeImage.getHeight()
                logger.info("Screenshot saved: {} ({}x{}, {} bytes)", filePath, width, height, fileSize)
                callback.accept(Result.success(ScreenshotResult(filePath, width, height, fileSize)))
            }
        } catch (e: Exception) {
            callback.accept(Result.failure(e))
        }
    }

    /**
     * 璁＄畻鐩綍涓嬬殑涓嬩竴涓埅鍥剧紪鍙枫€?
     */
    fun nextIndex(directory: Path): Int {
        if (!Files.exists(directory)) return 1
        val files = directory.toFile().listFiles() ?: return 1
        return (files.mapNotNull { INDEX_PATTERN.matchEntire(it.name)?.groupValues?.get(1)?.toIntOrNull() }
            .maxOrNull() ?: 0) + 1
    }

    /**
     * 娓呮礂鏂囦欢鍚嶏紝绉婚櫎鏂囦欢绯荤粺涓嶅厑璁哥殑瀛楃銆?
     */
    fun sanitize(name: String): String =
        name.replace(SANITIZE_PATTERN, "_")
}

