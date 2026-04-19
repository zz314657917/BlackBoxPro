package com.blackboxpro.neoforge.action.query

import com.blackboxpro.neoforge.action.ActionExecutor
import com.blackboxpro.neoforge.action.ActionResult
import com.blackboxpro.neoforge.util.ItemStackSerializer
import com.blackboxpro.neoforge.util.getIntOrDefault
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft

/**
 * 批量读取容器槽位。
 * Action ID: "query_container_slots"
 */
class QueryContainerSlotsAction : ActionExecutor {

    override fun execute(params: JsonObject): ActionResult {
        val client = Minecraft.getInstance()
        val player = client.player
            ?: return ActionResult.fail("Player not available")

        val handler = player.containerMenu
        val windowId = params.getIntOrDefault("windowId", handler.containerId)

        val slotIndices = if (params.has("slots")) {
            params.getAsJsonArray("slots").map { it.asInt }
        } else {
            (0 until handler.slots.size).toList()
        }

        val slotsObj = JsonObject()
        for (idx in slotIndices) {
            if (idx < 0 || idx >= handler.slots.size) continue
            val stack = handler.slots[idx].item
            slotsObj.add(idx.toString(), ItemStackSerializer.serializeSlot(stack))
        }

        val data = JsonObject().apply {
            addProperty("windowId", windowId)
            add("slots", slotsObj)
        }

        return ActionResult.ok("Container slots queried", data)
    }
}
