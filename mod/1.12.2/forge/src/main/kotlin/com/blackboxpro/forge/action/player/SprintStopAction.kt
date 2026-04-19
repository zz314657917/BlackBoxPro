package com.blackboxpro.forge.action.player

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.network.play.client.CPacketEntityAction

class SprintStopAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val mc = Minecraft.getMinecraft()
        val player = mc.player
            ?: return ActionResult.fail("Player not available")
        val connection = mc.connection
            ?: return ActionResult.fail("Not connected to server")

        player.isSprinting = false
        connection.sendPacket(CPacketEntityAction(player, CPacketEntityAction.Action.STOP_SPRINTING))
        return ActionResult.ok("Stopped sprinting")
    }
}
