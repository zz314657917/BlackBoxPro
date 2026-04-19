package com.blackboxpro.forge.action.query

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.blackboxpro.forge.util.ItemStackSerializer
import com.blackboxpro.forge.util.getIntOrDefault
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft

class QueryContainerSlotsAction : ActionExecutor {

    override fun execute(params: JsonObject): ActionResult {
        val mc = Minecraft.getMinecraft()
        val player = mc.player
            ?: return ActionResult.fail("Player not available")

        val container = player.openContainer
        val windowId = params.getIntOrDefault("windowId", container.windowId)

        val slotIndices = if (params.has("slots")) {
            params.getAsJsonArray("slots").map { it.asInt }
        } else {
            (0 until container.inventorySlots.size).toList()
        }

        val slotsObj = JsonObject()
        for (idx in slotIndices) {
            if (idx < 0 || idx >= container.inventorySlots.size) continue
            val stack = container.inventorySlots[idx].stack
            slotsObj.add(idx.toString(), ItemStackSerializer.serializeSlot(stack))
        }

        val data = JsonObject().apply {
            addProperty("windowId", windowId)
            add("slots", slotsObj)
        }

        return ActionResult.ok("Container slots queried", data)
    }
}
