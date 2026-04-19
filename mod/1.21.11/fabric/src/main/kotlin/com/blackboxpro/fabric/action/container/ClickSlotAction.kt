package com.blackboxpro.fabric.action.container

import com.blackboxpro.fabric.action.ActionExecutor
import com.blackboxpro.fabric.action.ActionResult
import com.blackboxpro.fabric.util.requireInt
import com.google.gson.JsonObject
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import net.minecraft.client.MinecraftClient
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket
import net.minecraft.screen.slot.SlotActionType
import net.minecraft.screen.sync.ItemStackHash

class ClickSlotAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val windowId = params.requireInt("windowId")
        val stateId = params.requireInt("stateId")
        val slot = params.requireInt("slot")
        val button = params.requireInt("button")
        val mode = params.requireInt("mode")

        val actionType = when (mode) {
            0 -> SlotActionType.PICKUP
            1 -> SlotActionType.QUICK_MOVE
            2 -> SlotActionType.SWAP
            3 -> SlotActionType.CLONE
            4 -> SlotActionType.THROW
            5 -> SlotActionType.QUICK_CRAFT
            6 -> SlotActionType.PICKUP_ALL
            else -> return ActionResult.fail("Invalid slot action mode: $mode")
        }

        val handler = MinecraftClient.getInstance().networkHandler
            ?: return ActionResult.fail("Not connected to server")

        handler.sendPacket(
            ClickSlotC2SPacket(
                windowId,
                stateId,
                slot.toShort(),
                button.toByte(),
                actionType,
                Int2ObjectOpenHashMap(),
                ItemStackHash.EMPTY
            )
        )
        return ActionResult.ok("Clicked slot $slot in window $windowId (mode=$actionType)")
    }
}
