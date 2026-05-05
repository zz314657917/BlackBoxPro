package com.blackboxpro.forge.action.container

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.blackboxpro.forge.util.requireInt
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.inventory.ClickType

class ClickSlotAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val mc = Minecraft.getMinecraft()
        val player = mc.player
            ?: return ActionResult.fail("Player not available")
        val playerController = mc.playerController
            ?: return ActionResult.fail("Player controller not available")
        mc.connection
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

        val container = player.openContainer
        if (container.windowId != windowId) {
            return ActionResult.fail("Window mismatch: requested=$windowId current=${container.windowId}")
        }

        // Use the vanilla client click path so 1.12.2 computes slotClick locally
        // before PlayerControllerMP sends the final CPacketClickWindow.
        playerController.windowClick(windowId, slot, button, clickType, player)
        return ActionResult.ok("Clicked slot $slot in window $windowId (mode=$mode)")
    }
}
