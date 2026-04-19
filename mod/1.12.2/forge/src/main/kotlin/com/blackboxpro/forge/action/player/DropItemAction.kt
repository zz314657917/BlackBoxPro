package com.blackboxpro.forge.action.player

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.network.play.client.CPacketPlayerDigging
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.BlockPos

class DropItemAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val connection = Minecraft.getMinecraft().connection
            ?: return ActionResult.fail("Not connected to server")

        connection.sendPacket(
            CPacketPlayerDigging(CPacketPlayerDigging.Action.DROP_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN)
        )
        return ActionResult.ok("Dropped one item")
    }
}
