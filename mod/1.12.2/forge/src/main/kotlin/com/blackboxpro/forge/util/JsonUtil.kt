package com.blackboxpro.forge.util

import com.google.gson.JsonObject

fun JsonObject.getStringOrNull(key: String): String? =
    if (has(key) && !get(key).isJsonNull) get(key).asString else null

fun JsonObject.getIntOrDefault(key: String, default: Int): Int =
    if (has(key) && !get(key).isJsonNull) get(key).asInt else default

fun JsonObject.getLongOrDefault(key: String, default: Long): Long =
    if (has(key) && !get(key).isJsonNull) get(key).asLong else default

fun JsonObject.getDoubleOrDefault(key: String, default: Double): Double =
    if (has(key) && !get(key).isJsonNull) get(key).asDouble else default

fun JsonObject.getBooleanOrDefault(key: String, default: Boolean): Boolean =
    if (has(key) && !get(key).isJsonNull) get(key).asBoolean else default

fun JsonObject.getFloatOrDefault(key: String, default: Float): Float =
    if (has(key) && !get(key).isJsonNull) get(key).asFloat else default

fun JsonObject.requireString(key: String): String =
    getStringOrNull(key) ?: throw IllegalArgumentException("Missing required field: $key")

fun JsonObject.requireInt(key: String): Int =
    if (has(key) && !get(key).isJsonNull) get(key).asInt
    else throw IllegalArgumentException("Missing required field: $key")

fun JsonObject.requireLong(key: String): Long =
    if (has(key) && !get(key).isJsonNull) get(key).asLong
    else throw IllegalArgumentException("Missing required field: $key")

fun JsonObject.requireDouble(key: String): Double =
    if (has(key) && !get(key).isJsonNull) get(key).asDouble
    else throw IllegalArgumentException("Missing required field: $key")

fun JsonObject.requireBoolean(key: String): Boolean =
    if (has(key) && !get(key).isJsonNull) get(key).asBoolean
    else throw IllegalArgumentException("Missing required field: $key")
