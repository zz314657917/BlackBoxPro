package com.blackboxpro.forge.util

import com.google.gson.JsonObject
import net.minecraft.util.text.ITextComponent
import net.minecraft.util.text.Style
import net.minecraft.util.text.event.ClickEvent
import net.minecraft.util.text.event.HoverEvent

object ChatStyleHelper {

    data class StyleMatch(
        val matchedText: String,
        val style: Style
    )

    fun findStyleMatch(raw: String, match: String): StyleMatch? {
        val component = runCatching { ITextComponent.Serializer.jsonToComponent(raw) }.getOrNull() ?: return null
        return findStyleMatch(component, match)
    }

    fun findStyleMatch(component: ITextComponent, match: String): StyleMatch? =
        findStyleMatch(component, match, null)

    fun styleToJson(style: Style): JsonObject = JsonObject().apply {
        style.clickEvent?.let { add("clickEvent", clickEventToJson(it)) }
        style.hoverEvent?.let { add("hoverEvent", hoverEventToJson(it)) }
        style.color?.let { addProperty("color", it.friendlyName) }
        if (style.bold) addProperty("bold", true)
        if (style.italic) addProperty("italic", true)
        if (style.underlined) addProperty("underlined", true)
        if (style.strikethrough) addProperty("strikethrough", true)
        if (style.obfuscated) addProperty("obfuscated", true)
        style.insertion?.let { addProperty("insertion", it) }
    }

    fun clickEventToJson(clickEvent: ClickEvent): JsonObject = JsonObject().apply {
        addProperty("action", clickEvent.action.canonicalName)
        addProperty("value", clickEvent.value)
    }

    fun hoverEventToJson(hoverEvent: HoverEvent): JsonObject = JsonObject().apply {
        addProperty("action", hoverEvent.action.canonicalName)
        addProperty("contents", hoverEvent.value.unformattedText)
        addProperty("contentsFormatted", hoverEvent.value.formattedText)
    }

    private fun findStyleMatch(component: ITextComponent, match: String, parentStyle: Style?): StyleMatch? {
        val effectiveStyle = component.style.createShallowCopy().apply {
            if (parentStyle != null) setParentStyle(parentStyle)
        }
        val directText = component.unformattedComponentText
        if (directText.contains(match)) {
            return StyleMatch(match, effectiveStyle)
        }
        component.siblings.forEach { sibling ->
            val result = findStyleMatch(sibling, match, effectiveStyle)
            if (result != null) return result
        }
        return null
    }
}
