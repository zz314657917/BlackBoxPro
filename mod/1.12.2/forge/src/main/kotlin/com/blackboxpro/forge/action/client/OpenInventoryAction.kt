package com.blackboxpro.forge.action.client

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.inventory.GuiInventory

class OpenInventoryAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val mc = Minecraft.getMinecraft()
        val player = mc.player ?: return ActionResult.fail("Player not available")
        mc.displayGuiScreen(GuiInventory(player))
        return ActionResult.ok("Inventory opened")
    }
}
