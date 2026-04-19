package com.blackboxpro.neoforge.action.advanced

import com.blackboxpro.neoforge.action.ActionExecutor
import com.blackboxpro.neoforge.action.ActionResult
import com.blackboxpro.neoforge.util.requireInt
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.network.protocol.game.ServerboundRecipeBookSeenRecipePacket
import net.minecraft.world.item.crafting.display.RecipeDisplayId

class RecipeBookSeenAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val recipeIndex = params.requireInt("recipeIndex")

        val networkHandler = Minecraft.getInstance().connection
            ?: return ActionResult.fail("Not connected to server")

        networkHandler.send(ServerboundRecipeBookSeenRecipePacket(RecipeDisplayId(recipeIndex)))
        return ActionResult.ok("Marked recipe index=$recipeIndex as seen")
    }
}
