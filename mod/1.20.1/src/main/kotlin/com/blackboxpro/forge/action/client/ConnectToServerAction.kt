package com.blackboxpro.forge.action.client

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.blackboxpro.forge.util.requireString
import com.blackboxpro.runtime.action.AsyncClientActionSupport
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.ConnectScreen
import net.minecraft.client.gui.screens.DisconnectedScreen
import net.minecraft.client.gui.screens.Screen
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
                val serverData = ServerData("BlackBoxPro", address, false)
                ConnectScreen.startConnecting(
                    client.screen ?: TitleScreen(),
                    client,
                    ServerAddress.parseString(address),
                    serverData,
                    false
                )
            },
            poll = {
                if (client.level != null && client.player != null) {
                    ActionResult.ok("Connecting to $address")
                } else if (client.screen is DisconnectedScreen || client.screen?.javaClass?.simpleName?.contains("Disconnected") == true) {
                    ActionResult.fail("Disconnected while connecting to $address: ${describeDisconnect(client.screen)}")
                } else {
                    null
                }
            }
        )
    }

    private fun describeDisconnect(screen: Screen?): String {
        if (screen == null) return "unknown"
        val screenType = screen.javaClass.simpleName
        val reason = runCatching {
            var current: Class<*>? = screen.javaClass
            while (current != null) {
                val field = current.declaredFields.firstOrNull { it.name == "reason" }
                if (field != null) {
                    field.isAccessible = true
                    return@runCatching field.get(screen)?.toString()
                }
                current = current.superclass
            }
            null
        }.getOrNull()
        return if (reason.isNullOrBlank()) screenType else "$screenType: $reason"
    }
}
