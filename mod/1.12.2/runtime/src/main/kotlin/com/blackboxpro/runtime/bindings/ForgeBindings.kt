package com.blackboxpro.runtime.bindings

import com.blackboxpro.common.runtime.LogHandler
import com.blackboxpro.common.runtime.LoggerSupplier
import com.blackboxpro.common.runtime.screenshot.RuntimeScreenshotBridge
import net.minecraft.client.Minecraft
import net.minecraft.util.ScreenShotHelper
import org.apache.logging.log4j.LogManager
import java.nio.file.Path
import java.util.concurrent.Executors
import javax.imageio.ImageIO


/**
 * Forge 1.12.2 platform bindings for common runtime.
 */
object ForgeBindings : LoggerSupplier {

    private val logger = LogManager.getLogger("BlackBoxPro-ForgeBindings")
    internal val screenshotExecutor = Executors.newSingleThreadExecutor { r ->
        Thread(r, "BlackBoxPro-Screenshot").apply { isDaemon = true }
    }

    val screenshotProvider = ForgeScreenshotProvider()

    init {
        RuntimeScreenshotBridge.bind(screenshotProvider)
        logger.info("ForgeBindings initialized with screenshot provider")
    }

    override fun getLogger(name: String): LogHandler = Log4jLogHandler(LogManager.getLogger(name))

    fun init() = Unit
}

private class Log4jLogHandler(private val logger: org.apache.logging.log4j.Logger) : LogHandler {
    override fun info(message: String, vararg args: Any?) = logger.info(message, *args)
    override fun debug(message: String, vararg args: Any?) = logger.debug(message, *args)
    override fun warn(message: String, vararg args: Any?) = logger.warn(message, *args)
    override fun error(message: String, vararg args: Any?) = logger.error(message, *args)
}

/**
 * Forge 1.12.2 screenshot provider for RuntimeScreenshotBridge.
 */
class ForgeScreenshotProvider : RuntimeScreenshotBridge.Provider {

    override fun currentPlayerName(): String? =
        Minecraft.getMinecraft().session.username

    override fun gameDirectory(): Path? =
        Minecraft.getMinecraft().gameDir.toPath()

    override fun captureAsync(directory: Path, fileName: String, callback: (Result<RuntimeScreenshotBridge.CaptureResult>) -> Unit) {
        try {
            val mc = Minecraft.getMinecraft()
            // glReadPixels 必须在渲染线程（同步部分）
            val image = ScreenShotHelper.createScreenshot(mc.displayWidth, mc.displayHeight, mc.framebuffer)
            // 文件 I/O 移到异步线程，避免阻塞主线程
            ForgeBindings.screenshotExecutor.execute {
                try {
                    directory.toFile().mkdirs()
                    val file = java.io.File(directory.toFile(), "$fileName.png")
                    ImageIO.write(image, "png", file)
                    image.flush()
                    callback(Result.success(
                        RuntimeScreenshotBridge.CaptureResult(
                            filePath = file.toPath(),
                            width = image.width,
                            height = image.height,
                            fileSize = file.length()
                        )
                    ))
                } catch (e: Exception) {
                    callback(Result.failure(e))
                }
            }
        } catch (e: Exception) {
            callback(Result.failure(e))
        }
    }
}
