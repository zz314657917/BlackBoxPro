package com.blackboxpro.fabric.action.query

import com.blackboxpro.fabric.action.ActionExecutor
import com.blackboxpro.fabric.action.ActionResult
import com.google.gson.JsonObject
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.ingame.*
import org.tabooproject.reflex.Reflex.Companion.getProperty

/**
 * 查询当前打开的屏幕/GUI 状态。
 * Action ID: "query_screen_state"
 */
class QueryScreenStateAction : ActionExecutor {

    override fun execute(params: JsonObject): ActionResult {
        val client = MinecraftClient.getInstance()
        val screen = client.currentScreen
        val disconnected = screen?.javaClass?.simpleName == "DisconnectedScreen"

        val data = JsonObject().apply {
            addProperty("open", screen != null)
            addProperty("screenClass", screen?.javaClass?.simpleName ?: "none")
            addProperty("title", screen?.title?.string ?: "")

            if (screen is HandledScreen<*>) {
                addProperty("isContainer", true)
                val handler = client.player?.currentScreenHandler
                if (handler != null) {
                    addProperty("windowId", handler.syncId)
                    addProperty("slotCount", handler.slots.size)
                }
            } else {
                addProperty("isContainer", false)
            }

            // 屏幕类型分类
            addProperty("screenType", if (disconnected) "disconnected" else classifyScreen(screen))

            // 断线屏幕：兼容不同命名和映射，尽量导出 reason/details
            if (disconnected) {
                readTextField(screen, listOf("reason", "f_96306_"))?.let { addProperty("reason", it) }
                readTextField(screen, listOf("details", "info", "f_96307_"))?.let { addProperty("details", it) }
            }
        }

        return ActionResult.ok("Screen state queried", data)
    }

    private fun classifyScreen(screen: net.minecraft.client.gui.screen.Screen?): String = when (screen) {
        null -> "none"
        is InventoryScreen -> "player_inventory"
        is CreativeInventoryScreen -> "creative_inventory"
        is GenericContainerScreen -> "generic_container"
        is Generic3x3ContainerScreen -> "generic_3x3"
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
        is HorseScreen -> "horse"
        is BookScreen -> "book"
        is BookEditScreen -> "book_edit"
        is HandledScreen<*> -> "container_unknown"
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

        // Fallback: 按类型搜索 Component 字段（Reflex 不支持按类型搜索，保留原始反射）
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
