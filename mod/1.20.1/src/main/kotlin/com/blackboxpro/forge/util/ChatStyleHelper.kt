package com.blackboxpro.forge.util

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.Style
import net.minecraft.network.chat.TextColor
import java.util.Optional

object ChatStyleHelper {

    data class StyleMatch(
        val matchedText: String,
        val style: Style,
    )

    fun findStyleMatch(component: Component, match: String): StyleMatch? {
        var result: StyleMatch? = null
        component.visit({ style, text ->
            if (result == null && text.contains(match)) {
                result = StyleMatch(match, style)
                Optional.of(text)
            } else {
                Optional.empty<String>()
            }
        }, Style.EMPTY)
        return result
    }

    fun styleToJson(style: Style): JsonObject = JsonObject().apply {
        style.clickEvent?.let { add("clickEvent", clickEventToJson(it)) }
        style.hoverEvent?.let { add("hoverEvent", hoverEventToJson(it)) }
        style.color?.let { addProperty("color", colorToHex(it)) }
        if (style.isBold) addProperty("bold", true)
        if (style.isItalic) addProperty("italic", true)
        if (style.isUnderlined) addProperty("underlined", true)
        if (style.isStrikethrough) addProperty("strikethrough", true)
        if (style.isObfuscated) addProperty("obfuscated", true)
        style.insertion?.let { addProperty("insertion", it) }
    }

    fun clickEventToJson(clickEvent: ClickEvent): JsonObject = JsonObject().apply {
        addProperty("action", clickEvent.action.name.lowercase())
        addProperty("value", clickEvent.value)
    }

    fun hoverEventToJson(hoverEvent: HoverEvent): JsonObject = JsonObject().apply {
        addProperty("action", hoverEvent.action.name.lowercase())
        addProperty("contents", hoverEvent.toString())
    }

    fun serializeComponent(component: Component): JsonElement =
        JsonPrimitive(component.string)

    fun componentToLegacyString(component: Component): String =
        component.string

    private fun colorToHex(color: TextColor): String =
        "#%06X".format(color.value and 0xFFFFFF)
}
