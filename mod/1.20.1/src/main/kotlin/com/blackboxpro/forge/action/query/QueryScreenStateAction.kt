package com.blackboxpro.forge.action.query

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.inventory.*
import org.tabooproject.reflex.Reflex.Companion.getProperty

/**
 * 鏌ヨ褰撳墠鎵撳紑鐨勫睆骞?GUI 鐘舵€併€?
 * Action ID: "query_screen_state"
 */
class QueryScreenStateAction : ActionExecutor {

    override fun execute(params: JsonObject): ActionResult {
        val client = Minecraft.getInstance()
        val screen = client.screen
        val disconnected = screen?.javaClass?.simpleName == "DisconnectedScreen"

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

            addProperty("screenType", if (disconnected) "disconnected" else classifyScreen(screen))

            if (disconnected) {
                readTextField(screen, listOf("reason", "f_96306_"))?.let { addProperty("reason", it) }
                readTextField(screen, listOf("details", "info", "f_96307_"))?.let { addProperty("details", it) }
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

    private fun readTextField(target: Any?, fieldNames: List<String>): String? {
        val instance = target ?: return null
        for (fieldName in fieldNames) {
            val value = runCatching {
                instance.getProperty<Any?>(fieldName)?.toString()
            }.getOrNull()
            if (!value.isNullOrBlank()) return value
        }

        // Fallback: 鎸夌被鍨嬫悳绱?Component 瀛楁锛圧eflex 涓嶆敮鎸佹寜绫诲瀷鎼滅储锛屼繚鐣欏師濮嬪弽灏勶級
        return runCatching {
            instance.javaClass.declaredFields.firstNotNullOfOrNull { field ->
                field.isAccessible = true
                val value = field.get(instance)
                if (value != null && value.javaClass.name.contains("Component", ignoreCase = true)) {
                    value.toString()
                } else {
                    null
                }
            }
        }.getOrNull()
    }
}

