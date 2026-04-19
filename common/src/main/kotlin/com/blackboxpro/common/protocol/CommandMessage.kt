package com.blackboxpro.common.protocol

import com.google.gson.JsonObject

data class CommandMessage(
    val id: String,
    val action: String,
    val params: JsonObject = JsonObject(),
    val delay: Long = 0L,
    val target: String? = null
)
