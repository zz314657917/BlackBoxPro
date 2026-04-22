package com.blackboxpro.forge.action.query

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.blackboxpro.forge.util.ItemStackSerializer
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.core.registries.BuiltInRegistries

/**
 * 鏌ヨ褰撳墠鎵撳紑鐨勫鍣ㄧ姸鎬併€?
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

