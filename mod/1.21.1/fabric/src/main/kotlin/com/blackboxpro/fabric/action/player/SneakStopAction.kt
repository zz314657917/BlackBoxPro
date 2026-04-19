package com.blackboxpro.fabric.action.player

import com.blackboxpro.fabric.action.ActionExecutor
import com.blackboxpro.fabric.action.ActionResult
import com.google.gson.JsonObject
import net.minecraft.client.MinecraftClient
import net.minecraft.network.packet.c2s.play.PlayerInputC2SPacket

class SneakStopAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val client = MinecraftClient.getInstance()
        val networkHandler = client.networkHandler
            ?: return ActionResult.fail("Not connected to server")

        networkHandler.sendPacket(PlayerInputC2SPacket(0.0f, 0.0f, false, false))
        return ActionResult.ok("Stopped sneaking")
    }
}
