package com.blackboxpro.forge.http

import com.blackboxpro.common.runtime.config.RuntimeBlackBoxConfig
import com.blackboxpro.forge.BlackBoxProForge
import com.blackboxpro.forge.dispatcher.ActionRegistry
import com.google.gson.JsonObject
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import org.slf4j.LoggerFactory

object StatusHandler : HttpHandler {

    private val logger = LoggerFactory.getLogger("BlackBoxPro-Http")

    override fun handle(exchange: HttpExchange) {
        try {
            val obj = JsonObject().apply {
                addProperty("status", "running")
                addProperty("version", BlackBoxProForge.VERSION)
                addProperty("platform", "forge")
                addProperty("httpPort", RuntimeBlackBoxConfig.current.network.httpPort)
                addProperty("actions", ActionRegistry.size())
                addProperty("ready", true)
            }
            val bytes = obj.toString().toByteArray(Charsets.UTF_8)
            exchange.responseHeaders.set("Content-Type", "application/json; charset=utf-8")
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        } catch (e: Exception) {
            logger.error("StatusHandler error", e)
        } finally {
            exchange.close()
        }
    }
}

