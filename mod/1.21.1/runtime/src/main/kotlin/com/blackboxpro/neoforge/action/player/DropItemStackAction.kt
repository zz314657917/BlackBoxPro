package com.blackboxpro.neoforge.action.player

import com.blackboxpro.neoforge.action.ActionExecutor
import com.blackboxpro.neoforge.action.ActionResult
import com.blackboxpro.neoforge.util.getIntOrDefault
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction

class DropItemStackAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val sequence = params.getIntOrDefault("sequence", 0)
        val networkHandler = Minecraft.getInstance().connection
            ?: return ActionResult.fail("Not connected to server")
        networkHandler.send(
            ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.DROP_ALL_ITEMS, BlockPos.ZERO, Direction.DOWN, sequence)
        )
        return ActionResult.ok("Dropped entire item stack")
    }
}
