package com.blackboxpro.forge.action.player

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.network.play.client.CPacketClientStatus

class PerformRespawnAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val connection = Minecraft.getMinecraft().connection
            ?: return ActionResult.fail("Not connected to server")

        connection.sendPacket(CPacketClientStatus(CPacketClientStatus.State.PERFORM_RESPAWN))
        return ActionResult.ok("Performed respawn")
    }
}
