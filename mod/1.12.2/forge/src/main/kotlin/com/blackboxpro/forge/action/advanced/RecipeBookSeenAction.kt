package com.blackboxpro.forge.action.advanced

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.blackboxpro.forge.util.requireString
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.network.play.client.CPacketRecipeInfo
import net.minecraft.util.ResourceLocation
import net.minecraftforge.fml.common.registry.ForgeRegistries

class RecipeBookSeenAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val recipeId = params.requireString("recipeId")

        val connection = Minecraft.getMinecraft().connection
            ?: return ActionResult.fail("Not connected to server")

        val resourceLocation = ResourceLocation(recipeId)
        val recipe = ForgeRegistries.RECIPES.getValue(resourceLocation)
            ?: return ActionResult.fail("Recipe not found: $recipeId")

        connection.sendPacket(CPacketRecipeInfo(recipe))

        return ActionResult.ok("Marked recipe '$recipeId' as seen")
    }
}
