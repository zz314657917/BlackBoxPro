package com.blackboxpro.plugin.http

import com.blackboxpro.common.protocol.CommandMessage
import com.blackboxpro.common.protocol.ResponseMessage
import com.blackboxpro.plugin.config.BlackBoxSettings
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import taboolib.common.platform.function.submit
import java.io.File
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.Collections
import java.util.IdentityHashMap

object ServerGermActions {

    private const val ACTION_GERM_GUI_PART_DOS = "germ_gui_part_dos"

    fun canHandle(action: String): Boolean = action == ACTION_GERM_GUI_PART_DOS

    fun handle(command: CommandMessage): ResponseMessage {
        if (!Bukkit.isPrimaryThread()) {
            val future = CompletableFuture<ResponseMessage>()
            submit {
                future.complete(runCatching { execute(command) }.getOrElse { failure(command.id, it.message ?: it.javaClass.simpleName) })
            }
            return try {
                future.get(BlackBoxSettings.responseTimeoutMs, TimeUnit.MILLISECONDS)
            } catch (e: Exception) {
                failure(command.id, "Timed out executing server Germ action: ${e.message}")
            }
        }
        return runCatching { execute(command) }.getOrElse { failure(command.id, it.message ?: it.javaClass.simpleName) }
    }

    private fun execute(command: CommandMessage): ResponseMessage {
        val params = command.params
        val playerName = command.target
            ?: params.string("player")
            ?: return failure(command.id, "Missing target player")
        val player = Bukkit.getPlayerExact(playerName)
            ?: return failure(command.id, "Player not online: $playerName")
        val guiName = params.string("guiName")
            ?: return failure(command.id, "Missing param: guiName")
        val partId = params.string("partId")
            ?: return failure(command.id, "Missing param: partId")
        val dosType = params.string("dosType")?.lowercase() ?: "click"
        val execute = params.boolean("execute", true)
        val resolvePlaceholders = params.boolean("resolvePlaceholders", true)
        val mode = params.string("mode")?.lowercase() ?: "command_util"

        val screen = germGuiScreen(guiName)
            ?: return failure(command.id, "Germ GUI not found: $guiName")
        val getterName = dosGetter(dosType)
            ?: return failure(command.id, "Unsupported dosType: $dosType")
        val part = findPart(screen, partId)
        val yamlPart = if (part == null) findYamlPart(guiName, partId) else null
        if (part == null && yamlPart == null) {
            return failure(
                command.id,
                "Germ GUI part not found: $guiName/$partId",
                data(guiName, partId, dosType, emptyList(), emptyList(), execute, mode, null, availableParts(screen, guiName))
            )
        }
        val rawDos = when {
            part != null -> readDos(part, getterName)
            yamlPart != null -> readYamlDos(yamlPart.section, dosType)
            else -> emptyList()
        }
        val partData = part?.let { partSummary(it, partId) } ?: yamlPart?.let(::yamlPartSummary)
        if (rawDos.isEmpty()) {
            return failure(
                command.id,
                "No $dosType dos configured for $guiName/$partId",
                data(guiName, partId, dosType, rawDos, rawDos, execute, mode, partData, availableParts(screen, guiName))
            )
        }
        val resolvedDos = if (resolvePlaceholders) rawDos.map { resolvePlaceholders(player, it) } else rawDos

        if (execute) {
            executeDos(player, resolvedDos, mode)
        }

        return ResponseMessage(
            command.id,
            "success",
            if (execute) "Germ GUI part dos executed" else "Germ GUI part dos resolved",
            data(guiName, partId, dosType, rawDos, resolvedDos, execute, mode, partData, null)
        )
    }

    private fun germGuiScreen(guiName: String): Any? {
        val screenClass = Class.forName("com.germ.germplugin.api.dynamic.gui.GermGuiScreen")
        return screenClass.getMethod("getGermGuiScreen", String::class.java).invoke(null, guiName)
    }

    private fun dosGetter(dosType: String): String? = when (dosType) {
        "click" -> "getClickDos"
        "click_release" -> "getClickReleaseDos"
        "hover" -> "getHoverDos"
        "leave" -> "getLeaveDos"
        "right" -> "getRightDos"
        "right_release" -> "getRightReleaseDos"
        "middle" -> "getMiddleDos"
        else -> null
    }

    private fun readDos(part: Any, getterName: String): List<String> {
        val method = runCatching { part.javaClass.getMethod(getterName) }.getOrNull() ?: return emptyList()
        val value = method.invoke(part) ?: return emptyList()
        if (value !is Iterable<*>) {
            return emptyList()
        }
        return value.mapNotNull { it?.toString() }.filter { it.isNotBlank() }
    }

    private fun findPart(root: Any, partId: String): Any? {
        directPart(root, partId)?.let { return it }
        val visited = Collections.newSetFromMap(IdentityHashMap<Any, Boolean>())
        val queue = java.util.ArrayDeque<Any>()
        queue.add(root)
        while (!queue.isEmpty()) {
            val current = queue.removeFirst()
            if (!visited.add(current)) {
                continue
            }
            directPart(current, partId)?.let { return it }
            for (child in children(current)) {
                if (partMatches(child, partId)) {
                    return child
                }
                queue.add(child)
            }
        }
        return null
    }

    private fun directPart(container: Any, partId: String): Any? =
        runCatching { container.javaClass.getMethod("getGuiPart", String::class.java).invoke(container, partId) }
            .getOrNull()

    private fun children(container: Any): List<Any> {
        val value = runCatching { container.javaClass.getMethod("getGuiParts").invoke(container) }
            .getOrNull()
            ?: return emptyList()
        if (value !is Iterable<*>) {
            return emptyList()
        }
        return value.mapNotNull { it }
    }

    private fun partMatches(part: Any, partId: String): Boolean =
        listOfNotNull(
            stringGetter(part, "getIndexName"),
            stringGetter(part, "getRealName")
        ).any { it == partId }

    private fun listParts(root: Any, maxParts: Int = 300): JsonArray {
        val result = JsonArray()
        val visited = Collections.newSetFromMap(IdentityHashMap<Any, Boolean>())
        val queue = java.util.ArrayDeque<Pair<Any, String>>()
        queue.add(root to "")
        while (!queue.isEmpty() && result.size() < maxParts) {
            val (current, path) = queue.removeFirst()
            if (!visited.add(current)) {
                continue
            }
            val currentChildren = children(current)
            for (child in currentChildren) {
                val childName = stringGetter(child, "getIndexName")
                    ?: stringGetter(child, "getRealName")
                    ?: child.javaClass.simpleName
                val childPath = if (path.isBlank()) childName else "$path/$childName"
                result.add(partSummary(child, childPath))
                if (result.size() >= maxParts) {
                    break
                }
                queue.add(child to childPath)
            }
        }
        return result
    }

    private fun partSummary(part: Any, path: String): JsonObject = JsonObject().apply {
        addProperty("source", "runtime")
        addProperty("path", path)
        addProperty("indexName", stringGetter(part, "getIndexName"))
        addProperty("realName", stringGetter(part, "getRealName"))
        addProperty("className", part.javaClass.name)
        addProperty("simpleClassName", part.javaClass.simpleName)
        add("dosTypes", availableDosTypes(part))
    }

    private fun availableDosTypes(part: Any): JsonArray = JsonArray().apply {
        mapOf(
            "click" to "getClickDos",
            "click_release" to "getClickReleaseDos",
            "hover" to "getHoverDos",
            "leave" to "getLeaveDos",
            "right" to "getRightDos",
            "right_release" to "getRightReleaseDos",
            "middle" to "getMiddleDos"
        ).forEach { (name, getter) ->
            if (readDos(part, getter).isNotEmpty()) {
                add(name)
            }
        }
    }

    private fun stringGetter(target: Any, getterName: String): String? =
        runCatching { target.javaClass.getMethod(getterName).invoke(target)?.toString() }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }

    private data class YamlPart(
        val file: File,
        val path: String,
        val section: ConfigurationSection
    )

    private fun findYamlPart(guiName: String, partId: String): YamlPart? =
        yamlGuiRoots(guiName).asSequence()
            .mapNotNull { (file, root) -> findYamlPart(file, root, partId) }
            .firstOrNull()

    private fun findYamlPart(file: File, root: ConfigurationSection, partId: String): YamlPart? {
        val queue = java.util.ArrayDeque<Pair<ConfigurationSection, String>>()
        queue.add(root to root.name)
        while (!queue.isEmpty()) {
            val (section, path) = queue.removeFirst()
            for (key in section.getKeys(false)) {
                val child = section.getConfigurationSection(key) ?: continue
                val childPath = "$path/$key"
                if (key == partId) {
                    return YamlPart(file, childPath, child)
                }
                queue.add(child to childPath)
            }
        }
        return null
    }

    private fun listYamlParts(guiName: String, maxParts: Int = 300): JsonArray {
        val result = JsonArray()
        for ((file, root) in yamlGuiRoots(guiName)) {
            val queue = java.util.ArrayDeque<Pair<ConfigurationSection, String>>()
            queue.add(root to root.name)
            while (!queue.isEmpty() && result.size() < maxParts) {
                val (section, path) = queue.removeFirst()
                for (key in section.getKeys(false)) {
                    val child = section.getConfigurationSection(key) ?: continue
                    val childPath = "$path/$key"
                    result.add(yamlPartSummary(YamlPart(file, childPath, child)))
                    if (result.size() >= maxParts) {
                        break
                    }
                    queue.add(child to childPath)
                }
            }
            if (result.size() >= maxParts) {
                break
            }
        }
        return result
    }

    private fun yamlGuiRoots(guiName: String): List<Pair<File, ConfigurationSection>> {
        val guiDir = Bukkit.getPluginManager().getPlugin("GermPlugin")
            ?.dataFolder
            ?.resolve("gui")
            ?: return emptyList()
        if (!guiDir.isDirectory) {
            return emptyList()
        }
        return guiDir.walkTopDown()
            .filter { it.isFile && it.extension.lowercase() in setOf("yml", "yaml") }
            .mapNotNull { file ->
                val yaml = runCatching { YamlConfiguration.loadConfiguration(file) }.getOrNull()
                val root = yaml?.getConfigurationSection(guiName)
                if (root == null) null else file to root
            }
            .toList()
    }

    private fun availableParts(screen: Any, guiName: String): JsonArray =
        JsonArray().apply {
            appendAll(listParts(screen))
            appendAll(listYamlParts(guiName))
        }

    private fun JsonArray.appendAll(other: JsonArray) {
        for (element in other) {
            add(element)
        }
    }

    private fun yamlPartSummary(part: YamlPart): JsonObject = JsonObject().apply {
        addProperty("source", "yaml")
        addProperty("sourceFile", part.file.absolutePath)
        addProperty("path", part.path)
        addProperty("indexName", part.section.name)
        addProperty("realName", part.section.name)
        addProperty("className", part.section.getString("type"))
        addProperty("simpleClassName", part.section.getString("type"))
        add("dosTypes", availableYamlDosTypes(part.section))
    }

    private fun availableYamlDosTypes(section: ConfigurationSection): JsonArray = JsonArray().apply {
        listOf("click", "click_release", "hover", "leave", "right", "right_release", "middle")
            .forEach { dosType ->
                if (readYamlDos(section, dosType).isNotEmpty()) {
                    add(dosType)
                }
            }
    }

    private fun readYamlDos(section: ConfigurationSection, dosType: String): List<String> {
        val key = yamlDosKey(dosType) ?: return emptyList()
        val value = section.get(key) ?: return emptyList()
        return when (value) {
            is Iterable<*> -> value.mapNotNull { it?.toString() }.filter { it.isNotBlank() }
            is Array<*> -> value.mapNotNull { it?.toString() }.filter { it.isNotBlank() }
            else -> listOf(value.toString()).filter { it.isNotBlank() }
        }
    }

    private fun yamlDosKey(dosType: String): String? = when (dosType) {
        "click" -> "clickDos"
        "click_release" -> "clickReleaseDos"
        "hover" -> "hoverDos"
        "leave" -> "leaveDos"
        "right" -> "rightDos"
        "right_release" -> "rightReleaseDos"
        "middle" -> "middleDos"
        else -> null
    }

    private fun resolvePlaceholders(player: Player, text: String): String {
        if (!Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            return text
        }
        return runCatching {
            val clazz = Class.forName("me.clip.placeholderapi.PlaceholderAPI")
            val method = clazz.getMethod("setPlaceholders", OfflinePlayer::class.java, String::class.java)
            method.invoke(null, player, text)?.toString() ?: text
        }.getOrDefault(text)
    }

    private fun executeDos(player: Player, dos: List<String>, mode: String) {
        when (mode) {
            "packet" -> {
                val clazz = Class.forName("com.germ.germplugin.api.GermPacketAPI")
                clazz.getMethod("sendGuiDos", Player::class.java, java.util.List::class.java).invoke(null, player, dos)
            }
            "command_util" -> {
                val clazz = Class.forName("com.germ.germplugin.api.util.CommandUtil")
                clazz.getMethod("execute", Player::class.java, java.util.List::class.java).invoke(null, player, dos)
            }
            "player_command" -> executePlayerCommandDos(player, dos)
            else -> throw IllegalArgumentException("Unsupported mode: $mode")
        }
    }

    private fun executePlayerCommandDos(player: Player, dos: List<String>) {
        for (entry in dos) {
            val command = entry.substringAfter("playercmd<->", missingDelimiterValue = "")
                .takeIf { it.isNotBlank() }
                ?: throw IllegalArgumentException("player_command mode only supports playercmd<-> dos: $entry")
            player.performCommand(command.removePrefix("/"))
        }
    }

    private fun data(
        guiName: String,
        partId: String,
        dosType: String,
        rawDos: List<String>,
        resolvedDos: List<String>,
        executed: Boolean,
        mode: String,
        part: JsonObject?,
        availableParts: JsonArray?
    ): JsonObject = JsonObject().apply {
        addProperty("guiName", guiName)
        addProperty("partId", partId)
        addProperty("dosType", dosType)
        addProperty("execute", executed)
        addProperty("mode", mode)
        add("rawDos", rawDos.toJsonArray())
        add("resolvedDos", resolvedDos.toJsonArray())
        if (part != null) {
            add("part", part)
        }
        if (availableParts != null) {
            add("availableParts", availableParts)
        }
    }

    private fun List<String>.toJsonArray(): JsonArray = JsonArray().also { array ->
        forEach { array.add(it) }
    }

    private fun JsonObject.string(name: String): String? =
        get(name)?.takeIf { !it.isJsonNull }?.asString?.takeIf { it.isNotBlank() }

    private fun JsonObject.boolean(name: String, default: Boolean): Boolean =
        get(name)?.takeIf { !it.isJsonNull }?.asBoolean ?: default

    private fun failure(id: String, message: String, data: JsonObject? = null): ResponseMessage =
        ResponseMessage(id, "failure", message, data)
}
