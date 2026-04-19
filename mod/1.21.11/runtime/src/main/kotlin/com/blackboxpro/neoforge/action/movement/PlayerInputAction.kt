package com.blackboxpro.neoforge.action.movement

import com.blackboxpro.neoforge.action.ActionExecutor
import com.blackboxpro.neoforge.action.ActionResult
import com.blackboxpro.neoforge.util.getBooleanOrDefault
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket
import net.minecraft.world.entity.player.Input

class PlayerInputAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val forward = params.getBooleanOrDefault("forward", false)
        val backward = params.getBooleanOrDefault("backward", false)
        val left = params.getBooleanOrDefault("left", false)
        val right = params.getBooleanOrDefault("right", false)
        val jump = params.getBooleanOrDefault("jump", false)
        val sneak = params.getBooleanOrDefault("sneak", false)
        val sprint = params.getBooleanOrDefault("sprint", false)

        val networkHandler = Minecraft.getInstance().connection
            ?: return ActionResult.fail("Not connected to server")

        networkHandler.send(
            ServerboundPlayerInputPacket(Input(forward, backward, left, right, jump, sneak, sprint))
        )
        return ActionResult.ok()
    }
}
