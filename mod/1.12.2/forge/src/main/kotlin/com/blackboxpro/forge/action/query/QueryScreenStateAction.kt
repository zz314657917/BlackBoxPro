package com.blackboxpro.forge.action.query

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.blackboxpro.forge.util.ContainerTooltipHelper
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiDisconnected
import net.minecraft.client.gui.GuiRepair
import net.minecraft.client.gui.GuiScreenBook
import net.minecraft.client.gui.inventory.*
import org.tabooproject.reflex.Reflex.Companion.getProperty

class QueryScreenStateAction : ActionExecutor {

    override fun execute(params: JsonObject): ActionResult {
        val mc = Minecraft.getMinecraft()
        val screen = mc.currentScreen

        val data = JsonObject().apply {
            addProperty("open", screen != null)
            addProperty("screenClass", screen?.javaClass?.simpleName ?: "none")

            if (screen is GuiContainer) {
                addProperty("isContainer", true)
                val container = mc.player?.openContainer
                if (container != null) {
                    addProperty("windowId", container.windowId)
                    addProperty("slotCount", container.inventorySlots.size)
                    val title = container.inventorySlots
                        .firstOrNull()?.inventory?.name ?: screen.javaClass.simpleName
                    addProperty("title", title)
                }
            } else {
                addProperty("isContainer", false)
                addProperty("title", screen?.javaClass?.simpleName ?: "")
            }

            addProperty("screenType", classifyScreen(screen))
            ContainerTooltipHelper.queryCurrentTooltip().applyToScreenState(this)

            if (screen is GuiDisconnected) {
                val fieldNames = listOf("reason", "field_96306_", "message", "cause")
                for (name in fieldNames) {
                    val found = runCatching {
                        val v = screen.getProperty<Any?>(name)
                        if (v != null) { addProperty("reason", v.toString()); true } else false
                    }.getOrNull() ?: false
                    if (found) break
                }
                if (!has("reason")) {
                    runCatching {
                        screen.javaClass.declaredFields.forEach { f ->
                            f.isAccessible = true
                            val v = f.get(screen)
                            if (v != null && v.javaClass.name.contains("TextComponent", ignoreCase = true)) {
                                addProperty("reason", v.toString())
                                return@forEach
                            }
                        }
                    }
                }
            }
        }

        return ActionResult.ok("Screen state queried", data)
    }

    private fun classifyScreen(screen: net.minecraft.client.gui.GuiScreen?): String = when (screen) {
        null -> "none"
        is GuiInventory -> "player_inventory"
        is GuiContainerCreative -> "creative_inventory"
        is GuiChest -> "generic_container"
        is GuiDispenser -> "generic_3x3"
        is GuiCrafting -> "crafting_table"
        is GuiFurnace -> "furnace"
        is GuiBrewingStand -> "brewing_stand"
        is GuiBeacon -> "beacon"
        is GuiScreenHorseInventory -> "horse"
        is GuiRepair -> "anvil"
        is net.minecraft.client.gui.GuiEnchantment -> "enchanting_table"
        is net.minecraft.client.gui.GuiMerchant -> "villager_trade"
        is net.minecraft.client.gui.inventory.GuiShulkerBox -> "shulker_box"
        is GuiScreenBook -> "book"
        is GuiDisconnected -> "disconnected"
        is GuiContainer -> "container_unknown"
        else -> "other"
    }
}
