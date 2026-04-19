package com.blackboxpro.forge.action.client

import com.blackboxpro.common.runtime.dispatcher.RuntimeResponseSender
import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.blackboxpro.forge.util.getBooleanOrDefault
import com.blackboxpro.forge.util.getStringOrNull
import com.blackboxpro.forge.util.requireString
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.world.GameType
import net.minecraft.world.WorldSettings
import net.minecraft.world.WorldType
import org.apache.logging.log4j.LogManager
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class CreateWorldAction : ActionExecutor {

    private val logger = LogManager.getLogger("BlackBoxPro-CreateWorldAction")

    override fun execute(params: JsonObject): ActionResult =
        ActionResult.fail("CreateWorldAction requires commandId, use execute(params, commandId)")

    override fun execute(params: JsonObject, commandId: String): ActionResult {
        val mc = Minecraft.getMinecraft()
        if (mc.world != null || mc.player != null) {
            return ActionResult.fail("Already in a world, leave the current world first")
        }

        val requestedWorldName = params.requireString("worldName").trim()
        validateWorldName(requestedWorldName)?.let { return ActionResult.fail(it) }

        val saveLoader = mc.getSaveLoader()
        val worldInfo = saveLoader.getWorldInfo(requestedWorldName)
        if (worldInfo != null) {
            return ActionResult.fail("World already exists: $requestedWorldName")
        }

        val gameType = parseGameType(params.getStringOrNull("gameMode"))
            ?: return ActionResult.fail("Invalid gameMode: ${params.getStringOrNull("gameMode")}")
        val allowCommands = params.getBooleanOrDefault("allowCommands", gameType == GameType.CREATIVE)
        val generateStructures = params.getBooleanOrDefault("generateStructures", true)
        val bonusChest = params.getBooleanOrDefault("bonusChest", false)
        val seedText = params.getStringOrNull("seed")?.trim()?.takeIf { it.isNotEmpty() }

        val seed = if (seedText != null) {
            seedText.toLongOrNull() ?: seedText.hashCode().toLong()
        } else {
            System.nanoTime()
        }

        val worldSettings = WorldSettings(seed, gameType, generateStructures, false, WorldType.DEFAULT)
        if (allowCommands) worldSettings.enableCommands()
        if (bonusChest) worldSettings.enableBonusChest()

        val completed = AtomicBoolean(false)

        mc.addScheduledTask {
            try {
                mc.launchIntegratedServer(requestedWorldName, requestedWorldName, worldSettings)
            } catch (t: Throwable) {
                logger.error("Failed to launch integrated server", t)
                if (completed.compareAndSet(false, true)) {
                    RuntimeResponseSender.sendResponse(
                        id = commandId,
                        status = "failure",
                        message = "Failed to create world: ${t.message}"
                    )
                }
            }
        }

        val scheduler = Executors.newSingleThreadScheduledExecutor { r ->
            Thread(r, "BlackBoxPro-CreateWorld").apply { isDaemon = true }
        }
        val startedAt = System.nanoTime()

        fun poll() {
            if (completed.get()) {
                scheduler.shutdown()
                return
            }
            val elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt)
            if (elapsed >= 90_000L) {
                if (completed.compareAndSet(false, true)) {
                    RuntimeResponseSender.sendResponse(
                        id = commandId,
                        status = "failure",
                        message = "Timed out waiting for world creation: $requestedWorldName"
                    )
                }
                scheduler.shutdown()
                return
            }
            mc.addScheduledTask {
                if (completed.get()) return@addScheduledTask
                if (mc.world != null && mc.player != null) {
                    if (completed.compareAndSet(false, true)) {
                        RuntimeResponseSender.sendResponse(
                            id = commandId,
                            status = "success",
                            message = "Created and entered world: $requestedWorldName",
                            data = JsonObject().apply {
                                addProperty("worldName", requestedWorldName)
                                addProperty("state", "in_world")
                            }
                        )
                    }
                    scheduler.shutdown()
                } else {
                    scheduler.schedule(::poll, 200, TimeUnit.MILLISECONDS)
                }
            }
        }

        scheduler.schedule(::poll, 500, TimeUnit.MILLISECONDS)
        return ActionResult.async()
    }

    private fun parseGameType(raw: String?): GameType? =
        when ((raw ?: "survival").trim().lowercase(Locale.ROOT)) {
            "survival" -> GameType.SURVIVAL
            "creative" -> GameType.CREATIVE
            "adventure" -> GameType.ADVENTURE
            "spectator" -> GameType.SPECTATOR
            else -> null
        }

    private fun validateWorldName(worldName: String): String? = when {
        worldName.isBlank() -> "worldName cannot be blank"
        INVALID_WORLD_NAME.any(worldName::contains) -> "worldName contains invalid filesystem characters"
        worldName.contains("..") -> "worldName cannot contain '..'"
        else -> null
    }

    companion object {
        private val INVALID_WORLD_NAME = charArrayOf('<', '>', ':', '"', '/', '\\', '|', '?', '*')
    }
}
