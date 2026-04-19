package com.blackboxpro.neoforge.action.player

import com.blackboxpro.neoforge.action.ActionExecutor
import com.blackboxpro.neoforge.action.ActionResult
import com.blackboxpro.neoforge.util.getIntOrDefault
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction

class SwapHandsAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val sequence = params.getIntOrDefault("sequence", 0)
        val networkHandler = Minecraft.getInstance().connection
            ?: return ActionResult.fail("Not connected to server")
        networkHandler.send(
            ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ZERO, Direction.DOWN, sequence)
        )
        return ActionResult.ok("Swapped items between hands")
    }
}
