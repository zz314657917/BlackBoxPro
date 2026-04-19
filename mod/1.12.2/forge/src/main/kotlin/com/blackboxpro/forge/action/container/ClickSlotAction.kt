package com.blackboxpro.forge.action.container

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.blackboxpro.forge.util.requireInt
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.inventory.ClickType
import net.minecraft.item.ItemStack

class ClickSlotAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val mc = Minecraft.getMinecraft()
        val player = mc.player
            ?: return ActionResult.fail("Player not available")
        val connection = mc.connection
            ?: return ActionResult.fail("Not connected to server")

        val windowId = params.requireInt("windowId")
        val slot = params.requireInt("slot")
        val button = params.requireInt("button")
        val mode = params.requireInt("mode")

        val clickType = when (mode) {
            0 -> ClickType.PICKUP
            1 -> ClickType.QUICK_MOVE
            2 -> ClickType.SWAP
            3 -> ClickType.CLONE
            4 -> ClickType.THROW
            5 -> ClickType.QUICK_CRAFT
            6 -> ClickType.PICKUP_ALL
            else -> return ActionResult.fail("Invalid mode: $mode (expected 0-6)")
        }

        // 自动获取事务 ID 和点击的物品
        val container = if (windowId == 0) player.inventoryContainer else player.openContainer
        val actionNumber = container.getNextTransactionID(player.inventory)
        val clickedItem = if (slot >= 0 && slot < container.inventorySlots.size) {
            container.inventorySlots[slot].stack
        } else {
            ItemStack.EMPTY
        }

        connection.sendPacket(
            net.minecraft.network.play.client.CPacketClickWindow(
                windowId, slot, button, clickType, clickedItem, actionNumber
            )
        )
        return ActionResult.ok("Clicked slot $slot in window $windowId (mode=$mode)")
    }
}
