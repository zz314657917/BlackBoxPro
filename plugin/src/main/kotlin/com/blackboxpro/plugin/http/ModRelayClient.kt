package com.blackboxpro.plugin.http

import com.blackboxpro.common.protocol.CommandMessage
import com.blackboxpro.common.protocol.HttpEndpoints
import com.blackboxpro.common.protocol.ResponseMessage
import com.blackboxpro.plugin.config.BlackBoxSettings
import com.google.gson.Gson
import taboolib.common.platform.function.warning
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object ModRelayClient {

    private val gson = Gson()

    fun forward(command: CommandMessage, timeoutMs: Long = BlackBoxSettings.responseTimeoutMs): ResponseMessage {
        val json = gson.toJson(command)
        val address = BlackBoxSettings.modHttpAddress
        val url = "$address${HttpEndpoints.EXECUTE}"
        val connection = URL(url).openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connection.doOutput = true
            connection.connectTimeout = 5000
            connection.readTimeout = (timeoutMs + 2000).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()

            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { it.write(json) }

            val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            gson.fromJson(body, ResponseMessage::class.java)
                ?: ResponseMessage(command.id, "failure", "Empty response from mod")
        } catch (e: Exception) {
            warning("[BlackBoxPro] Mod relay failed for action '${command.action}': ${e.message}")
            ResponseMessage(command.id, "failure", "Mod relay failed: ${e.message}")
        } finally {
            connection.disconnect()
        }
    }
}
