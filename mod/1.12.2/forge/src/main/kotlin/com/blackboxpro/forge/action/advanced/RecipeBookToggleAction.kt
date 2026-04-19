package com.blackboxpro.forge.action.advanced

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.blackboxpro.forge.util.getBooleanOrDefault
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.network.play.client.CPacketRecipeInfo

class RecipeBookToggleAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val open = params.getBooleanOrDefault("open", true)
        val filtering = params.getBooleanOrDefault("filtering", false)

        val connection = Minecraft.getMinecraft().connection
            ?: return ActionResult.fail("Not connected to server")

        connection.sendPacket(CPacketRecipeInfo(open, filtering))

        return ActionResult.ok("Toggled recipe book: open=$open, filtering=$filtering")
    }
}
