package com.blackboxpro.neoforge.action.client

import com.blackboxpro.neoforge.action.ActionExecutor
import com.blackboxpro.neoforge.action.ActionResult
import com.blackboxpro.neoforge.util.requireBoolean
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.network.protocol.game.ServerboundPlayerAbilitiesPacket

class PlayerAbilitiesAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val flying = params.requireBoolean("flying")

        val client = Minecraft.getInstance()
        val player = client.player
            ?: return ActionResult.fail("Player is not available")
        val networkHandler = client.connection
            ?: return ActionResult.fail("Not connected to server")

        if (flying && !player.abilities.mayfly) {
            return ActionResult.fail("Player is not allowed to fly")
        }

        player.abilities.flying = flying
        networkHandler.send(ServerboundPlayerAbilitiesPacket(player.abilities))

        return ActionResult.ok("Set flying to $flying")
    }
}
