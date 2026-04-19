package com.blackboxpro.forge.action.query

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.blackboxpro.forge.util.ItemStackSerializer
import com.blackboxpro.forge.util.getStringOrNull
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.util.EnumHand

class QueryHeldItemAction : ActionExecutor {

    override fun execute(params: JsonObject): ActionResult {
        val mc = Minecraft.getMinecraft()
        val player = mc.player
            ?: return ActionResult.fail("Player not available")

        val handStr = params.getStringOrNull("hand") ?: "main_hand"
        val hand = when (handStr.lowercase()) {
            "main_hand", "mainhand", "main" -> EnumHand.MAIN_HAND
            "off_hand", "offhand", "off" -> EnumHand.OFF_HAND
            else -> return ActionResult.fail("Invalid hand: $handStr")
        }

        val stack = player.getHeldItem(hand)
        val data = ItemStackSerializer.serialize(stack)

        return ActionResult.ok("Held item queried", data)
    }
}
