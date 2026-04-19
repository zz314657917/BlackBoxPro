package com.blackboxpro.fabric.action.advanced

import com.blackboxpro.fabric.action.ActionExecutor
import com.blackboxpro.fabric.action.ActionResult
import com.blackboxpro.fabric.util.requireBoolean
import com.blackboxpro.fabric.util.requireString
import com.google.gson.JsonObject
import net.minecraft.client.MinecraftClient
import net.minecraft.network.packet.c2s.play.RecipeCategoryOptionsC2SPacket
import net.minecraft.recipe.book.RecipeBookCategory

class RecipeBookToggleAction : ActionExecutor {

    companion object {
        private val CATEGORY_MAP = mapOf(
            "crafting" to RecipeBookCategory.CRAFTING,
            "furnace" to RecipeBookCategory.FURNACE,
            "blast_furnace" to RecipeBookCategory.BLAST_FURNACE,
            "smoker" to RecipeBookCategory.SMOKER
        )
    }

    override fun execute(params: JsonObject): ActionResult {
        val categoryStr = params.requireString("category")
        val open = params.requireBoolean("open")
        val filtering = params.requireBoolean("filtering")

        val category = CATEGORY_MAP[categoryStr.lowercase()]
            ?: return ActionResult.fail("Unknown recipe category: $categoryStr (valid: ${CATEGORY_MAP.keys})")

        val handler = MinecraftClient.getInstance().networkHandler
            ?: return ActionResult.fail("Not connected to server")

        handler.sendPacket(RecipeCategoryOptionsC2SPacket(category, open, filtering))
        return ActionResult.ok("Toggled recipe book: category=$categoryStr, open=$open, filtering=$filtering")
    }
}
