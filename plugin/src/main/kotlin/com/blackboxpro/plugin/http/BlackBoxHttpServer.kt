package com.blackboxpro.plugin.http

import com.blackboxpro.common.http.HttpConfig
import com.blackboxpro.common.protocol.HttpEndpoints
import com.blackboxpro.plugin.config.BlackBoxSettings
import com.sun.net.httpserver.HttpServer
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.function.info
import taboolib.common.platform.function.warning
import java.net.InetSocketAddress
import java.util.concurrent.Executors

object BlackBoxHttpServer {

    private var server: HttpServer? = null

    @Awake(LifeCycle.ENABLE)
    fun enable() {
        val port = BlackBoxSettings.httpPort
        try {
            val s = HttpServer.create(InetSocketAddress(port), 0)
            s.createContext(HttpEndpoints.EXECUTE, ExecuteHandler)
            s.createContext(HttpEndpoints.STATUS, StatusHandler)
            s.executor = Executors.newCachedThreadPool { r ->
                Thread(r, "BlackBoxPro-Http").apply { isDaemon = true }
            }
            s.start()
            server = s
            info("[BlackBoxPro] HTTP server started on port $port (mode: ${BlackBoxSettings.testMode})")
        } catch (e: Exception) {
            warning("[BlackBoxPro] Failed to start HTTP server on port $port: ${e.message}")
        }
    }

    @Awake(LifeCycle.DISABLE)
    fun disable() {
        server?.stop(0)
        server = null
        info("[BlackBoxPro] HTTP server stopped.")
    }
}
