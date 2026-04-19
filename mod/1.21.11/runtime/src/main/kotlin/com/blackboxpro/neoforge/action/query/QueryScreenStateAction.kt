package com.blackboxpro.neoforge.action.query

import com.blackboxpro.neoforge.action.ActionExecutor
import com.blackboxpro.neoforge.action.ActionResult
import com.blackboxpro.neoforge.util.ContainerTooltipHelper
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.inventory.*
import org.tabooproject.reflex.Reflex.Companion.getProperty

/**
 * 查询当前打开的屏幕/GUI 状态。
 * Action ID: "query_screen_state"
 */
class QueryScreenStateAction : ActionExecutor {

    override fun execute(params: JsonObject): ActionResult {
        val client = Minecraft.getInstance()
        val screen = client.screen

        val data = JsonObject().apply {
            addProperty("open", screen != null)
            addProperty("screenClass", screen?.javaClass?.simpleName ?: "none")
            addProperty("title", screen?.title?.string ?: "")

            if (screen is AbstractContainerScreen<*>) {
                addProperty("isContainer", true)
                val handler = client.player?.containerMenu
                if (handler != null) {
                    addProperty("windowId", handler.containerId)
                    addProperty("slotCount", handler.slots.size)
                }
            } else {
                addProperty("isContainer", false)
            }

            val screenType = if (screen?.javaClass?.simpleName == "DisconnectedScreen") "disconnected"
                             else classifyScreen(screen)
            addProperty("screenType", screenType)

            val tooltipState = ContainerTooltipHelper.queryCurrentTooltip()
            tooltipState.applyToScreenState(this)

            if (screen != null && screen.javaClass.simpleName == "DisconnectedScreen") {
                val reasonNames = listOf("reason", "f_96306_")
                for (name in reasonNames) {
                    val v = runCatching { screen.getProperty<Any?>(name) }.getOrNull()
                    if (v != null) { addProperty("reason", v.toString()); break }
                }
                val detailNames = listOf("details", "info", "f_96307_")
                for (name in detailNames) {
                    val v = runCatching { screen.getProperty<Any?>(name) }.getOrNull()
                    if (v != null) { addProperty("details", v.toString()); break }
                }
            }
        }

        return ActionResult.ok("Screen state queried", data)
    }

    private fun classifyScreen(screen: net.minecraft.client.gui.screens.Screen?): String = when (screen) {
        null -> "none"
        is InventoryScreen -> "player_inventory"
        is CreativeModeInventoryScreen -> "creative_inventory"
        is ContainerScreen -> "generic_container"
        is DispenserScreen -> "generic_3x3"
        is ShulkerBoxScreen -> "shulker_box"
        is CraftingScreen -> "crafting_table"
        is FurnaceScreen -> "furnace"
        is SmokerScreen -> "smoker"
        is BlastFurnaceScreen -> "blast_furnace"
        is BrewingStandScreen -> "brewing_stand"
        is AnvilScreen -> "anvil"
        is EnchantmentScreen -> "enchanting_table"
        is GrindstoneScreen -> "grindstone"
        is LoomScreen -> "loom"
        is CartographyTableScreen -> "cartography_table"
        is StonecutterScreen -> "stonecutter"
        is SmithingScreen -> "smithing_table"
        is MerchantScreen -> "villager_trade"
        is HopperScreen -> "hopper"
        is BeaconScreen -> "beacon"
        is HorseInventoryScreen -> "horse"
        is BookViewScreen -> "book"
        is BookEditScreen -> "book_edit"
        is AbstractContainerScreen<*> -> "container_unknown"
        else -> "other"
    }
}
