package com.blackboxpro.forge.util

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import net.minecraft.network.chat.Component
import java.util.concurrent.ConcurrentLinkedDeque

object ChatHistoryBuffer {

    private const val MAX_SIZE = 200

    data class ChatEntry(
        val timestamp: Long,
        val component: Component,
        val raw: String,
        val plain: String,
        val type: String,
    )

    private val buffer = ConcurrentLinkedDeque<ChatEntry>()

    fun addMessage(message: Component, type: String) {
        val plain = message.string
        buffer.addLast(ChatEntry(System.currentTimeMillis(), message, plain, plain, type))
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
}
