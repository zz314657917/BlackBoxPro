package com.blackboxpro.neoforge.util

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.mojang.serialization.JsonOps
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.ComponentSerialization
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentLinkedDeque

/**
 * 聊天消息环形缓冲区。
 * 通过 NeoForge ClientChatReceivedEvent 捕获消息，供 query_chat_history 查询。
 */
object ChatHistoryBuffer {

    private val logger = LoggerFactory.getLogger("BlackBoxPro-ChatHistory")
    private const val MAX_SIZE = 200

    data class ChatEntry(
        val timestamp: Long,
        val raw: String,
        val plain: String,
        val type: String
    )

    private val buffer = ConcurrentLinkedDeque<ChatEntry>()

    fun addMessage(message: Component, type: String) {
        val raw = serializeMessage(message)
        val plain = message.string
        buffer.addLast(ChatEntry(System.currentTimeMillis(), raw, plain, type))
        while (buffer.size > MAX_SIZE) buffer.pollFirst()
    }

    fun query(count: Int, filter: String?, since: Long?): JsonObject {
        var stream = buffer.toList().asSequence()
        if (since != null) stream = stream.filter { it.timestamp >= since }
        if (filter != null) stream = stream.filter { it.plain.contains(filter) }
        val messages = stream.toList().takeLast(count.coerceIn(1, 100))

        val arr = JsonArray()
        messages.forEach { entry ->
            arr.add(JsonObject().apply {
                addProperty("timestamp", entry.timestamp)
                addProperty("raw", entry.raw)
                addProperty("plain", entry.plain)
                addProperty("type", entry.type)
            })
        }
        return JsonObject().apply {
            add("messages", arr)
            addProperty("total", buffer.size)
        }
    }

    fun getEntry(index: Int): ChatEntry? {
        if (index < 0) return null
        val entries = buffer.toList()
        val targetIndex = entries.lastIndex - index
        return entries.getOrNull(targetIndex)
    }

    fun clear() = buffer.clear()

    private fun serializeMessage(message: Component): String = runCatching {
        val client = Minecraft.getInstance()
        val ops = client.connection?.registryAccess()?.createSerializationContext(JsonOps.INSTANCE) ?: JsonOps.INSTANCE
        ComponentSerialization.CODEC.encodeStart(ops, message).result().orElse(JsonPrimitive(message.string)).toString()
    }.getOrElse {
        JsonPrimitive(message.string).toString()
    }
}
