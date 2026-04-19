package com.blackboxpro.common.runtime.screenshot

import java.nio.file.Path

object RuntimeScreenshotBridge {

    data class CaptureResult(
        val filePath: Path,
        val width: Int,
        val height: Int,
        val fileSize: Long
    )

    interface Provider {
        fun currentPlayerName(): String?
        fun gameDirectory(): Path?
        fun captureAsync(directory: Path, fileName: String, callback: (Result<CaptureResult>) -> Unit)
    }

    private object NoopProvider : Provider {
        override fun currentPlayerName(): String? = null
        override fun gameDirectory(): Path? = null
        override fun captureAsync(directory: Path, fileName: String, callback: (Result<CaptureResult>) -> Unit) {
            callback(Result.failure(IllegalStateException("Screenshot provider not bound")))
        }
    }

    @Volatile
    private var provider: Provider = NoopProvider

    fun bind(provider: Provider) {
        this.provider = provider
    }

    fun currentPlayerName(): String? = provider.currentPlayerName()

    fun gameDirectory(): Path? = provider.gameDirectory()

    fun captureAsync(directory: Path, fileName: String, callback: (Result<CaptureResult>) -> Unit) {
        provider.captureAsync(directory, fileName, callback)
    }
}
