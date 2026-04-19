package com.blackboxpro.neoforge.action.client

import com.blackboxpro.neoforge.action.ActionExecutor
import com.blackboxpro.neoforge.action.ActionResult
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.inventory.InventoryScreen

class OpenInventoryAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val client = Minecraft.getInstance()
        val player = client.player ?: return ActionResult.fail("Player not available")
        client.setScreen(InventoryScreen(player))
        return ActionResult.ok("Inventory opened")
    }
}
