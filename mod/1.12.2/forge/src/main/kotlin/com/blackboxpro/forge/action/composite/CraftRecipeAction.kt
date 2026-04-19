package com.blackboxpro.forge.action.composite

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.blackboxpro.forge.dispatcher.ActionRegistry
import com.blackboxpro.forge.util.getBooleanOrDefault
import com.blackboxpro.forge.util.requireInt
import com.blackboxpro.forge.util.requireString
import com.google.gson.JsonObject

class CraftRecipeAction : ActionExecutor {

    override fun execute(params: JsonObject): ActionResult {
        val windowId = params.requireInt("windowId")
        val recipeId = params.requireString("recipeId")
        val makeAll = params.getBooleanOrDefault("makeAll", false)

        val selectRecipe = ActionRegistry.find("select_recipe")
            ?: return ActionResult.fail("select_recipe action not registered")
        val selectResult = selectRecipe.execute(JsonObject().apply {
            addProperty("windowId", windowId)
            addProperty("recipeId", recipeId)
            addProperty("makeAll", makeAll)
        })
        if (!selectResult.success) return ActionResult.fail("Failed to select recipe: ${selectResult.message}")

        val seenRecipe = ActionRegistry.find("recipe_book_seen")
        seenRecipe?.execute(JsonObject().apply {
            addProperty("recipeId", recipeId)
        })

        return ActionResult.ok("Crafted recipe id=$recipeId in window $windowId (makeAll=$makeAll)")
    }
}
