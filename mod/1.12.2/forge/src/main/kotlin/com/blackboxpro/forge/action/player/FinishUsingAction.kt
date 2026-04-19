package com.blackboxpro.forge.action.player

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.network.play.client.CPacketPlayerDigging
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.BlockPos

class FinishUsingAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val mc = Minecraft.getMinecraft()
        val connection = mc.connection
            ?: return ActionResult.fail("Not connected to server")
        val player = mc.player
            ?: return ActionResult.fail("Player not available")

        // 同步客户端状态：释放正在使用的物品（弓箭蓄力等）
        player.stopActiveHand()

        connection.sendPacket(
            CPacketPlayerDigging(CPacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN)
        )
        return ActionResult.ok("Finished using item")
    }
}
