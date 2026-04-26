package com.blackboxpro.forge.util

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import java.lang.reflect.Array
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.util.Collections
import java.util.IdentityHashMap
import kotlin.math.max

object GermScreenProbeHelper {

    private const val DEFAULT_MAX_DEPTH = 4
    private const val DEFAULT_MAX_COMPONENTS = 200
    private const val MAX_WARNINGS = 40
    private const val MAX_FIELD_HINTS = 16
    private const val MAX_STRING_LENGTH = 160

    private val germSignals = listOf("germ", "germmc")
    private val guiSignals = listOf(
        "gui",
        "screen",
        "part",
        "component",
        "button",
        "slot",
        "canvas",
        "scroll",
        "texture",
        "label",
        "checkbox",
        "input",
        "frame"
    )
    private val skipClassPrefixes = listOf(
        "java.",
        "javax.",
        "sun.",
        "kotlin.",
        "com.google.gson.",
        "org.lwjgl.",
        "net.minecraft.",
        "net.minecraftforge."
    )
    private val coordinateNames = mapOf(
        "name" to listOf("getName", "getGuiName", "getIdentity", "getId", "getKey"),
        "x" to listOf("getX", "getLeft", "getStartX"),
        "y" to listOf("getY", "getTop", "getStartY"),
        "width" to listOf("getWidth", "getW"),
        "height" to listOf("getHeight", "getH"),
        "visible" to listOf("isVisible", "getVisible"),
        "enabled" to listOf("isEnabled", "getEnabled"),
        "invalid" to listOf("isInvalid", "getInvalid")
    )
    private val fieldAliases = mapOf(
        "name" to listOf("name", "guiName", "identity", "id", "key"),
        "x" to listOf("x", "left", "startX"),
        "y" to listOf("y", "top", "startY"),
        "width" to listOf("width", "w"),
        "height" to listOf("height", "h"),
        "visible" to listOf("visible"),
        "enabled" to listOf("enabled"),
        "invalid" to listOf("invalid")
    )

    data class ProbeOptions(
        val maxDepth: Int = DEFAULT_MAX_DEPTH,
        val maxComponents: Int = DEFAULT_MAX_COMPONENTS,
        val includeFields: Boolean = false
    )

    private data class ComponentSnapshot(
        val id: String,
        val className: String,
        val simpleClassName: String,
        val name: String?,
        val x: Double?,
        val y: Double?,
        val width: Double?,
        val height: Double?,
        val visible: Boolean?,
        val enabled: Boolean?,
        val invalid: Boolean?,
        val containsMouse: Boolean?,
        val fieldHints: JsonObject?
    ) {
        fun toJson(): JsonObject = JsonObject().apply {
            addProperty("id", id)
            addProperty("className", className)
            addProperty("simpleClassName", simpleClassName)
            if (name != null) addProperty("name", name)
            if (x != null) addProperty("x", x)
            if (y != null) addProperty("y", y)
            if (width != null) addProperty("width", width)
            if (height != null) addProperty("height", height)
            if (visible != null) addProperty("visible", visible)
            if (enabled != null) addProperty("enabled", enabled)
            if (invalid != null) addProperty("invalid", invalid)
            if (containsMouse != null) addProperty("containsMouse", containsMouse)
            if (fieldHints != null) add("fieldHints", fieldHints)
        }
    }

    fun query(options: ProbeOptions): JsonObject {
        val mc = Minecraft.getMinecraft()
        val screen = mc.currentScreen
        val cursor = ScreenMouseHelper.queryCursorState()
        val warnings = mutableListOf<String>()
        val components = mutableListOf<ComponentSnapshot>()
        val visited = Collections.newSetFromMap(IdentityHashMap<Any, Boolean>())
        val safeOptions = options.copy(
            maxDepth = options.maxDepth.coerceIn(1, 8),
            maxComponents = options.maxComponents.coerceIn(1, 500)
        )

        var screenClassName = "none"
        var probeMode = "none"
        var screenHasGermSignal = false

        if (screen != null) {
            screenClassName = screen.javaClass.name
            screenHasGermSignal = hasGermSignal(screenClassName)
            probeMode = if (screenHasGermSignal) "screen-class" else "reflective-fields"
            scanValue(
                value = screen,
                path = "root",
                depth = 0,
                options = safeOptions,
                visited = visited,
                components = components,
                warnings = warnings,
                mouseX = cursor.mouseX,
                mouseY = cursor.mouseY
            )
        }

        val componentArray = JsonArray()
        components.forEach { componentArray.add(it.toJson()) }

        val hoveredArray = JsonArray()
        components.filter { it.containsMouse == true }.forEach { hoveredArray.add(it.toJson()) }

        val warningArray = JsonArray()
        if (screen != null && screenHasGermSignal && components.isEmpty()) {
            warnings.add("no-components-found")
        }
        warnings.take(MAX_WARNINGS).forEach { warningArray.add(it) }
        if (warnings.size > MAX_WARNINGS) {
            warningArray.add("warnings-truncated:${warnings.size - MAX_WARNINGS}")
        }

        val supported = screenHasGermSignal ||
            components.any { hasGermSignal(it.className) } ||
            components.any { it.x != null && it.y != null && it.width != null && it.height != null }

        return cursor.toJson().apply {
            addProperty("screenClassName", screenClassName)
            addProperty("supported", supported)
            addProperty("probeMode", if (screen == null) "none" else probeMode)
            add("components", componentArray)
            add("hovered", hoveredArray)
            add("probeWarnings", warningArray)
        }
    }

    private fun scanValue(
        value: Any?,
        path: String,
        depth: Int,
        options: ProbeOptions,
        visited: MutableSet<Any>,
        components: MutableList<ComponentSnapshot>,
        warnings: MutableList<String>,
        mouseX: Int,
        mouseY: Int
    ) {
        if (value == null) return
        if (components.size >= options.maxComponents) {
            addWarning(warnings, "component-limit-reached:${options.maxComponents}")
            return
        }
        if (depth > options.maxDepth) {
            addWarning(warnings, "depth-limit-reached:$path")
            return
        }
        if (!visited.add(value)) return

        val type = value.javaClass
        if (shouldSkipType(type)) return

        val snapshot = snapshotComponent(value, path, options.includeFields, warnings, mouseX, mouseY)
        if (snapshot != null) {
            components.add(snapshot)
            if (components.size >= options.maxComponents) {
                addWarning(warnings, "component-limit-reached:${options.maxComponents}")
                return
            }
        }

        if (type.isArray) {
            val length = runCatching { Array.getLength(value) }.getOrDefault(0)
            for (i in 0 until length.coerceAtMost(options.maxComponents)) {
                scanValue(Array.get(value, i), "$path[$i]", depth + 1, options, visited, components, warnings, mouseX, mouseY)
            }
            return
        }

        if (value is Iterable<*>) {
            var index = 0
            for (entry in value) {
                if (index >= options.maxComponents) break
                scanValue(entry, "$path[$index]", depth + 1, options, visited, components, warnings, mouseX, mouseY)
                index++
            }
            return
        }

        if (value is Map<*, *>) {
            var index = 0
            for (entry in value.entries) {
                if (index >= options.maxComponents) break
                scanValue(entry.value, "$path{${safeString(entry.key)}}", depth + 1, options, visited, components, warnings, mouseX, mouseY)
                index++
            }
            return
        }

        declaredFields(type).forEach { field ->
            if (Modifier.isStatic(field.modifiers)) return@forEach
            if (field.type.isPrimitive || isScalarType(field.type)) return@forEach
            if (!fieldLooksRelevant(field.name, field.type)) return@forEach
            val child = runCatching {
                field.isAccessible = true
                field.get(value)
            }.getOrElse {
                addWarning(warnings, "field-read-failed:$path.${field.name}:${it.javaClass.simpleName}")
                null
            }
            scanValue(child, "$path.${field.name}", depth + 1, options, visited, components, warnings, mouseX, mouseY)
        }
    }

    private fun snapshotComponent(
        value: Any,
        path: String,
        includeFields: Boolean,
        warnings: MutableList<String>,
        mouseX: Int,
        mouseY: Int
    ): ComponentSnapshot? {
        val type = value.javaClass
        val className = type.name
        val relevant = hasGermSignal(className) || guiSignals.any { className.contains(it, ignoreCase = true) }
        val name = readString(value, "name", warnings)
        val x = readDouble(value, "x", warnings)
        val y = readDouble(value, "y", warnings)
        val width = readDouble(value, "width", warnings)
        val height = readDouble(value, "height", warnings)
        val visible = readBoolean(value, "visible", warnings)
        val enabled = readBoolean(value, "enabled", warnings)
        val invalid = readBoolean(value, "invalid", warnings)
        val hasBounds = x != null && y != null && width != null && height != null

        if (!relevant && name == null && !hasBounds) return null

        val containsMouse = if (hasBounds) {
            mouseX.toDouble() >= x!! &&
                mouseY.toDouble() >= y!! &&
                mouseX.toDouble() <= x + max(0.0, width!!) &&
                mouseY.toDouble() <= y + max(0.0, height!!)
        } else {
            null
        }

        return ComponentSnapshot(
            id = path,
            className = className,
            simpleClassName = type.simpleName.takeIf { it.isNotEmpty() } ?: className.substringAfterLast('.'),
            name = name,
            x = x,
            y = y,
            width = width,
            height = height,
            visible = visible,
            enabled = enabled,
            invalid = invalid,
            containsMouse = containsMouse,
            fieldHints = if (includeFields) buildFieldHints(value, warnings) else null
        )
    }

    private fun readString(value: Any, key: String, warnings: MutableList<String>): String? {
        readByGetters(value, key, warnings)?.let { return safeString(it) }
        return readByFields(value, key, warnings)?.let { safeString(it) }
    }

    private fun readDouble(value: Any, key: String, warnings: MutableList<String>): Double? {
        readByGetters(value, key, warnings)?.let { return toDoubleOrNull(it) }
        return readByFields(value, key, warnings)?.let { toDoubleOrNull(it) }
    }

    private fun readBoolean(value: Any, key: String, warnings: MutableList<String>): Boolean? {
        readByGetters(value, key, warnings)?.let { return toBooleanOrNull(it) }
        return readByFields(value, key, warnings)?.let { toBooleanOrNull(it) }
    }

    private fun readByGetters(value: Any, key: String, warnings: MutableList<String>): Any? {
        coordinateNames[key].orEmpty().forEach { name ->
            val method = findNoArgMethod(value.javaClass, name) ?: return@forEach
            if (!isSafeScalarReturn(method.returnType)) return@forEach
            return runCatching {
                method.isAccessible = true
                method.invoke(value)
            }.getOrElse {
                addWarning(warnings, "getter-failed:${value.javaClass.simpleName}.$name:${it.javaClass.simpleName}")
                null
            }
        }
        return null
    }

    private fun readByFields(value: Any, key: String, warnings: MutableList<String>): Any? {
        fieldAliases[key].orEmpty().forEach { name ->
            val field = findField(value.javaClass, name) ?: return@forEach
            if (!isSafeScalarReturn(field.type)) return@forEach
            return runCatching {
                field.isAccessible = true
                field.get(value)
            }.getOrElse {
                addWarning(warnings, "scalar-field-failed:${value.javaClass.simpleName}.$name:${it.javaClass.simpleName}")
                null
            }
        }
        return null
    }

    private fun buildFieldHints(value: Any, warnings: MutableList<String>): JsonObject {
        val hints = JsonObject()
        var count = 0
        for (field in declaredFields(value.javaClass)) {
            if (count >= MAX_FIELD_HINTS) break
            if (Modifier.isStatic(field.modifiers)) continue
            if (!isSafeScalarReturn(field.type)) continue
            val fieldValue = runCatching {
                field.isAccessible = true
                field.get(value)
            }.getOrElse {
                addWarning(warnings, "hint-field-failed:${value.javaClass.simpleName}.${field.name}:${it.javaClass.simpleName}")
                null
            } ?: continue
            addHint(hints, field.name, fieldValue)
            count++
        }
        return hints
    }

    private fun addHint(target: JsonObject, name: String, value: Any) {
        when (value) {
            is Number -> target.addProperty(name, value)
            is Boolean -> target.addProperty(name, value)
            is Enum<*> -> target.addProperty(name, value.name)
            else -> target.addProperty(name, safeString(value))
        }
    }

    private fun findNoArgMethod(type: Class<*>, name: String): Method? {
        var current: Class<*>? = type
        while (current != null) {
            current.declaredMethods.firstOrNull { it.name == name && it.parameterTypes.isEmpty() }?.let { return it }
            current = current.superclass
        }
        return null
    }

    private fun findField(type: Class<*>, name: String): Field? {
        var current: Class<*>? = type
        while (current != null) {
            current.declaredFields.firstOrNull { it.name == name }?.let { return it }
            current = current.superclass
        }
        return null
    }

    private fun declaredFields(type: Class<*>): List<Field> {
        val fields = mutableListOf<Field>()
        var current: Class<*>? = type
        while (current != null && current != Any::class.java) {
            fields.addAll(current.declaredFields)
            current = current.superclass
        }
        return fields
    }

    private fun shouldSkipType(type: Class<*>): Boolean {
        if (type.isPrimitive || isScalarType(type)) return true
        return skipClassPrefixes.any { type.name.startsWith(it) } && !hasGermSignal(type.name)
    }

    private fun fieldLooksRelevant(name: String, type: Class<*>): Boolean {
        if (hasGermSignal(type.name)) return true
        if (guiSignals.any { signal -> name.contains(signal, ignoreCase = true) || type.name.contains(signal, ignoreCase = true) }) {
            return true
        }
        return Iterable::class.java.isAssignableFrom(type) || Map::class.java.isAssignableFrom(type) || type.isArray
    }

    private fun hasGermSignal(value: String): Boolean =
        germSignals.any { value.contains(it, ignoreCase = true) }

    private fun isSafeScalarReturn(type: Class<*>): Boolean =
        type.isPrimitive || isScalarType(type) || type.isEnum

    private fun isScalarType(type: Class<*>): Boolean =
        type == String::class.java ||
            Number::class.java.isAssignableFrom(type) ||
            type == java.lang.Boolean::class.java ||
            type == java.lang.Character::class.java

    private fun toDoubleOrNull(value: Any): Double? = when (value) {
        is Number -> value.toDouble()
        is String -> value.toDoubleOrNull()
        else -> null
    }

    private fun toBooleanOrNull(value: Any): Boolean? = when (value) {
        is Boolean -> value
        is String -> when {
            value.equals("true", ignoreCase = true) -> true
            value.equals("false", ignoreCase = true) -> false
            else -> null
        }
        else -> null
    }

    private fun safeString(value: Any?): String? {
        if (value == null) return null
        val raw = value.toString()
        return if (raw.length > MAX_STRING_LENGTH) raw.take(MAX_STRING_LENGTH) + "..." else raw
    }

    private fun addWarning(warnings: MutableList<String>, warning: String) {
        if (warnings.size < MAX_WARNINGS + 1) warnings.add(warning)
    }
}
