package com.blackboxpro.neoforge.action.client

import com.blackboxpro.neoforge.action.ActionExecutor
import com.blackboxpro.neoforge.action.ActionResult
import com.blackboxpro.neoforge.util.requireString
import com.blackboxpro.runtime.action.AsyncClientActionSupport
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.TitleScreen
import net.minecraft.world.level.storage.LevelStorageException
import net.minecraft.world.level.storage.LevelSummary
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

class JoinWorldAction : ActionExecutor {

    override fun execute(params: JsonObject): ActionResult =
        ActionResult.fail("JoinWorldAction requires commandId, use execute(params, commandId)")

    override fun execute(params: JsonObject, commandId: String): ActionResult {
        val client = Minecraft.getInstance()
        if (client.level != null || client.player != null) {
            return ActionResult.fail("Already in a world, leave the current world first")
        }

        val requestedWorldName = params.requireString("worldName").trim()
        if (requestedWorldName.isBlank()) {
            return ActionResult.fail("worldName cannot be blank")
        }

        val started = AtomicBoolean(false)
        val targetWorldId = AtomicReference<String?>(null)
        val targetDisplayName = AtomicReference<String?>(null)
        val terminal = AtomicReference<ActionResult?>(null)

        return AsyncClientActionSupport.startPolling(
            commandId = commandId,
            executeOnMainThread = { task -> client.execute(task) },
            timeoutMs = 90_000L,
            timeoutMessage = "Timed out waiting to join world: $requestedWorldName",
            startAction = {
                val candidates = try {
                    client.levelSource.findLevelCandidates()
                } catch (e: LevelStorageException) {
                    terminal.set(ActionResult.fail("Failed to enumerate local worlds: ${e.message}"))
                    return@startPolling
                }
                client.levelSource.loadLevelSummaries(candidates)
                    .whenComplete { summaries, throwable ->
                        if (throwable != null) {
                            terminal.set(ActionResult.fail("Failed to load local worlds: ${throwable.message}"))
                            return@whenComplete
                        }
                        val target = summaries.orEmpty().firstOrNull { matches(it, requestedWorldName) }
                        if (target == null) {
                            terminal.set(ActionResult.fail("World not found: $requestedWorldName"))
                            return@whenComplete
                        }
                        if (!target.primaryActionActive()) {
                            terminal.set(ActionResult.fail("World is not loadable: ${target.levelName}"))
                            return@whenComplete
                        }
                        targetWorldId.set(target.levelId)
                        targetDisplayName.set(target.levelName)
                        client.execute {
                            started.set(true)
                            client.createWorldOpenFlows().openWorld(target.levelId) {
                                terminal.set(ActionResult.fail("Failed to join world: ${target.levelName}"))
                                client.setScreen(TitleScreen())
                            }
                        }
                    }
            },
            poll = {
                terminal.get()?.let { return@startPolling it }
                if (!started.get()) {
                    return@startPolling null
                }
                if (client.level != null && client.player != null) {
                    val worldId = targetWorldId.get() ?: requestedWorldName
                    val displayName = targetDisplayName.get() ?: requestedWorldName
                    ActionResult.ok(
                        "Joined world: $displayName",
                        JsonObject().apply {
                            addProperty("worldId", worldId)
                            addProperty("worldName", displayName)
                            addProperty("state", "in_world")
                        }
                    )
                } else {
                    null
                }
            }
        )
    }

    private fun matches(summary: LevelSummary, requestedWorldName: String): Boolean {
        val normalized = requestedWorldName.lowercase(Locale.ROOT)
        return summary.levelId.lowercase(Locale.ROOT) == normalized ||
            summary.levelName.lowercase(Locale.ROOT) == normalized
    }
}
