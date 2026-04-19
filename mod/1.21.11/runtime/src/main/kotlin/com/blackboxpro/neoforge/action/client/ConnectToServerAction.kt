package com.blackboxpro.neoforge.action.client

import com.blackboxpro.neoforge.action.ActionExecutor
import com.blackboxpro.neoforge.action.ActionResult
import com.blackboxpro.neoforge.util.requireString
import com.blackboxpro.runtime.action.AsyncClientActionSupport
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.ConnectScreen
import net.minecraft.client.gui.screens.TitleScreen
import net.minecraft.client.multiplayer.ServerData
import net.minecraft.client.multiplayer.resolver.ServerAddress

class ConnectToServerAction : ActionExecutor {

    override fun execute(params: JsonObject): ActionResult =
        ActionResult.fail("ConnectToServerAction requires commandId, use execute(params, commandId)")

    override fun execute(params: JsonObject, commandId: String): ActionResult {
        val ip = params.requireString("ip")
        val port = params.get("port")?.asInt ?: 25565
        val address = "$ip:$port"

        val client = Minecraft.getInstance()
        if (client.level != null) {
            return ActionResult.fail("Already in a world, disconnect first")
        }

        return AsyncClientActionSupport.startPolling(
            commandId = commandId,
            executeOnMainThread = { task -> client.execute(task) },
            timeoutMs = 30_000L,
            timeoutMessage = "Timed out connecting to $address",
            startAction = {
                client.execute {
                    val serverData = ServerData("BlackBoxPro", address, ServerData.Type.OTHER)
                    ConnectScreen.startConnecting(client.screen ?: TitleScreen(), client, ServerAddress.parseString(address), serverData, false, null)
                }
            },
            poll = {
                if (client.level != null && client.player != null) {
                    ActionResult.ok("Connecting to $address")
                } else {
                    null
                }
            }
        )
    }
}
