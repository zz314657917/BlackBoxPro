package com.blackboxpro.fabric.action.advanced

import com.blackboxpro.fabric.action.ActionExecutor
import com.blackboxpro.fabric.action.ActionResult
import com.blackboxpro.fabric.util.requireInt
import com.google.gson.JsonObject
import net.minecraft.client.MinecraftClient
import net.minecraft.network.packet.c2s.play.RecipeBookDataC2SPacket
import net.minecraft.recipe.RecipeEntry

class RecipeBookSeenAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val recipeIndex = params.requireInt("recipeIndex")
        val client = MinecraftClient.getInstance()
        val networkHandler = client.networkHandler
            ?: return ActionResult.fail("Not connected to server")
        val recipe = client.world?.recipeManager?.values()?.elementAtOrNull(recipeIndex)
            ?: return ActionResult.fail("Recipe not found at index: $recipeIndex")

        @Suppress("UNCHECKED_CAST")
        networkHandler.sendPacket(RecipeBookDataC2SPacket(recipe as RecipeEntry<*>))
        return ActionResult.ok("Marked recipe index=$recipeIndex as seen")
    }
}
