package com.blackboxpro.forge.action.movement

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.blackboxpro.forge.util.getBooleanOrDefault
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.network.play.client.CPacketInput

class PlayerInputAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val forward = params.getBooleanOrDefault("forward", false)
        val backward = params.getBooleanOrDefault("backward", false)
        val left = params.getBooleanOrDefault("left", false)
        val right = params.getBooleanOrDefault("right", false)
        val jump = params.getBooleanOrDefault("jump", false)
        val sneak = params.getBooleanOrDefault("sneak", false)

        // 1.12.2 CPacketInput takes float strafe, float forward, boolean jumping, boolean sneaking
        val strafeValue = when {
            left && !right -> 1.0f
            right && !left -> -1.0f
            else -> 0.0f
        }
        val forwardValue = when {
            forward && !backward -> 1.0f
            backward && !forward -> -1.0f
            else -> 0.0f
        }

        val connection = Minecraft.getMinecraft().connection
            ?: return ActionResult.fail("Not connected to server")

        connection.sendPacket(CPacketInput(strafeValue, forwardValue, jump, sneak))
        return ActionResult.ok()
    }
}
