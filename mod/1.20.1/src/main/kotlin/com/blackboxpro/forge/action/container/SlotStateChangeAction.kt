package com.blackboxpro.forge.action.container

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.blackboxpro.forge.util.requireBoolean
import com.blackboxpro.forge.util.requireInt
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft

class SlotStateChangeAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val windowId = params.requireInt("windowId")
        val slotId = params.requireInt("slotId")
        val state = params.requireBoolean("state")

        Minecraft.getInstance().connection
            ?: return ActionResult.fail("Not connected to server")

        return ActionResult.ok(
            "Slot state change has no dedicated 1.20.1 protocol packet; treated as acknowledged (window=$windowId, slot=$slotId, state=$state)"
        )
    }
}
