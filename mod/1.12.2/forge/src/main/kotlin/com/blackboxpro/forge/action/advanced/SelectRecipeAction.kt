package com.blackboxpro.forge.action.advanced

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.blackboxpro.forge.util.getBooleanOrDefault
import com.blackboxpro.forge.util.requireInt
import com.blackboxpro.forge.util.requireString
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.network.play.client.CPacketPlaceRecipe
import net.minecraft.util.ResourceLocation
import net.minecraftforge.fml.common.registry.ForgeRegistries

class SelectRecipeAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val windowId = params.requireInt("windowId")
        val recipeId = params.requireString("recipeId")
        val makeAll = params.getBooleanOrDefault("makeAll", false)

        val connection = Minecraft.getMinecraft().connection
            ?: return ActionResult.fail("Not connected to server")

        val resourceLocation = ResourceLocation(recipeId)
        val recipe = ForgeRegistries.RECIPES.getValue(resourceLocation)
            ?: return ActionResult.fail("Recipe not found: $recipeId")

        connection.sendPacket(CPacketPlaceRecipe(windowId, recipe, makeAll))

        return ActionResult.ok("Selected recipe '$recipeId' in window $windowId (makeAll=$makeAll)")
    }
}
