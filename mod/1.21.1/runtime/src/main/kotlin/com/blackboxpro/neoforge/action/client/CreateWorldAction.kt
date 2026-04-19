package com.blackboxpro.neoforge.action.client

import com.blackboxpro.neoforge.action.ActionExecutor
import com.blackboxpro.neoforge.action.ActionResult
import com.blackboxpro.neoforge.util.getBooleanOrDefault
import com.blackboxpro.neoforge.util.getStringOrNull
import com.blackboxpro.neoforge.util.requireString
import com.blackboxpro.runtime.action.AsyncClientActionSupport
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.TitleScreen
import net.minecraft.world.Difficulty
import net.minecraft.world.level.GameType
import net.minecraft.world.level.LevelSettings
import net.minecraft.world.level.WorldDataConfiguration
import net.minecraft.world.level.levelgen.WorldOptions
import net.minecraft.world.level.levelgen.presets.WorldPresets
import java.nio.file.Files
import java.util.Locale
import java.util.OptionalLong

class CreateWorldAction : ActionExecutor {

    override fun execute(params: JsonObject): ActionResult =
        ActionResult.fail("CreateWorldAction requires commandId, use execute(params, commandId)")

    override fun execute(params: JsonObject, commandId: String): ActionResult {
        val client = Minecraft.getInstance()
        if (client.level != null || client.player != null) {
            return ActionResult.fail("Already in a world, leave the current world first")
        }

        val requestedWorldName = params.requireString("worldName").trim()
        validateWorldName(requestedWorldName)?.let { return ActionResult.fail(it) }

        val worldDir = client.levelSource.baseDir.resolve(requestedWorldName)
        if (Files.exists(worldDir.resolve("level.dat")) || Files.exists(worldDir.resolve("level.dat_old"))) {
            return ActionResult.fail("World already exists: $requestedWorldName")
        }

        val mode = parseMode(params.getStringOrNull("gameMode"))
        val difficulty = parseDifficulty(params.getStringOrNull("difficulty"))
        val allowCommands = params.getBooleanOrDefault("allowCommands", mode.gameType == GameType.CREATIVE)
        val generateStructures = params.getBooleanOrDefault("generateStructures", true)
        val bonusChest = params.getBooleanOrDefault("bonusChest", false)
        val seed = parseSeed(params.getStringOrNull("seed"))
        val actualSeed = if (seed.isPresent) seed.asLong else WorldOptions.randomSeed()

        return AsyncClientActionSupport.startPolling(
            commandId = commandId,
            executeOnMainThread = { task -> client.execute(task) },
            timeoutMs = 90_000L,
            timeoutMessage = "Timed out waiting for world creation: $requestedWorldName",
            startAction = {
                val levelSettings = createLevelSettings(
                    requestedWorldName,
                    mode,
                    difficulty,
                    allowCommands
                )
                val worldOptions = WorldOptions(actualSeed, generateStructures, bonusChest)
                client.createWorldOpenFlows().createFreshLevel(
                    requestedWorldName,
                    levelSettings,
                    worldOptions,
                    WorldPresets::createNormalWorldDimensions,
                    TitleScreen()
                )
            },
            poll = {
                if (client.level != null && client.player != null) {
                    ActionResult.ok(
                        "Created and entered world: $requestedWorldName",
                        JsonObject().apply {
                            addProperty("worldName", requestedWorldName)
                            addProperty("state", "in_world")
                        }
                    )
                } else {
                    null
                }
            }
        )
    }

    private fun parseMode(raw: String?): WorldMode =
        when ((raw ?: "survival").trim().lowercase(Locale.ROOT)) {
            "survival" -> WorldMode(GameType.SURVIVAL, false)
            "creative" -> WorldMode(GameType.CREATIVE, false)
            "hardcore" -> WorldMode(GameType.SURVIVAL, true)
            else -> throw IllegalArgumentException("Invalid gameMode: $raw")
        }

    private fun parseDifficulty(raw: String?): Difficulty =
        when ((raw ?: "normal").trim().lowercase(Locale.ROOT)) {
            "peaceful" -> Difficulty.PEACEFUL
            "easy" -> Difficulty.EASY
            "normal" -> Difficulty.NORMAL
            "hard" -> Difficulty.HARD
            else -> throw IllegalArgumentException("Invalid difficulty: $raw")
        }

    private fun parseSeed(raw: String?): OptionalLong =
        raw?.takeIf { it.isNotBlank() }?.let(WorldOptions::parseSeed) ?: OptionalLong.empty()

    private fun createLevelSettings(
        worldName: String,
        mode: WorldMode,
        difficulty: Difficulty,
        allowCommands: Boolean
    ): LevelSettings {
        val gameRulesClass = Class.forName("net.minecraft.world.level.GameRules")
        val gameRules = gameRulesClass.getDeclaredConstructor().newInstance()
        val constructor = LevelSettings::class.java.getConstructor(
            String::class.java,
            GameType::class.java,
            Boolean::class.javaPrimitiveType,
            Difficulty::class.java,
            Boolean::class.javaPrimitiveType,
            gameRulesClass,
            WorldDataConfiguration::class.java
        )
        return constructor.newInstance(
            worldName,
            mode.gameType,
            mode.hardcore,
            difficulty,
            allowCommands,
            gameRules,
            WorldDataConfiguration.DEFAULT
        ) as LevelSettings
    }

    private fun validateWorldName(worldName: String): String? = when {
        worldName.isBlank() -> "worldName cannot be blank"
        INVALID_WORLD_NAME.any(worldName::contains) -> "worldName contains invalid filesystem characters"
        worldName.contains("..") -> "worldName cannot contain '..'"
        else -> null
    }

    private data class WorldMode(val gameType: GameType, val hardcore: Boolean)

    companion object {
        private val INVALID_WORLD_NAME = charArrayOf('<', '>', ':', '"', '/', '\\', '|', '?', '*')
    }
}
