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
import kotlin.math.abs
import kotlin.math.max

object GermScreenProbeHelper {

    private const val DEFAULT_MAX_DEPTH = 4
    private const val DEFAULT_MAX_COMPONENTS = 200
    private const val MAX_WARNINGS = 40
    private const val MAX_FIELD_HINTS = 16
    private const val MAX_NUMERIC_HINTS = 40
    private const val MAX_BOUNDS_CANDIDATES = 8
    private const val MAX_METHOD_HINTS = 16
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
        "name" to listOf("getName", "getGuiName", "getIdentity", "getId", "getKey", "getText", "getLabel", "getTitle"),
        "x" to listOf("getX", "getLeft", "getStartX", "getGuiX", "getAbsoluteX", "getRealX"),
        "y" to listOf("getY", "getTop", "getStartY", "getGuiY", "getAbsoluteY", "getRealY"),
        "width" to listOf("getWidth", "getW", "getGuiWidth"),
        "height" to listOf("getHeight", "getH", "getGuiHeight"),
        "visible" to listOf("isVisible", "getVisible"),
        "enabled" to listOf("isEnabled", "getEnabled"),
        "invalid" to listOf("isInvalid", "getInvalid")
    )
    private val fieldAliases = mapOf(
        "name" to listOf("name", "guiName", "identity", "id", "key", "text", "label", "title"),
        "x" to listOf("x", "left", "startX", "guiX", "absoluteX", "realX"),
        "y" to listOf("y", "top", "startY", "guiY", "absoluteY", "realY"),
        "width" to listOf("width", "w", "guiWidth"),
        "height" to listOf("height", "h", "guiHeight"),
        "visible" to listOf("visible"),
        "enabled" to listOf("enabled"),
        "invalid" to listOf("invalid")
    )
    private val clickMethodSignals = listOf("click", "mouse", "press", "release", "dos", "action", "handle", "interact")

    data class ProbeOptions(
        val maxDepth: Int = DEFAULT_MAX_DEPTH,
        val maxComponents: Int = DEFAULT_MAX_COMPONENTS,
        val includeFields: Boolean = false,
        val hitX: Double? = null,
        val hitY: Double? = null
    )

    private data class ComponentSnapshot(
        val id: String,
        val parentId: String?,
        val depth: Int,
        val order: Int,
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
        val containsHit: Boolean?,
        val boundsSource: String?,
        val hitSource: String?,
        val hitPredicateMethod: String?,
        val fieldHints: JsonObject?,
        val numericHints: JsonArray?,
        val boundsCandidates: List<BoundsCandidate>,
        val candidateMouseMethods: List<String>?
    ) {
        val hasBounds: Boolean = x != null && y != null && width != null && height != null
        val centerX: Double? = if (hasBounds) x!! + max(0.0, width!!) / 2.0 else null
        val centerY: Double? = if (hasBounds) y!! + max(0.0, height!!) / 2.0 else null
        val area: Double? = if (hasBounds) max(0.0, width!!) * max(0.0, height!!) else null

        fun toJson(hitRank: Int? = null): JsonObject = JsonObject().apply {
            addProperty("id", id)
            if (parentId != null) addProperty("parentId", parentId)
            addProperty("depth", depth)
            addProperty("order", order)
            if (hitRank != null) addProperty("hitRank", hitRank)
            addProperty("className", className)
            addProperty("simpleClassName", simpleClassName)
            if (name != null) addProperty("name", name)
            if (x != null) addProperty("x", x)
            if (y != null) addProperty("y", y)
            if (width != null) addProperty("width", width)
            if (height != null) addProperty("height", height)
            if (centerX != null) addProperty("centerX", centerX)
            if (centerY != null) addProperty("centerY", centerY)
            if (area != null) addProperty("area", area)
            if (visible != null) addProperty("visible", visible)
            if (enabled != null) addProperty("enabled", enabled)
            if (invalid != null) addProperty("invalid", invalid)
            if (containsMouse != null) addProperty("containsMouse", containsMouse)
            if (containsHit != null) addProperty("containsHit", containsHit)
            if (boundsSource != null) addProperty("boundsSource", boundsSource)
            if (hitSource != null) addProperty("hitSource", hitSource)
            if (hitPredicateMethod != null) addProperty("hitPredicateMethod", hitPredicateMethod)
            if (fieldHints != null) add("fieldHints", fieldHints)
            if (numericHints != null) add("numericHints", numericHints)
            if (boundsCandidates.isNotEmpty()) {
                add("boundsCandidates", JsonArray().apply {
                    boundsCandidates.forEach { add(it.toJson(hitX = null, hitY = null)) }
                })
            }
            if (!candidateMouseMethods.isNullOrEmpty()) {
                add("candidateMouseMethods", JsonArray().apply {
                    candidateMouseMethods.forEach { add(it) }
                })
            }
        }
    }

    private data class BoundsCandidate(
        val source: String,
        val x: Double,
        val y: Double,
        val width: Double,
        val height: Double
    ) {
        val normalizedWidth: Double = max(0.0, width)
        val normalizedHeight: Double = max(0.0, height)
        val centerX: Double = x + normalizedWidth / 2.0
        val centerY: Double = y + normalizedHeight / 2.0
        val area: Double = normalizedWidth * normalizedHeight

        fun contains(px: Double, py: Double): Boolean =
            px >= x && py >= y && px <= x + normalizedWidth && py <= y + normalizedHeight

        fun toJson(hitX: Double?, hitY: Double?): JsonObject = JsonObject().apply {
            addProperty("source", source)
            addProperty("x", x)
            addProperty("y", y)
            addProperty("width", width)
            addProperty("height", height)
            addProperty("centerX", centerX)
            addProperty("centerY", centerY)
            addProperty("area", area)
            if (hitX != null && hitY != null) addProperty("containsHit", contains(hitX, hitY))
        }
    }

    private data class NumericRead(val value: Double, val source: String)

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
        val hitRequested = safeOptions.hitX != null && safeOptions.hitY != null

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
                mouseY = cursor.mouseY,
                hitX = safeOptions.hitX,
                hitY = safeOptions.hitY,
                parentComponentId = null,
                germContext = screenHasGermSignal
            )
        }

        val componentArray = JsonArray()
        components.forEach { componentArray.add(it.toJson()) }

        val hoveredArray = JsonArray()
        components.filter { it.containsMouse == true }.forEach { hoveredArray.add(it.toJson()) }

        val hitComponents = if (hitRequested) {
            components
                .filter { it.containsHit == true }
                .sortedWith(compareBy<ComponentSnapshot> { it.area ?: Double.MAX_VALUE }
                    .thenByDescending { it.depth }
                    .thenByDescending { it.order })
        } else {
            emptyList()
        }

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
            addProperty("componentCount", components.size)
            addProperty("hoveredCount", components.count { it.containsMouse == true })
            add("components", componentArray)
            add("hovered", hoveredArray)
            if (hitRequested) add("hitTest", buildHitTest(safeOptions.hitX!!, safeOptions.hitY!!, hitComponents))
            add("probeWarnings", warningArray)
        }
    }

    fun hitTest(options: ProbeOptions): JsonObject {
        val cursor = ScreenMouseHelper.queryCursorState()
        val hitX = options.hitX ?: cursor.mouseX.toDouble()
        val hitY = options.hitY ?: cursor.mouseY.toDouble()
        return query(options.copy(hitX = hitX, hitY = hitY))
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
        mouseY: Int,
        hitX: Double?,
        hitY: Double?,
        parentComponentId: String?,
        germContext: Boolean
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
        val currentGermContext = germContext || hasGermSignal(type.name)
        if (type.isArray) {
            val length = runCatching { Array.getLength(value) }.getOrDefault(0)
            for (i in 0 until length.coerceAtMost(options.maxComponents)) {
                scanValue(Array.get(value, i), "$path[$i]", depth + 1, options, visited, components, warnings, mouseX, mouseY, hitX, hitY, parentComponentId, currentGermContext)
            }
            return
        }

        if (value is Iterable<*>) {
            var index = 0
            for (entry in value) {
                if (index >= options.maxComponents) break
                scanValue(entry, "$path[$index]", depth + 1, options, visited, components, warnings, mouseX, mouseY, hitX, hitY, parentComponentId, currentGermContext)
                index++
            }
            return
        }

        if (value is Map<*, *>) {
            var index = 0
            for (entry in value.entries) {
                if (index >= options.maxComponents) break
                scanValue(entry.value, "$path{${safeString(entry.key)}}", depth + 1, options, visited, components, warnings, mouseX, mouseY, hitX, hitY, parentComponentId, currentGermContext)
                index++
            }
            return
        }

        if (shouldSkipType(type)) return

        val snapshot = snapshotComponent(
            value = value,
            path = path,
            parentId = parentComponentId,
            depth = depth,
            order = components.size,
            includeFields = options.includeFields,
            warnings = warnings,
            mouseX = mouseX,
            mouseY = mouseY,
            hitX = hitX,
            hitY = hitY
        )
        val nextParentComponentId = snapshot?.id ?: parentComponentId
        if (snapshot != null) {
            components.add(snapshot)
            if (components.size >= options.maxComponents) {
                addWarning(warnings, "component-limit-reached:${options.maxComponents}")
                return
            }
        }

        declaredFields(type).forEach { field ->
            if (Modifier.isStatic(field.modifiers)) return@forEach
            if (field.type.isPrimitive || isScalarType(field.type)) return@forEach
            if (!currentGermContext && !fieldLooksRelevant(field.name, field.type)) return@forEach
            val child = runCatching {
                field.isAccessible = true
                field.get(value)
            }.getOrElse {
                addWarning(warnings, "field-read-failed:$path.${field.name}:${it.javaClass.simpleName}")
                null
            }
            scanValue(child, "$path.${field.name}", depth + 1, options, visited, components, warnings, mouseX, mouseY, hitX, hitY, nextParentComponentId, currentGermContext)
        }
    }

    private fun snapshotComponent(
        value: Any,
        path: String,
        parentId: String?,
        depth: Int,
        order: Int,
        includeFields: Boolean,
        warnings: MutableList<String>,
        mouseX: Int,
        mouseY: Int,
        hitX: Double?,
        hitY: Double?
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
        val directBounds = if (x != null && y != null && width != null && height != null) {
            BoundsCandidate("direct", x, y, width, height)
        } else {
            null
        }
        val boundsCandidates = buildBoundsCandidates(value, directBounds)
        val primaryBounds = boundsCandidates.firstOrNull()
        val snapshotX = x ?: primaryBounds?.x
        val snapshotY = y ?: primaryBounds?.y
        val snapshotWidth = width ?: primaryBounds?.width
        val snapshotHeight = height ?: primaryBounds?.height
        val hasBounds = primaryBounds != null

        if (!relevant && name == null && !hasBounds) return null

        val mouseHitCandidate = if (hasBounds) {
            boundsCandidates.firstOrNull { it.contains(mouseX.toDouble(), mouseY.toDouble()) }
        } else {
            null
        }
        val containsMouse = if (hasBounds) mouseHitCandidate != null else null
        val boundsHitCandidate = if (hasBounds && hitX != null && hitY != null) {
            boundsCandidates.firstOrNull { it.contains(hitX, hitY) }
        } else {
            null
        }
        val boundsHit = if (hitX != null && hitY != null && hasBounds) boundsHitCandidate != null else null
        val predicateHit = if (hitX != null && hitY != null) {
            invokeHitPredicate(value, hitX, hitY, warnings)
        } else {
            null
        }
        val containsHit = when {
            boundsHitCandidate != null -> true
            predicateHit?.hit == true -> true
            boundsHit != null -> false
            predicateHit != null -> predicateHit.hit
            else -> null
        }
        val hitSource = when {
            boundsHitCandidate != null -> boundsHitCandidate.source
            predicateHit?.hit == true -> "predicate"
            else -> null
        }
        val exposedBoundsCandidates = if (includeFields || boundsHitCandidate != null || mouseHitCandidate != null) {
            boundsCandidates.take(MAX_BOUNDS_CANDIDATES)
        } else {
            emptyList()
        }

        return ComponentSnapshot(
            id = path,
            parentId = parentId,
            depth = depth,
            order = order,
            className = className,
            simpleClassName = type.simpleName.takeIf { it.isNotEmpty() } ?: className.substringAfterLast('.'),
            name = name,
            x = snapshotX,
            y = snapshotY,
            width = snapshotWidth,
            height = snapshotHeight,
            visible = visible,
            enabled = enabled,
            invalid = invalid,
            containsMouse = containsMouse,
            containsHit = containsHit,
            boundsSource = primaryBounds?.source,
            hitSource = hitSource,
            hitPredicateMethod = predicateHit?.method,
            fieldHints = if (includeFields) buildFieldHints(value, warnings) else null,
            numericHints = if (includeFields) buildNumericHints(value) else null,
            boundsCandidates = exposedBoundsCandidates,
            candidateMouseMethods = if (includeFields) buildCandidateMouseMethods(value) else null
        )
    }

    private data class PredicateHit(val hit: Boolean, val method: String)

    private fun buildHitTest(hitX: Double, hitY: Double, hits: List<ComponentSnapshot>): JsonObject =
        JsonObject().apply {
            addProperty("x", hitX)
            addProperty("y", hitY)
            addProperty("hitCount", hits.size)
            val hitArray = JsonArray()
            hits.forEachIndexed { index, snapshot -> hitArray.add(snapshot.toJson(index + 1)) }
            add("hits", hitArray)
            hits.firstOrNull()?.let { add("bestHit", it.toJson(1)) }
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

    private fun buildNumericHints(value: Any): JsonArray {
        val hints = JsonArray()
        val visited = Collections.newSetFromMap(IdentityHashMap<Any, Boolean>())
        collectNumericHints(value, "root", 0, visited, hints)
        return hints
    }

    private fun collectNumericHints(
        value: Any?,
        path: String,
        depth: Int,
        visited: MutableSet<Any>,
        hints: JsonArray
    ) {
        if (value == null || hints.size() >= MAX_NUMERIC_HINTS || depth > 3) return
        if (value is Number || value is String) {
            toDoubleOrNull(value)?.let { addNumericHint(hints, path, path.substringAfterLast('.'), it, safeString(value)) }
            return
        }
        if (isScalarType(value.javaClass) || value.javaClass.isEnum) return
        if (!visited.add(value)) return

        if (value.javaClass.isArray || value is Iterable<*> || value is Map<*, *>) {
            forEachChildValue(value, 12) { childPath, child ->
                collectNumericHints(child, "$path$childPath", depth + 1, visited, hints)
            }
            return
        }

        for (field in declaredFields(value.javaClass)) {
            if (hints.size() >= MAX_NUMERIC_HINTS) break
            if (Modifier.isStatic(field.modifiers)) continue
            val child = readFieldValue(value, field) ?: continue
            val childPath = "$path.${field.name}"
            if (isSafeScalarReturn(field.type)) {
                toDoubleOrNull(child)?.let { addNumericHint(hints, childPath, field.name, it, safeString(child)) }
                continue
            }
            readExpressionNumber(child, childPath, 0, Collections.newSetFromMap(IdentityHashMap<Any, Boolean>()))?.let {
                addNumericHint(hints, "$childPath#effective", field.name, it.value, it.source)
            }
            if (fieldLooksRelevant(field.name, field.type) || hasGermSignal(field.type.name)) {
                collectNumericHints(child, childPath, depth + 1, visited, hints)
            }
        }
    }

    private fun addNumericHint(target: JsonArray, path: String, name: String, value: Double, raw: String?) {
        if (target.size() >= MAX_NUMERIC_HINTS) return
        target.add(JsonObject().apply {
            addProperty("path", path)
            addProperty("name", name)
            addProperty("value", value)
            if (raw != null) addProperty("raw", raw)
        })
    }

    private fun buildBoundsCandidates(value: Any, directBounds: BoundsCandidate?): List<BoundsCandidate> {
        val candidates = mutableListOf<BoundsCandidate>()
        directBounds?.let { addBoundsCandidate(candidates, it) }

        val obfuscatedX = readEffectiveFieldNumber(value, "do")
        val obfuscatedY = readEffectiveFieldNumber(value, "super")
        val obfuscatedWidth = readEffectiveFieldNumber(value, "throws")
        val obfuscatedHeight = readEffectiveFieldNumber(value, "null")
        val isTextComponent = looksLikeTextComponent(value)
        val textWidth = readDirectFieldNumber(value, "return")
        val textX = readEffectiveFieldPathNumber(value, "else", "true")
            ?: findNestedNumber(value, "true", 1.0, 10000.0)
            ?: obfuscatedX
        val textY = readEffectiveFieldPathNumber(value, "else", "char")
            ?: findNestedNumber(value, "char", 1.0, 10000.0)
            ?: obfuscatedY
        val addedTextBounds = if (textX != null && textY != null && textWidth != null && isTextComponent) {
            val textHeight = findNestedNumber(value, "case", 6.0, 32.0)?.value ?: 12.0
            addBoundsCandidate(
                candidates,
                BoundsCandidate(
                    source = "germ-obfuscated:text:return",
                    x = textX.value,
                    y = textY.value,
                    width = textWidth.value,
                    height = textHeight
                )
            )
            true
        } else {
            false
        }

        if (!addedTextBounds && obfuscatedX != null && obfuscatedY != null && obfuscatedWidth != null && obfuscatedHeight != null) {
            addBoundsCandidate(
                candidates,
                BoundsCandidate(
                    source = "germ-obfuscated:do/super/throws/null",
                    x = obfuscatedX.value,
                    y = obfuscatedY.value,
                    width = obfuscatedWidth.value,
                    height = obfuscatedHeight.value
                )
            )
        }

        return candidates.take(MAX_BOUNDS_CANDIDATES)
    }

    private fun addBoundsCandidate(target: MutableList<BoundsCandidate>, candidate: BoundsCandidate) {
        if (!isUsableBounds(candidate)) return
        if (target.any { sameBounds(it, candidate) }) return
        target.add(candidate)
    }

    private fun isUsableBounds(candidate: BoundsCandidate): Boolean =
        isFinite(candidate.x) &&
            isFinite(candidate.y) &&
            isFinite(candidate.width) &&
            isFinite(candidate.height) &&
            candidate.width > 0.0 &&
            candidate.height > 0.0 &&
            candidate.width <= 10000.0 &&
            candidate.height <= 10000.0

    private fun sameBounds(left: BoundsCandidate, right: BoundsCandidate): Boolean =
        abs(left.x - right.x) < 0.01 &&
            abs(left.y - right.y) < 0.01 &&
            abs(left.width - right.width) < 0.01 &&
            abs(left.height - right.height) < 0.01

    private fun readEffectiveFieldNumber(value: Any, fieldName: String): NumericRead? {
        return choosePreferredNumericRead(readEffectiveFieldNumbers(value, fieldName))
    }

    private fun readEffectiveFieldPathNumber(value: Any, vararg fieldNames: String): NumericRead? {
        return choosePreferredNumericRead(readEffectiveFieldPathNumbers(value, fieldNames.toList(), 0, "root"))
    }

    private fun readEffectiveFieldNumbers(value: Any, fieldName: String): List<NumericRead> =
        readFieldValues(value, fieldName).mapNotNull { child ->
            readExpressionNumber(child, fieldName, 0, Collections.newSetFromMap(IdentityHashMap<Any, Boolean>()))
        }

    private fun readEffectiveFieldPathNumbers(value: Any?, fieldNames: List<String>, index: Int, path: String): List<NumericRead> {
        if (value == null) return emptyList()
        if (index >= fieldNames.size) {
            return listOfNotNull(readExpressionNumber(value, path, 0, Collections.newSetFromMap(IdentityHashMap<Any, Boolean>())))
        }
        val fieldName = fieldNames[index]
        return readFieldValues(value, fieldName).flatMap { child ->
            readEffectiveFieldPathNumbers(child, fieldNames, index + 1, "$path.$fieldName")
        }
    }

    private fun choosePreferredNumericRead(values: List<NumericRead>): NumericRead? {
        if (values.isEmpty()) return null
        return values.firstOrNull { isFinite(it.value) && abs(it.value) in 0.01..10000.0 } ?: values.first()
    }

    private fun readDirectFieldNumber(value: Any, fieldName: String): NumericRead? {
        return choosePreferredNumericRead(
            readFieldValues(value, fieldName).mapNotNull { child ->
                child?.let { toDoubleOrNull(it) }?.let { NumericRead(it, fieldName) }
            }
        )
    }

    private fun readExpressionNumber(value: Any?, path: String, depth: Int, visited: MutableSet<Any>): NumericRead? {
        if (value == null || depth > 3) return null
        if (value is Number || value is String) {
            return toDoubleOrNull(value)?.let { NumericRead(it, path) }
        }
        if (isScalarType(value.javaClass) || value.javaClass.isEnum) return null
        if (!visited.add(value)) return null

        var total = 0.0
        val sources = mutableListOf<String>()
        readDirectFieldNumber(value, "ALLATORIxDEMO")?.let {
            total += it.value
            sources.add("$path.ALLATORIxDEMO=${it.value}")
        }
        val enumValue = readFieldValue(value, "enum")
        if (enumValue != null) {
            var index = 0
            forEachChildValue(enumValue, 8) { childPath, child ->
                readExpressionNumber(child, "$path.enum$childPath", depth + 1, visited)?.let {
                    total += it.value
                    sources.add(it.source)
                }
                index++
            }
        }
        return if (sources.isEmpty()) null else NumericRead(total, sources.joinToString("+"))
    }

    private fun findNestedNumber(value: Any, fieldName: String, minValue: Double, maxValue: Double): NumericRead? {
        val visited = Collections.newSetFromMap(IdentityHashMap<Any, Boolean>())
        return findNestedNumber(value, "root", fieldName, minValue, maxValue, 0, visited)
    }

    private fun findNestedNumber(
        value: Any?,
        path: String,
        fieldName: String,
        minValue: Double,
        maxValue: Double,
        depth: Int,
        visited: MutableSet<Any>
    ): NumericRead? {
        if (value == null || depth > 3) return null
        if (value is Number || value is String || isScalarType(value.javaClass) || value.javaClass.isEnum) return null
        if (!visited.add(value)) return null

        for (field in declaredFields(value.javaClass)) {
            if (Modifier.isStatic(field.modifiers)) continue
            val child = readFieldValue(value, field) ?: continue
            val childPath = "$path.${field.name}"
            if (field.name == fieldName) {
                toDoubleOrNull(child)?.let {
                    if (it in minValue..maxValue) return NumericRead(it, childPath)
                }
                readExpressionNumber(child, childPath, 0, Collections.newSetFromMap(IdentityHashMap<Any, Boolean>()))?.let {
                    if (it.value in minValue..maxValue) return it
                }
            }
            if (!isSafeScalarReturn(field.type)) {
                findNestedNumber(child, childPath, fieldName, minValue, maxValue, depth + 1, visited)?.let { return it }
            }
        }
        return null
    }

    private fun looksLikeTextComponent(value: Any): Boolean {
        val markers = listOf("throw", "name", "text", "label", "title")
        return markers.any { name ->
            readFieldValues(value, name).any { safeString(it)?.contains("text", ignoreCase = true) == true }
        }
    }

    private fun readFieldValue(value: Any, fieldName: String): Any? {
        val field = findField(value.javaClass, fieldName) ?: return null
        return readFieldValue(value, field)
    }

    private fun readFieldValues(value: Any, fieldName: String): List<Any?> =
        declaredFields(value.javaClass)
            .asSequence()
            .filter { !Modifier.isStatic(it.modifiers) && it.name == fieldName }
            .map { readFieldValue(value, it) }
            .toList()

    private fun readFieldValue(value: Any, field: Field): Any? =
        runCatching {
            field.isAccessible = true
            field.get(value)
        }.getOrNull()

    private fun forEachChildValue(value: Any, limit: Int, consumer: (String, Any?) -> Unit) {
        when {
            value.javaClass.isArray -> {
                val length = runCatching { Array.getLength(value) }.getOrDefault(0)
                for (i in 0 until length.coerceAtMost(limit)) consumer("[$i]", Array.get(value, i))
            }
            value is Iterable<*> -> {
                var index = 0
                for (entry in value) {
                    if (index >= limit) break
                    consumer("[$index]", entry)
                    index++
                }
            }
            value is Map<*, *> -> {
                var index = 0
                for (entry in value.entries) {
                    if (index >= limit) break
                    consumer("{${safeString(entry.key) ?: index.toString()}}", entry.value)
                    index++
                }
            }
        }
    }

    private fun buildCandidateMouseMethods(value: Any): List<String> {
        val methods = linkedSetOf<String>()
        for (method in declaredMethods(value.javaClass)) {
            if (methods.size >= MAX_METHOD_HINTS) break
            if (Modifier.isStatic(method.modifiers)) continue
            if (!looksLikeMouseMethod(method)) continue
            methods.add(methodSignature(method))
        }
        return methods.toList()
    }

    private fun invokeHitPredicate(value: Any, hitX: Double, hitY: Double, warnings: MutableList<String>): PredicateHit? {
        var tested: PredicateHit? = null
        for (method in declaredMethods(value.javaClass)) {
            if (Modifier.isStatic(method.modifiers)) continue
            if (!looksLikeHitPredicate(method)) continue
            val args = buildNumericArgs(method.parameterTypes, hitX, hitY) ?: continue
            val signature = methodSignature(method)
            val result = runCatching {
                method.isAccessible = true
                method.invoke(value, *args)
            }.getOrElse {
                addWarning(warnings, "hit-predicate-failed:${value.javaClass.simpleName}.$signature:${it.javaClass.simpleName}")
                null
            }
            if (result is Boolean) {
                val hit = PredicateHit(result, signature)
                if (result) return hit
                tested = hit
            }
        }
        return tested
    }

    private fun looksLikeHitPredicate(method: Method): Boolean =
        (method.returnType == java.lang.Boolean.TYPE || method.returnType == java.lang.Boolean::class.java) &&
            method.parameterTypes.size == 2 &&
            method.parameterTypes.all { isNumericPrimitive(it) || Number::class.java.isAssignableFrom(it) }

    private fun buildNumericArgs(types: kotlin.Array<Class<*>>, hitX: Double, hitY: Double): kotlin.Array<Any>? {
        if (types.size != 2) return null
        return arrayOf(coerceNumericArg(types[0], hitX) ?: return null, coerceNumericArg(types[1], hitY) ?: return null)
    }

    private fun coerceNumericArg(type: Class<*>, value: Double): Any? = when (type) {
        java.lang.Double.TYPE, java.lang.Double::class.java -> value
        java.lang.Float.TYPE, java.lang.Float::class.java -> value.toFloat()
        java.lang.Integer.TYPE, java.lang.Integer::class.java -> value.toInt()
        java.lang.Long.TYPE, java.lang.Long::class.java -> value.toLong()
        java.lang.Short.TYPE, java.lang.Short::class.java -> value.toInt().toShort()
        java.lang.Byte.TYPE, java.lang.Byte::class.java -> value.toInt().toByte()
        else -> null
    }

    private fun looksLikeMouseMethod(method: Method): Boolean {
        val name = method.name.lowercase()
        if (clickMethodSignals.any { name.contains(it) }) return true
        val params = method.parameterTypes
        if (params.size !in 2..4) return false
        val numericCount = params.count { isNumericPrimitive(it) || Number::class.java.isAssignableFrom(it) }
        return numericCount >= 2 && params.all(::isSafeMouseMethodParam)
    }

    private fun methodSignature(method: Method): String {
        val params = method.parameterTypes.joinToString(",") { it.simpleName.ifEmpty { it.name } }
        val returnType = method.returnType.simpleName.ifEmpty { method.returnType.name }
        return "${method.name}($params):$returnType"
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

    private fun declaredMethods(type: Class<*>): List<Method> {
        val methods = mutableListOf<Method>()
        var current: Class<*>? = type
        while (current != null && current != Any::class.java) {
            methods.addAll(current.declaredMethods)
            current = current.superclass
        }
        return methods
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

    private fun isNumericPrimitive(type: Class<*>): Boolean =
        type == java.lang.Integer.TYPE ||
            type == java.lang.Long.TYPE ||
            type == java.lang.Double.TYPE ||
            type == java.lang.Float.TYPE ||
            type == java.lang.Short.TYPE ||
            type == java.lang.Byte.TYPE

    private fun isSafeMouseMethodParam(type: Class<*>): Boolean =
        isNumericPrimitive(type) ||
            type == java.lang.Boolean.TYPE ||
            type == String::class.java ||
            Number::class.java.isAssignableFrom(type) ||
            type == java.lang.Boolean::class.java

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

    private fun isFinite(value: Double): Boolean = !value.isNaN() && !value.isInfinite()

    private fun safeString(value: Any?): String? {
        if (value == null) return null
        val raw = value.toString()
        return if (raw.length > MAX_STRING_LENGTH) raw.take(MAX_STRING_LENGTH) + "..." else raw
    }

    private fun addWarning(warnings: MutableList<String>, warning: String) {
        if (warnings.size < MAX_WARNINGS + 1) warnings.add(warning)
    }
}
