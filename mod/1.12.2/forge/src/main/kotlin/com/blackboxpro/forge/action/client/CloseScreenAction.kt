package com.blackboxpro.forge.action.client

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft

class CloseScreenAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val mc = Minecraft.getMinecraft()
        if (mc.currentScreen != null) {
            mc.displayGuiScreen(null)
        }
        return ActionResult.ok("Screen closed")
    }
}
