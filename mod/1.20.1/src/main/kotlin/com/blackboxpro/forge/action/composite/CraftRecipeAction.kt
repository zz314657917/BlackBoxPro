package com.blackboxpro.forge.action.composite

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.blackboxpro.forge.dispatcher.ActionRegistry
import com.blackboxpro.forge.util.getBooleanOrDefault
import com.blackboxpro.forge.util.requireInt
import com.google.gson.JsonObject

class CraftRecipeAction : ActionExecutor {

    override fun execute(params: JsonObject): ActionResult {
        val windowId = params.requireInt("windowId")
        val recipeIndex = params.requireInt("recipeIndex")
        val makeAll = params.getBooleanOrDefault("makeAll", false)

        // 1. 閫夋嫨閰嶆柟
        val selectRecipe = ActionRegistry.find("select_recipe")
            ?: return ActionResult.fail("select_recipe action not registered")
        val selectResult = selectRecipe.execute(JsonObject().apply {
            addProperty("windowId", windowId)
            addProperty("recipeIndex", recipeIndex)
            addProperty("makeAll", makeAll)
        })
        if (!selectResult.success) return ActionResult.fail("Failed to select recipe: ${selectResult.message}")

        // 2. 鏍囪閰嶆柟涓哄凡鏌ョ湅
        val seenRecipe = ActionRegistry.find("recipe_book_seen")
        seenRecipe?.execute(JsonObject().apply {
            addProperty("recipeIndex", recipeIndex)
        })

        return ActionResult.ok("Crafted recipe index=$recipeIndex in window $windowId (makeAll=$makeAll)")
    }
}

