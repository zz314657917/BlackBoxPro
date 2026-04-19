package com.blackboxpro.neoforge.action.block

import com.blackboxpro.neoforge.action.ActionExecutor
import com.blackboxpro.neoforge.action.ActionResult
import com.blackboxpro.neoforge.util.HandUtil
import com.blackboxpro.neoforge.util.getIntOrDefault
import com.blackboxpro.neoforge.util.getStringOrNull
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.network.protocol.game.ServerboundUseItemPacket

class UseItemAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val hand = HandUtil.fromString(params.getStringOrNull("hand") ?: "main_hand")
        val sequence = params.getIntOrDefault("sequence", 0)

        val client = Minecraft.getInstance()
        val handler = client.connection
            ?: return ActionResult.fail("Not connected to server")
        val player = client.player
            ?: return ActionResult.fail("Player not available")

        handler.send(ServerboundUseItemPacket(hand, sequence, player.yRot, player.xRot))
        return ActionResult.ok("Used item")
    }
}
