package com.blackboxpro.neoforge.action.advanced

import com.blackboxpro.neoforge.action.ActionExecutor
import com.blackboxpro.neoforge.action.ActionResult
import com.blackboxpro.neoforge.util.requireBoolean
import com.blackboxpro.neoforge.util.requireString
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.network.protocol.game.ServerboundRecipeBookChangeSettingsPacket
import net.minecraft.world.inventory.RecipeBookType

class RecipeBookToggleAction : ActionExecutor {

    companion object {
        private val CATEGORY_MAP = mapOf(
            "crafting" to RecipeBookType.CRAFTING,
            "furnace" to RecipeBookType.FURNACE,
            "blast_furnace" to RecipeBookType.BLAST_FURNACE,
            "smoker" to RecipeBookType.SMOKER
        )
    }

    override fun execute(params: JsonObject): ActionResult {
        val categoryStr = params.requireString("category")
        val open = params.requireBoolean("open")
        val filtering = params.requireBoolean("filtering")

        val category = CATEGORY_MAP[categoryStr.lowercase()]
            ?: return ActionResult.fail("Unknown recipe category: $categoryStr (valid: ${CATEGORY_MAP.keys})")

        val handler = Minecraft.getInstance().connection
            ?: return ActionResult.fail("Not connected to server")

        handler.send(ServerboundRecipeBookChangeSettingsPacket(category, open, filtering))
        return ActionResult.ok("Toggled recipe book: category=$categoryStr, open=$open, filtering=$filtering")
    }
}
