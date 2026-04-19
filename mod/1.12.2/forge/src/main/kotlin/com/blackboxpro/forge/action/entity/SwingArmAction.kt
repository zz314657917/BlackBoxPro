package com.blackboxpro.forge.action.entity

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.blackboxpro.forge.util.HandUtil
import com.blackboxpro.forge.util.getStringOrNull
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft

class SwingArmAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val hand = HandUtil.fromString(params.getStringOrNull("hand") ?: "main_hand")

        val player = Minecraft.getMinecraft().player
            ?: return ActionResult.fail("Player not available")

        // swingArm 同时播放客户端动画 + 发送 CPacketAnimation
        player.swingArm(hand)
        return ActionResult.ok("Swung arm with ${hand.name.lowercase()}")
    }
}
