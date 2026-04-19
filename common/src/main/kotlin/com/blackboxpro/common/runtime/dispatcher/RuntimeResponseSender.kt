package com.blackboxpro.common.runtime.dispatcher

import com.google.gson.JsonObject

object RuntimeResponseSender {

    interface Sender {
        fun sendResponse(id: String, status: String, message: String? = null, data: JsonObject? = null)
    }

    private object NoopSender : Sender {
        override fun sendResponse(id: String, status: String, message: String?, data: JsonObject?) = Unit
    }

    @Volatile
    private var sender: Sender = NoopSender

    fun bind(sender: Sender) {
        this.sender = sender
    }

    fun sendResponse(id: String, status: String, message: String? = null, data: JsonObject? = null) {
        sender.sendResponse(id, status, message, data)
    }
}
