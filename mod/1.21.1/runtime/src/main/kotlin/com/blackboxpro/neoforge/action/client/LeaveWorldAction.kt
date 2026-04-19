package com.blackboxpro.neoforge.action.client

import com.blackboxpro.neoforge.action.ActionExecutor
import com.blackboxpro.neoforge.action.ActionResult
import com.blackboxpro.runtime.action.AsyncClientActionSupport
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.TitleScreen

class LeaveWorldAction : ActionExecutor {

    override fun execute(params: JsonObject): ActionResult =
        ActionResult.fail("LeaveWorldAction requires commandId, use execute(params, commandId)")

    override fun execute(params: JsonObject, commandId: String): ActionResult {
        val client = Minecraft.getInstance()
        if (client.level == null && client.player == null && client.screen is TitleScreen) {
            return ActionResult.ok(
                "Already at title screen",
                JsonObject().apply { addProperty("state", "title_screen") }
            )
        }

        return AsyncClientActionSupport.startPolling(
            commandId = commandId,
            executeOnMainThread = { task -> client.execute(task) },
            timeoutMs = 60_000L,
            timeoutMessage = "Timed out waiting to leave the current world",
            startAction = {
                if (client.level != null) {
                    client.level!!.disconnect()
                    client.disconnect()
                }
                client.setScreen(TitleScreen())
            },
            poll = {
                if (client.level == null && client.player == null && client.screen is TitleScreen) {
                    ActionResult.ok(
                        "Returned to title screen",
                        JsonObject().apply { addProperty("state", "title_screen") }
                    )
                } else {
                    null
                }
            }
        )
    }
}
