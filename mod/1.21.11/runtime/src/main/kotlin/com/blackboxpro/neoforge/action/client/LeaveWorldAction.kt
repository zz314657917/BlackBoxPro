package com.blackboxpro.neoforge.action.client

import com.blackboxpro.neoforge.action.ActionExecutor
import com.blackboxpro.neoforge.action.ActionResult
import com.blackboxpro.runtime.action.AsyncClientActionSupport
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.GenericMessageScreen
import net.minecraft.client.gui.screens.TitleScreen
import net.minecraft.network.chat.Component

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
                client.level?.let { level ->
                    level.disconnect(Component.translatable("menu.savingLevel"))
                    client.disconnect(GenericMessageScreen(Component.translatable("menu.savingLevel")), false)
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
