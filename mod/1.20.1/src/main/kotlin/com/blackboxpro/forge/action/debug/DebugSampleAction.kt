package com.blackboxpro.forge.action.debug

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.blackboxpro.forge.util.requireString
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft

class DebugSampleAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val type = params.requireString("type")

        Minecraft.getInstance().connection
            ?: return ActionResult.fail("Not connected to server")

        return ActionResult.ok(
            "Debug sample subscription has no dedicated Forge 1.20.1 protocol packet; treated as acknowledged (type=$type)"
        )
    }
}
