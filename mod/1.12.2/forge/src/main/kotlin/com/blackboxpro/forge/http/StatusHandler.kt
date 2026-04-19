package com.blackboxpro.forge.http

import com.blackboxpro.common.runtime.config.RuntimeBlackBoxConfig
import com.blackboxpro.forge.dispatcher.ActionRegistry
import com.google.gson.JsonObject
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import net.minecraftforge.fml.common.Loader
import org.apache.logging.log4j.LogManager

object StatusHandler : HttpHandler {

    private val logger = LogManager.getLogger("BlackBoxPro-Http")
    private val runtimeVersion: String by lazy {
        Loader.instance().activeModList
            .firstOrNull { it.modId == "blackboxpro" }
            ?.version ?: "unknown"
    }

    override fun handle(exchange: HttpExchange) {
        try {
            val mc = net.minecraft.client.Minecraft.getMinecraft()
            val obj = JsonObject().apply {
                addProperty("status", "running")
                addProperty("version", runtimeVersion)
                addProperty("platform", "forge")
                addProperty("httpPort", RuntimeBlackBoxConfig.current.network.httpPort)
                addProperty("actions", ActionRegistry.size())
                addProperty("ready", mc.player != null && mc.world != null)
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
