package com.blackboxpro.neoforge.action.entity

import com.blackboxpro.neoforge.action.ActionExecutor
import com.blackboxpro.neoforge.action.ActionResult
import com.blackboxpro.neoforge.util.getStringOrNull
import com.blackboxpro.neoforge.util.HandUtil
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft

class SwingArmAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val hand = HandUtil.fromString(params.getStringOrNull("hand") ?: "main_hand")

        val player = Minecraft.getInstance().player
            ?: return ActionResult.fail("Player not available")

        // swing 同时播放客户端动画 + 发送 ServerboundSwingPacket
        player.swing(hand)
        return ActionResult.ok("Swung arm with ${hand.name.lowercase()}")
    }
}
