package com.blackboxpro.forge.action.entity

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.blackboxpro.forge.util.getStringOrNull
import com.blackboxpro.forge.util.HandUtil
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft

class SwingArmAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val hand = HandUtil.fromString(params.getStringOrNull("hand") ?: "main_hand")

        val player = Minecraft.getInstance().player
            ?: return ActionResult.fail("Player not available")

        // swing 鍚屾椂鎾斁瀹㈡埛绔姩鐢?+ 鍙戦€?ServerboundSwingPacket
        player.swing(hand)
        return ActionResult.ok("Swung arm with ${hand.name.lowercase()}")
    }
}

