package com.blackboxpro.neoforge.action.debug

import com.blackboxpro.neoforge.action.ActionExecutor
import com.blackboxpro.neoforge.action.ActionResult
import com.blackboxpro.neoforge.util.getStringOrNull
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.network.protocol.game.ServerboundDebugSampleSubscriptionPacket
import net.minecraft.util.debugchart.RemoteDebugSampleType

class DebugSampleAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val typeStr = params.getStringOrNull("type") ?: "tick_time"
        val type = when (typeStr.lowercase()) {
            "tick_time", "tick" -> RemoteDebugSampleType.TICK_TIME
            else -> return ActionResult.fail("Unknown debug sample type: $typeStr (valid: tick_time)")
        }

        val handler = Minecraft.getInstance().connection
            ?: return ActionResult.fail("Not connected to server")

        handler.send(ServerboundDebugSampleSubscriptionPacket(type))
        return ActionResult.ok("Subscribed to debug sample: $typeStr")
    }
}
