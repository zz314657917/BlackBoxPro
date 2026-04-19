package com.blackboxpro.neoforge.action.query

import com.blackboxpro.neoforge.action.ActionExecutor
import com.blackboxpro.neoforge.action.ActionResult
import com.blackboxpro.neoforge.util.ItemStackSerializer
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.core.registries.BuiltInRegistries

/**
 * 查询当前打开的容器状态。
 * Action ID: "query_container_state"
 */
class QueryContainerStateAction : ActionExecutor {

    override fun execute(params: JsonObject): ActionResult {
        val client = Minecraft.getInstance()
        val player = client.player
            ?: return ActionResult.fail("Player not available")

        val handler = player.containerMenu
        val isInventoryOnly = handler === player.inventoryMenu

        val data = JsonObject().apply {
            addProperty("open", !isInventoryOnly)
            addProperty("windowId", handler.containerId)
            addProperty("stateId", handler.stateId)
            addProperty("type", try { handler.type?.let { BuiltInRegistries.MENU.getKey(it)?.toString() } ?: "unknown" } catch (_: Throwable) { "unknown" })
            addProperty("slotCount", handler.slots.size)

            val screen = client.screen
            if (screen is AbstractContainerScreen<*>) {
                addProperty("title", screen.title.string)
            }

            add("carriedItem", ItemStackSerializer.serializeSlot(handler.carried))
        }

        return ActionResult.ok("Container state queried", data)
    }
}
