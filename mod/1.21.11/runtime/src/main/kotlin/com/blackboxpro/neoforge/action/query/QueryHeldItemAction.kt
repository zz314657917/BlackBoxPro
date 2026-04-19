package com.blackboxpro.neoforge.action.query

import com.blackboxpro.neoforge.action.ActionExecutor
import com.blackboxpro.neoforge.action.ActionResult
import com.blackboxpro.neoforge.util.ItemStackSerializer
import com.blackboxpro.neoforge.util.getStringOrNull
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.world.InteractionHand

/**
 * 读取手持物品详情。
 * Action ID: "query_held_item"
 */
class QueryHeldItemAction : ActionExecutor {

    override fun execute(params: JsonObject): ActionResult {
        val client = Minecraft.getInstance()
        val player = client.player
            ?: return ActionResult.fail("Player not available")

        val handStr = params.getStringOrNull("hand") ?: "main_hand"
        val hand = when (handStr.lowercase()) {
            "main_hand", "mainhand", "main" -> InteractionHand.MAIN_HAND
            "off_hand", "offhand", "off" -> InteractionHand.OFF_HAND
            else -> return ActionResult.fail("Invalid hand: $handStr")
        }

        val stack = player.getItemInHand(hand)
        val data = ItemStackSerializer.serialize(stack)

        return ActionResult.ok("Held item queried", data)
    }
}
