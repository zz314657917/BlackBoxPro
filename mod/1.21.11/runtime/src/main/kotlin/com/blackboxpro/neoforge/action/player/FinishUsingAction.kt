package com.blackboxpro.neoforge.action.player

import com.blackboxpro.neoforge.action.ActionExecutor
import com.blackboxpro.neoforge.action.ActionResult
import com.blackboxpro.neoforge.util.getIntOrDefault
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction

class FinishUsingAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val sequence = params.getIntOrDefault("sequence", 0)
        val client = Minecraft.getInstance()
        val networkHandler = client.connection
            ?: return ActionResult.fail("Not connected to server")
        val player = client.player
            ?: return ActionResult.fail("Player not available")

        // 同步客户端状态：释放正在使用的物品（弓箭蓄力等）
        player.releaseUsingItem()

        networkHandler.send(
            ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.RELEASE_USE_ITEM, BlockPos.ZERO, Direction.DOWN, sequence)
        )
        return ActionResult.ok("Finished using item")
    }
}
