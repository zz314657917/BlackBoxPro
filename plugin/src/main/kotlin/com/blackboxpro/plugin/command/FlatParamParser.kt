package com.blackboxpro.plugin.command

import com.google.gson.JsonObject

/**
 * 扁平化参数解析器。
 *
 * 将 `key:value` 格式的参数列表解析为 [JsonObject]，自动推断值类型。
 */
object FlatParamParser {

    /**
     * 解析 `key:value` 参数列表为 [JsonObject]。
     *
     * 类型推断规则：
     * - `true` / `false` → Boolean
     * - 纯整数 → Number (Long)
     * - 含小数 → Number (Double)
     * - 其余 → String（保留原始值）
     */
    fun parse(args: List<String>): JsonObject = JsonObject().apply {
        for (arg in args) {
            val colonIndex = arg.indexOf(':')
            if (colonIndex <= 0 || colonIndex == arg.lastIndex) continue
            val key = arg.substring(0, colonIndex)
            val raw = arg.substring(colonIndex + 1)
            when {
                raw.equals("true", ignoreCase = true) -> addProperty(key, true)
                raw.equals("false", ignoreCase = true) -> addProperty(key, false)
                raw.toLongOrNull() != null -> addProperty(key, raw.toLong())
                raw.toDoubleOrNull() != null -> addProperty(key, raw.toDouble())
                else -> addProperty(key, raw)
            }
        }
    }
}
