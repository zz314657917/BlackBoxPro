package com.blackboxpro.neoforge.action.movement

import com.blackboxpro.neoforge.action.ActionExecutor
import com.blackboxpro.neoforge.action.ActionResult
import com.blackboxpro.neoforge.util.getBooleanOrDefault
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket

class PlayerInputAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val forward = params.getBooleanOrDefault("forward", false)
        val backward = params.getBooleanOrDefault("backward", false)
        val left = params.getBooleanOrDefault("left", false)
        val right = params.getBooleanOrDefault("right", false)
        val jump = params.getBooleanOrDefault("jump", false)
        val sneak = params.getBooleanOrDefault("sneak", false)

        val networkHandler = Minecraft.getInstance().connection
            ?: return ActionResult.fail("Not connected to server")

        val xxa = when {
            left == right -> 0.0f
            left -> 1.0f
            else -> -1.0f
        }
        val zza = when {
            forward == backward -> 0.0f
            forward -> 1.0f
            else -> -1.0f
        }

        networkHandler.send(ServerboundPlayerInputPacket(xxa, zza, jump, sneak))
        return ActionResult.ok()
    }
}
