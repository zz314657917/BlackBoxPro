package com.blackboxpro.fabric.action.movement

import com.blackboxpro.fabric.action.ActionExecutor
import com.blackboxpro.fabric.action.ActionResult
import com.blackboxpro.fabric.util.getBooleanOrDefault
import com.google.gson.JsonObject
import net.minecraft.client.MinecraftClient
import net.minecraft.network.packet.c2s.play.PlayerInputC2SPacket

class PlayerInputAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val forward = params.getBooleanOrDefault("forward", false)
        val backward = params.getBooleanOrDefault("backward", false)
        val left = params.getBooleanOrDefault("left", false)
        val right = params.getBooleanOrDefault("right", false)
        val jump = params.getBooleanOrDefault("jump", false)
        val sneak = params.getBooleanOrDefault("sneak", false)

        val networkHandler = MinecraftClient.getInstance().networkHandler
            ?: return ActionResult.fail("Not connected to server")

        val sideways = when {
            left == right -> 0.0f
            left -> -1.0f
            else -> 1.0f
        }
        val forwardSpeed = when {
            forward == backward -> 0.0f
            forward -> 1.0f
            else -> -1.0f
        }

        networkHandler.sendPacket(PlayerInputC2SPacket(sideways, forwardSpeed, jump, sneak))
        return ActionResult.ok()
    }
}
