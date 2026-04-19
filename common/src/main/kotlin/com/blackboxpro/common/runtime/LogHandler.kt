package com.blackboxpro.common.runtime

/**
 * Functional logging interface for common module.
 * Platforms bind their actual logger implementation at startup.
 */
interface LogHandler {
    fun info(message: String, vararg args: Any?)
    fun debug(message: String, vararg args: Any?)
    fun warn(message: String, vararg args: Any?)
    fun error(message: String, vararg args: Any?)
}

/**
 * Supplier that provides platform-specific LogHandler by name.
 * Bound by each platform (Fabric/NeoForge/Forge) at startup.
 */
interface LoggerSupplier {
    fun getLogger(name: String): LogHandler
}
