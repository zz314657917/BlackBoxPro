package com.blackboxpro.neoforge.action.query

import com.blackboxpro.neoforge.action.ActionExecutor
import com.blackboxpro.neoforge.action.ActionResult
import com.blackboxpro.neoforge.util.ChatStyleHelper
import com.blackboxpro.neoforge.util.getBooleanOrDefault
import com.blackboxpro.neoforge.util.requireInt
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.item.Item
import net.minecraft.world.item.TooltipFlag

class QuerySlotTooltipAction : ActionExecutor {

    override fun execute(params: JsonObject): ActionResult {
        val slotIndex = params.requireInt("slot")
        val advanced = params.getBooleanOrDefault("advanced", false)

        val client = Minecraft.getInstance()
        val player = client.player ?: return ActionResult.fail("Player not available")
        val menu = when (val screen = client.screen) {
            is AbstractContainerScreen<*> -> screen.menu
            else -> player.containerMenu
        }

        if (slotIndex < 0 || slotIndex >= menu.slots.size) {
            return ActionResult.fail("Slot index $slotIndex out of range [0, ${menu.slots.size})")
        }

        val stack = menu.slots[slotIndex].item
        if (stack.isEmpty) {
            return ActionResult.ok("Slot $slotIndex is empty", JsonObject().apply {
                addProperty("slot", slotIndex)
                addProperty("empty", true)
            })
        }

        val tooltipLines = client.level?.let { level ->
            val tooltipFlag = if (advanced) TooltipFlag.Default.ADVANCED else TooltipFlag.Default.NORMAL
            stack.getTooltipLines(Item.TooltipContext.of(level), player, tooltipFlag)
        } ?: net.minecraft.client.gui.screens.Screen.getTooltipFromItem(client, stack)

        val data = JsonObject().apply {
            addProperty("slot", slotIndex)
            addProperty("empty", false)
            addProperty("itemId", BuiltInRegistries.ITEM.getKey(stack.item).toString())
            addProperty("itemName", stack.hoverName.string)
            addProperty("count", stack.count)
            addProperty("damage", stack.damageValue)
            addProperty("maxDamage", stack.maxDamage)
            add("tooltip", JsonArray().apply {
                tooltipLines.forEach { add(it.string) }
            })
            add("tooltipFormatted", JsonArray().apply {
                tooltipLines.forEach { add(ChatStyleHelper.componentToLegacyString(it)) }
            })
            add("tooltipJson", JsonArray().apply {
                tooltipLines.forEach { add(ChatStyleHelper.serializeComponent(it)) }
            })
        }
        return ActionResult.ok("Tooltip queried for slot $slotIndex", data)
    }
}
