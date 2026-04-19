package com.blackboxpro.neoforge.action.advanced

import com.blackboxpro.neoforge.action.ActionExecutor
import com.blackboxpro.neoforge.action.ActionResult
import com.blackboxpro.neoforge.util.getBooleanOrDefault
import com.blackboxpro.neoforge.util.requireInt
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.network.protocol.game.ServerboundPlaceRecipePacket
import net.minecraft.world.item.crafting.display.RecipeDisplayId

class SelectRecipeAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val windowId = params.requireInt("windowId")
        val recipeIndex = params.requireInt("recipeIndex")
        val makeAll = params.getBooleanOrDefault("makeAll", false)

        val networkHandler = Minecraft.getInstance().connection
            ?: return ActionResult.fail("Not connected to server")

        networkHandler.send(
            ServerboundPlaceRecipePacket(windowId, RecipeDisplayId(recipeIndex), makeAll)
        )
        return ActionResult.ok("Selected recipe index=$recipeIndex in window $windowId (makeAll=$makeAll)")
    }
}
