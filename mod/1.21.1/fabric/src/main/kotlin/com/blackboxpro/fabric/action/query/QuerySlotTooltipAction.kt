package com.blackboxpro.fabric.action.query

import com.blackboxpro.fabric.action.ActionExecutor
import com.blackboxpro.fabric.action.ActionResult
import com.blackboxpro.fabric.util.ChatStyleHelper
import com.blackboxpro.fabric.util.getBooleanOrDefault
import com.blackboxpro.fabric.util.requireInt
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.item.Item
import net.minecraft.item.tooltip.TooltipType
import net.minecraft.registry.Registries

class QuerySlotTooltipAction : ActionExecutor {

    override fun execute(params: JsonObject): ActionResult {
        val slotIndex = params.requireInt("slot")
        val advanced = params.getBooleanOrDefault("advanced", false)

        val client = MinecraftClient.getInstance()
        val player = client.player ?: return ActionResult.fail("Player not available")
        val handler = when (val screen = client.currentScreen) {
            is HandledScreen<*> -> screen.screenHandler
            else -> player.currentScreenHandler
        }

        if (slotIndex < 0 || slotIndex >= handler.slots.size) {
            return ActionResult.fail("Slot index $slotIndex out of range [0, ${handler.slots.size})")
        }

        val stack = handler.slots[slotIndex].stack
        if (stack.isEmpty) {
            return ActionResult.ok("Slot $slotIndex is empty", JsonObject().apply {
                addProperty("slot", slotIndex)
                addProperty("empty", true)
            })
        }

        val tooltipLines = client.world?.let { world ->
            val tooltipType = if (advanced) TooltipType.ADVANCED else TooltipType.BASIC
            stack.getTooltip(Item.TooltipContext.create(world), player, tooltipType)
        } ?: Screen.getTooltipFromItem(client, stack)

        val data = JsonObject().apply {
            addProperty("slot", slotIndex)
            addProperty("empty", false)
            addProperty("itemId", Registries.ITEM.getId(stack.item).toString())
            addProperty("itemName", stack.name.string)
            addProperty("count", stack.count)
            addProperty("damage", stack.damage)
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
