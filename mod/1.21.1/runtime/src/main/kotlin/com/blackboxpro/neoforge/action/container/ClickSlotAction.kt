package com.blackboxpro.neoforge.action.container

import com.blackboxpro.neoforge.action.ActionExecutor
import com.blackboxpro.neoforge.action.ActionResult
import com.blackboxpro.neoforge.util.requireInt
import com.google.gson.JsonObject
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import net.minecraft.client.Minecraft
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket
import net.minecraft.world.inventory.ClickType
import net.minecraft.world.item.ItemStack

class ClickSlotAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val windowId = params.requireInt("windowId")
        val stateId = params.requireInt("stateId")
        val slot = params.requireInt("slot")
        val button = params.requireInt("button")
        val mode = params.requireInt("mode")

        val actionType = when (mode) {
            0 -> ClickType.PICKUP
            1 -> ClickType.QUICK_MOVE
            2 -> ClickType.SWAP
            3 -> ClickType.CLONE
            4 -> ClickType.THROW
            5 -> ClickType.QUICK_CRAFT
            6 -> ClickType.PICKUP_ALL
            else -> return ActionResult.fail("Invalid slot action mode: $mode")
        }

        val handler = Minecraft.getInstance().connection
            ?: return ActionResult.fail("Not connected to server")

        handler.send(
            ServerboundContainerClickPacket(
                windowId,
                stateId,
                slot,
                button,
                actionType,
                ItemStack.EMPTY,
                Int2ObjectOpenHashMap()
            )
        )
        return ActionResult.ok("Clicked slot $slot in window $windowId (mode=$actionType)")
    }
}
