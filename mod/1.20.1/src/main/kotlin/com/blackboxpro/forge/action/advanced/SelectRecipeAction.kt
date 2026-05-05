package com.blackboxpro.forge.action.advanced

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.blackboxpro.forge.util.getBooleanOrDefault
import com.blackboxpro.forge.util.requireInt
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.network.protocol.game.ServerboundPlaceRecipePacket

class SelectRecipeAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val windowId = params.requireInt("windowId")
        val recipeIndex = params.requireInt("recipeIndex")
        val makeAll = params.getBooleanOrDefault("makeAll", false)

        val client = Minecraft.getInstance()
        val handler = client.connection
            ?: return ActionResult.fail("Not connected to server")
        val recipe = client.level?.recipeManager?.recipes?.elementAtOrNull(recipeIndex)
            ?: return ActionResult.fail("Recipe not found at index: $recipeIndex")

        handler.send(ServerboundPlaceRecipePacket(windowId, recipe, makeAll))
        return ActionResult.ok("Selected recipe index=$recipeIndex in window $windowId (makeAll=$makeAll)")
    }
}

