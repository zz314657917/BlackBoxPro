package com.blackboxpro.neoforge.action.player

import com.blackboxpro.neoforge.action.ActionExecutor
import com.blackboxpro.neoforge.action.ActionResult
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket

class PerformRespawnAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val networkHandler = Minecraft.getInstance().connection
            ?: return ActionResult.fail("Not connected to server")

        networkHandler.send(ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.PERFORM_RESPAWN))
        return ActionResult.ok("Performed respawn")
    }
}
