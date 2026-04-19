package com.blackboxpro.fabric.action.debug

import com.blackboxpro.fabric.action.ActionExecutor
import com.blackboxpro.fabric.action.ActionResult
import com.blackboxpro.fabric.util.requireString
import com.google.gson.JsonObject
import net.minecraft.client.MinecraftClient
import net.minecraft.network.packet.c2s.play.DebugSampleSubscriptionC2SPacket
import net.minecraft.util.profiler.log.DebugSampleType

class DebugSampleAction : ActionExecutor {

    override fun execute(params: JsonObject): ActionResult {
        val typeStr = params.requireString("type").lowercase()
        if (typeStr != "tick_time" && typeStr != "time") {
            return ActionResult.fail("Unknown debug subscription type: $typeStr (valid: tick_time)")
        }

        val handler = MinecraftClient.getInstance().networkHandler
            ?: return ActionResult.fail("Not connected to server")

        handler.sendPacket(DebugSampleSubscriptionC2SPacket(DebugSampleType.TICK_TIME))
        return ActionResult.ok("Subscribed to debug sample: tick_time")
    }
}
