# GermScreenProbe Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a read-only `query_germ_screen` action for 1.12.2 Forge that probes the current Germ-like GUI screen through safe reflection and reports screen, component, and hover data without adding a Germ compile dependency.

**Architecture:** The shared action catalog and Bukkit plugin API expose the new query action. The 1.12.2 Forge module implements the runtime behavior through a focused `GermScreenProbeHelper` that reuses `ScreenMouseHelper.queryCursorState()` for baseline cursor/screen data, scans only safe object fields/getters, and returns `supported=false` instead of failing when the current screen is not Germ-like. Existing mouse/cursor actions remain unchanged.

**Tech Stack:** Kotlin 1.9.25, Gradle, Minecraft Forge 1.12.2, Gson `JsonObject`/`JsonArray`, Java reflection, existing BlackBoxPro action registry and test catalog.

---

## File Structure

- `common/src/main/kotlin/com/blackboxpro/common/action/ActionCatalog.kt`: register `query_germ_screen` as a query action with optional parameters.
- `plugin/src/main/kotlin/com/blackboxpro/plugin/api/action/QueryActions.kt`: add a public `queryGermScreen(...)` wrapper.
- `plugin/src/main/kotlin/com/blackboxpro/plugin/api/action/MouseActions.kt`: add a convenience forwarding wrapper for GUI automation call sites.
- `mod/1.12.2/forge/src/main/kotlin/com/blackboxpro/forge/util/GermScreenProbeHelper.kt`: new helper that owns all safe reflection, component extraction, hover calculation, and JSON serialization.
- `mod/1.12.2/forge/src/main/kotlin/com/blackboxpro/forge/action/query/QueryGermScreenAction.kt`: action executor that parses optional parameters and delegates to the helper.
- `mod/1.12.2/forge/src/main/kotlin/com/blackboxpro/forge/dispatcher/ActionRegistry.kt`: register the 1.12.2 action.
- `plugin/src/main/kotlin/com/blackboxpro/plugin/command/testframework/BlackBoxTestCatalog.kt`: add default parameters, verification, and keep it out of smoke unless a Germ GUI fixture exists.
- `knowledge/tasks/current-task.md`: after implementation and verification, record the exact build/runtime result in the existing handoff file.

Do not stage or commit `scripts/test-cells/cells.json` or `scripts/test-cells/cells-1201.json`.

---

### Task 1: Expose `query_germ_screen` In Shared Catalog And Plugin API

**Files:**
- Modify: `F:/mcplugins/BlackBoxPro-dev-2.0/common/src/main/kotlin/com/blackboxpro/common/action/ActionCatalog.kt`
- Modify: `F:/mcplugins/BlackBoxPro-dev-2.0/plugin/src/main/kotlin/com/blackboxpro/plugin/api/action/QueryActions.kt`
- Modify: `F:/mcplugins/BlackBoxPro-dev-2.0/plugin/src/main/kotlin/com/blackboxpro/plugin/api/action/MouseActions.kt`

- [ ] **Step 1: Add the action definition**

In `ActionCatalog.kt`, in the `// Query` block immediately after `register("query_cursor_state")`, add:

```kotlin
register("query_germ_screen", "maxDepth", "maxComponents", "includeFields")
```

- [ ] **Step 2: Add the primary Bukkit API wrapper**

In `QueryActions.kt`, immediately after `queryCursorState(...)`, add:

```kotlin
fun queryGermScreen(
    player: Player,
    maxDepth: Int = 4,
    maxComponents: Int = 200,
    includeFields: Boolean = false
): CompletableFuture<ResponseMessage> =
    BlackBoxApi.sendAsync(player, "query_germ_screen", JsonObject().apply {
        addProperty("maxDepth", maxDepth)
        addProperty("maxComponents", maxComponents)
        addProperty("includeFields", includeFields)
    })
```

- [ ] **Step 3: Add the convenience GUI wrapper**

In `MouseActions.kt`, immediately after `queryCursorState(...)`, add:

```kotlin
fun queryGermScreen(
    player: Player,
    maxDepth: Int = 4,
    maxComponents: Int = 200,
    includeFields: Boolean = false
): CompletableFuture<ResponseMessage> =
    QueryActions.queryGermScreen(player, maxDepth, maxComponents, includeFields)
```

- [ ] **Step 4: Build plugin/common surface**

Run:

```powershell
$env:JAVA_HOME='F:/mcplugins/.local-tools/temurin21/jdk-21.0.10+7'
./gradlew.bat common_build plugin_build
```

Expected: both tasks complete successfully. If `plugin_build` fails because the wrapper signature imports are missing, fix imports in the edited file before continuing.

- [ ] **Step 5: Commit the public API surface**

Run:

```powershell
git status --short
git add -- common/src/main/kotlin/com/blackboxpro/common/action/ActionCatalog.kt plugin/src/main/kotlin/com/blackboxpro/plugin/api/action/QueryActions.kt plugin/src/main/kotlin/com/blackboxpro/plugin/api/action/MouseActions.kt
git commit -m "feat: expose Germ screen query action"
```

Expected staged files: only the three paths above. Do not stage `scripts/test-cells/cells.json`, `scripts/test-cells/cells-1201.json`, or unrelated knowledge edits.

---

### Task 2: Implement The Read-Only Forge Probe Helper

**Files:**
- Create: `F:/mcplugins/BlackBoxPro-dev-2.0/mod/1.12.2/forge/src/main/kotlin/com/blackboxpro/forge/util/GermScreenProbeHelper.kt`

- [ ] **Step 1: Create `GermScreenProbeHelper.kt` with the full helper**

Add this file exactly:

```kotlin
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
            if (!fieldLooksRelevant(field.name, field.type.name)) return@forEach
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
            simpleClassName = type.simpleName.ifBlank { className.substringAfterLast('.') },
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

    private fun fieldLooksRelevant(name: String, typeName: String): Boolean {
        if (hasGermSignal(typeName)) return true
        if (guiSignals.any { signal -> name.contains(signal, ignoreCase = true) || typeName.contains(signal, ignoreCase = true) }) {
            return true
        }
        val fieldType = runCatching { Class.forName(typeName) }.getOrNull() ?: return false
        return Iterable::class.java.isAssignableFrom(fieldType) || Map::class.java.isAssignableFrom(fieldType)
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
        is String -> value.equals("true", ignoreCase = true) || if (value.equals("false", ignoreCase = true)) false else return null
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
```

- [ ] **Step 2: Build the Forge module and fix compile-only issues**

Run:

```powershell
$env:JAVA_HOME='F:/mcplugins/.local-tools/temurin21/jdk-21.0.10+7'
./gradlew.bat forge1122_build
```

Expected: compile reaches `:forge:compileKotlin` for the new helper. If Kotlin rejects `ifBlank`, replace that expression with:

```kotlin
val simpleName = type.simpleName.takeIf { it.isNotEmpty() } ?: className.substringAfterLast('.')
```

Then use `simpleClassName = simpleName`.

- [ ] **Step 3: Commit the helper**

Run:

```powershell
git add -- mod/1.12.2/forge/src/main/kotlin/com/blackboxpro/forge/util/GermScreenProbeHelper.kt
git commit -m "feat: add Germ screen probe helper"
```

Expected: commit includes only `GermScreenProbeHelper.kt`.

---

### Task 3: Add And Register The 1.12.2 Forge Action

**Files:**
- Create: `F:/mcplugins/BlackBoxPro-dev-2.0/mod/1.12.2/forge/src/main/kotlin/com/blackboxpro/forge/action/query/QueryGermScreenAction.kt`
- Modify: `F:/mcplugins/BlackBoxPro-dev-2.0/mod/1.12.2/forge/src/main/kotlin/com/blackboxpro/forge/dispatcher/ActionRegistry.kt`

- [ ] **Step 1: Create the action executor**

Add `QueryGermScreenAction.kt`:

```kotlin
package com.blackboxpro.forge.action.query

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.blackboxpro.forge.util.GermScreenProbeHelper
import com.blackboxpro.forge.util.getBooleanOrDefault
import com.blackboxpro.forge.util.getIntOrDefault
import com.google.gson.JsonObject

class QueryGermScreenAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val options = GermScreenProbeHelper.ProbeOptions(
            maxDepth = params.getIntOrDefault("maxDepth", 4),
            maxComponents = params.getIntOrDefault("maxComponents", 200),
            includeFields = params.getBooleanOrDefault("includeFields", false)
        )
        val data = GermScreenProbeHelper.query(options)
        return ActionResult.ok("Germ screen queried", data)
    }
}
```

- [ ] **Step 2: Register it in `ActionRegistry`**

In `ActionRegistry.kt`, in the query behavior block immediately after:

```kotlin
register("query_cursor_state", QueryCursorStateAction())
```

add:

```kotlin
register("query_germ_screen", QueryGermScreenAction())
```

The existing wildcard import `import com.blackboxpro.forge.action.query.*` should already cover the class.

- [ ] **Step 3: Build the Forge runtime**

Run:

```powershell
$env:JAVA_HOME='F:/mcplugins/.local-tools/temurin21/jdk-21.0.10+7'
./gradlew.bat forge1122_build
```

Expected: `BlackBoxPro-forge-1.12.2-*.jar` is produced under `mod/1.12.2/forge/build/libs`.

- [ ] **Step 4: Commit the Forge action registration**

Run:

```powershell
git add -- mod/1.12.2/forge/src/main/kotlin/com/blackboxpro/forge/action/query/QueryGermScreenAction.kt mod/1.12.2/forge/src/main/kotlin/com/blackboxpro/forge/dispatcher/ActionRegistry.kt
git commit -m "feat: register Germ screen query on Forge 1.12.2"
```

Expected: commit includes only the action executor and registry change.

---

### Task 4: Wire The Action Into The Test Catalog

**Files:**
- Modify: `F:/mcplugins/BlackBoxPro-dev-2.0/plugin/src/main/kotlin/com/blackboxpro/plugin/command/testframework/BlackBoxTestCatalog.kt`

- [ ] **Step 1: Add the action to fixture-pending list**

In `pendingFixtureActions`, immediately after `"query_tooltip_state",`, add:

```kotlin
"query_germ_screen",
```

Reason: the action can return on any screen, but meaningful Germ component output needs an opened Germ GUI fixture. Keeping it pending avoids false full-regression expectations.

- [ ] **Step 2: Add a fixture skip reason**

In `prepareFixture(...)`, near the existing GUI-related cases, add this branch before the `// 其他未分类` branch:

```kotlin
"query_germ_screen" ->
    CompletableFuture.completedFuture(BlackBoxPrepareResult("需要先打开真实 Germ GUI；当前 run_test 默认场景只验证 action 可注册"))
```

- [ ] **Step 3: Add default catalog params**

In `defaultParams(...)`, immediately after the `query_cursor_state` branch, add:

```kotlin
"query_germ_screen" -> JsonObject().apply {
    addProperty("maxDepth", 4)
    addProperty("maxComponents", 200)
    addProperty("includeFields", false)
}
```

- [ ] **Step 4: Add query verification**

In `verify(...)`, add `"query_germ_screen",` to the grouped query actions that require non-empty data. The block should include:

```kotlin
"query_screen_state",
"query_cursor_state",
"query_germ_screen",
"query_boss_bar" -> if (data != null && data.size() > 0) null else "查询响应没有返回数据"
```

If `query_cursor_state` is not currently in that group, add it together with `query_germ_screen`.

- [ ] **Step 5: Build the plugin catalog**

Run:

```powershell
$env:JAVA_HOME='F:/mcplugins/.local-tools/temurin21/jdk-21.0.10+7'
./gradlew.bat plugin_build
```

Expected: plugin build succeeds and the test catalog compiles.

- [ ] **Step 6: Commit the catalog update**

Run:

```powershell
git add -- plugin/src/main/kotlin/com/blackboxpro/plugin/command/testframework/BlackBoxTestCatalog.kt
git commit -m "test: add Germ screen query to catalog"
```

Expected: commit includes only `BlackBoxTestCatalog.kt`.

---

### Task 5: Build, Run A Real 1.12.2 Test-Cell Probe, And Clean Up

**Files:**
- Modify after verification: `F:/mcplugins/BlackBoxPro-dev-2.0/knowledge/tasks/current-task.md`

- [ ] **Step 1: Run the full build needed for this feature**

Run:

```powershell
$env:JAVA_HOME='F:/mcplugins/.local-tools/temurin21/jdk-21.0.10+7'
./gradlew.bat forge1122_build plugin_build
```

Expected: build succeeds. If default shell Java 8 is used accidentally, rerun with the `JAVA_HOME` line above.

- [ ] **Step 2: Pick a free Germ-capable 1.12.2 cell**

Run:

```powershell
scripts/test-cells/Get-TestCellStatus.ps1
```

Expected: choose one of `cell-01..05` with no active lease. Prefer a cell that is already known to have `GermPlugin` on the server and `GermMod` in the bot mods baseline.

- [ ] **Step 3: Deploy the new Forge jar to the chosen bot**

Use `cell-05` as the default verification cell because it is part of the 1.12.2 Germ-capable pool. If `Get-TestCellStatus.ps1` shows `cell-05` is leased, stop before this step and deliberately switch every `cell-05` command in this task to another free `cell-01..05` row from `scripts/test-cells/cells.json`.

```powershell
$cellId = 'cell-05'
$botVersionDir = 'G:/MC/game/BlackBoxProTestCells/cell-05/.minecraft/versions/bot'
$jar = Get-ChildItem -LiteralPath 'mod/1.12.2/forge/build/libs' -Filter 'BlackBoxPro-forge-1.12.2-*.jar' | Sort-Object LastWriteTime -Descending | Select-Object -First 1
Copy-Item -LiteralPath $jar.FullName -Destination (Join-Path $botVersionDir 'mods') -Force
```

Expected: the selected bot `mods/` directory contains the freshly built BlackBoxPro Forge jar.

- [ ] **Step 4: Start the cell through managed scripts**

Run:

```powershell
scripts/test-cells/Invoke-TestCell.ps1 -Mode ensure -CellId cell-05 -Owner germ-screen-probe
```

Expected: server and bot start, the plugin relay is reachable, and the bot reports ready. If the cell is leased by another owner, stop and choose a different cell rather than stealing it.

- [ ] **Step 5: Verify the action is registered**

Call the existing plugin API or command route used for action smoke in this repo. The expected result is that `/status` or action catalog output includes `query_germ_screen`, and the action count increases by one relative to the previous 1.12.2 runtime.

If using HTTP directly, target the selected cell's `pluginHttpPort` from `cells.json` and run the same action execution shape used by previous local regression scripts.

- [ ] **Step 6: Probe a non-Germ screen**

Execute `query_germ_screen` while no Germ GUI is open:

```json
{
  "maxDepth": 4,
  "maxComponents": 200,
  "includeFields": false
}
```

Expected response data:

```json
{
  "open": false,
  "supported": false,
  "probeMode": "none",
  "components": [],
  "hovered": [],
  "probeWarnings": []
}
```

If an inventory or chat screen is open, `open` may be `true`, but `supported` should remain `false` unless the screen or scanned components look Germ-like.

- [ ] **Step 7: Open a real Germ GUI and probe again**

Use the known local Germ GUI command from the current test setup, for example:

```text
/sw open
```

Then execute `query_cursor_state` and `query_germ_screen`.

Expected minimum result:

```json
{
  "open": true,
  "screenClassName": "a real client screen class name",
  "components": [],
  "hovered": [],
  "probeWarnings": []
}
```

Acceptable Germ-specific outcomes:

- `supported=true` with one or more components.
- `supported=true` with `probeWarnings` containing `no-components-found`.
- `supported=false` only if `screenClassName` shows the Germ client hides its UI behind Minecraft or mixin classes; record that evidence in `current-task.md`.

- [ ] **Step 8: Clean up the exact cell**

Run:

```powershell
scripts/test-cells/Invoke-TestCell.ps1 -Mode stop -CellId cell-05 -Owner germ-screen-probe
scripts/test-cells/Invoke-TestCell.ps1 -Mode release -CellId cell-05 -Owner germ-screen-probe
```

Then verify no managed test processes/listeners remain for that cell:

```powershell
scripts/test-cells/Get-TestCellStatus.ps1
```

Expected: selected cell has no active lease, no server/bot process, and no relevant listener ports. If a `cmd/java/javaw` process remains for the selected cell, stop the matching process through the test-cell scripts or by exact command-line match only.

- [ ] **Step 9: Update handoff with verified facts**

Append a concise entry to `knowledge/tasks/current-task.md` with:

```markdown
- 2026-04-26 `query_germ_screen` implementation:
  - build: `./gradlew.bat forge1122_build plugin_build`
  - cell: `cell-05`
  - non-Germ result: record `open`, `supported`, and `probeMode` from the actual response
  - Germ GUI result: record `supported`, component count, hovered count, and warning count from the actual response
  - cleanup: record lease state, process count, and listener count after stop/release
```

- [ ] **Step 10: Commit verification notes**

Run:

```powershell
git add -- knowledge/tasks/current-task.md
git commit -m "docs: record Germ screen probe verification"
```

Expected: commit includes only the handoff file. Do not stage `knowledge/05-current-focus.md` unless the current implementation also intentionally updates it in a separate reviewable commit.

---

## Final Verification Checklist

- [ ] `ActionCatalog` contains `query_germ_screen`.
- [ ] `ActionRegistry` registers `QueryGermScreenAction` only in 1.12.2 Forge.
- [ ] `QueryActions.queryGermScreen(...)` and `MouseActions.queryGermScreen(...)` compile.
- [ ] `GermScreenProbeHelper` has no imports from Germ packages.
- [ ] `query_germ_screen` returns success with `supported=false` for no screen or non-Germ screens.
- [ ] Reflection failures are collected in `probeWarnings` and do not fail the whole action.
- [ ] `./gradlew.bat forge1122_build plugin_build` passes with `JAVA_HOME=F:/mcplugins/.local-tools/temurin21/jdk-21.0.10+7`.
- [ ] Real test-cell verification is cleaned up: lease released, no matching `cmd/java/javaw` process, no relevant listener left.
- [ ] `scripts/test-cells/cells.json` and `scripts/test-cells/cells-1201.json` remain uncommitted.
