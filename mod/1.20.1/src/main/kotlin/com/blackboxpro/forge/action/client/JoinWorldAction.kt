package com.blackboxpro.forge.action.client

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.blackboxpro.forge.util.requireString
import com.blackboxpro.runtime.action.AsyncClientActionSupport
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.TitleScreen

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

        return AsyncClientActionSupport.startPolling(
            commandId = commandId,
            executeOnMainThread = { task -> client.execute(task) },
            timeoutMs = 90_000L,
            timeoutMessage = "Timed out waiting to join world: $requestedWorldName",
            startAction = {
                client.execute {
                    client.createWorldOpenFlows().loadLevel(TitleScreen(), requestedWorldName)
                }
            },
            poll = {
                if (client.level != null && client.player != null) {
                    ActionResult.ok(
                        "Joined world: $requestedWorldName",
                        JsonObject().apply {
                            addProperty("worldId", requestedWorldName)
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
}
