package com.blackboxpro.plugin.http

import com.blackboxpro.plugin.BlackBoxPro
import com.blackboxpro.plugin.config.BlackBoxSettings
import com.google.gson.JsonObject
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import taboolib.common.platform.function.warning

object StatusHandler : HttpHandler {

    override fun handle(exchange: HttpExchange) {
        try {
            val obj = JsonObject().apply {
                addProperty("version", BlackBoxPro.VERSION)
                addProperty("mode", BlackBoxSettings.testMode)
                addProperty("httpPort", BlackBoxSettings.httpPort)
                addProperty("modHttpAddress", BlackBoxSettings.modHttpAddress)
                addProperty("ready", true)
            }
            val bytes = obj.toString().toByteArray(Charsets.UTF_8)
            exchange.responseHeaders.set("Content-Type", "application/json; charset=utf-8")
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        } catch (e: Exception) {
            warning("[BlackBoxPro] StatusHandler error: ${e.message}")
        } finally {
            exchange.close()
        }
    }
}
