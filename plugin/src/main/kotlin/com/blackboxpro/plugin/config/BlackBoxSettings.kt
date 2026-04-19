package com.blackboxpro.plugin.config

import taboolib.module.configuration.Config
import taboolib.module.configuration.Configuration

object BlackBoxSettings {

    @Config("config.yml")
    lateinit var conf: Configuration
        private set

    val debug: Boolean get() = conf.getBoolean("debug", false)

    val responseTimeoutMs: Long get() = conf.getLong("response-timeout-ms", 10000L)

    val httpPort: Int get() = conf.getInt("http-port", 38080)

    val testMode: String get() = conf.getString("test-mode", "dual") ?: "dual"

    val modHttpAddress: String get() = conf.getString("mod-http-address", "http://localhost:38081") ?: "http://localhost:38081"
}
