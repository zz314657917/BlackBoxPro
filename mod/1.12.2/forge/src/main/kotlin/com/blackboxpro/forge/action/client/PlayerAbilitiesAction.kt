package com.blackboxpro.forge.action.client

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.blackboxpro.forge.util.requireBoolean
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.network.play.client.CPacketPlayerAbilities

class PlayerAbilitiesAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val flying = params.requireBoolean("flying")

        val mc = Minecraft.getMinecraft()
        val player = mc.player
            ?: return ActionResult.fail("Player is not available")
        val connection = mc.connection
            ?: return ActionResult.fail("Not connected to server")

        if (flying && !player.capabilities.allowFlying) {
            return ActionResult.fail("Player is not allowed to fly")
        }

        player.capabilities.isFlying = flying
        connection.sendPacket(CPacketPlayerAbilities(player.capabilities))

        return ActionResult.ok("Set flying to $flying")
    }
}
