package com.blackboxpro.plugin.api.action

import com.blackboxpro.plugin.api.BlackBoxApi
import com.blackboxpro.common.protocol.ResponseMessage
import com.google.gson.JsonObject
import org.bukkit.entity.Player
import java.util.concurrent.CompletableFuture

/**
 * 容器/GUI 操作行为快捷 API。
 */
object ContainerActions {

    fun clickSlot(player: Player, windowId: Int, stateId: Int, slot: Int, button: Int, mode: Int): CompletableFuture<ResponseMessage> =
        BlackBoxApi.sendAsync(player, "click_slot", JsonObject().apply {
            addProperty("windowId", windowId); addProperty("stateId", stateId)
            addProperty("slot", slot); addProperty("button", button); addProperty("mode", mode)
        })

    fun clickButton(player: Player, windowId: Int, buttonId: Int): CompletableFuture<ResponseMessage> =
        BlackBoxApi.sendAsync(player, "click_button", JsonObject().apply {
            addProperty("windowId", windowId); addProperty("buttonId", buttonId)
        })

    fun closeContainer(player: Player, windowId: Int): CompletableFuture<ResponseMessage> =
        BlackBoxApi.sendAsync(player, "close_container", JsonObject().apply {
            addProperty("windowId", windowId)
        })

    /** 关闭当前打开的容器（客户端自动获取 windowId） */
    fun closeContainer(player: Player): CompletableFuture<ResponseMessage> =
        BlackBoxApi.sendAsync(player, "close_container")

    fun setCarriedItem(player: Player, slot: Int): CompletableFuture<ResponseMessage> =
        BlackBoxApi.sendAsync(player, "set_carried_item", JsonObject().apply {
            addProperty("slot", slot)
        })

    fun pickItem(player: Player, x: Int, y: Int, z: Int, includeData: Boolean = false): CompletableFuture<ResponseMessage> =
        BlackBoxApi.sendAsync(player, "pick_item", JsonObject().apply {
            addProperty("x", x); addProperty("y", y); addProperty("z", z)
            addProperty("includeData", includeData)
        })

    fun pickEntity(player: Player, entityId: Int, includeData: Boolean = false): CompletableFuture<ResponseMessage> =
        BlackBoxApi.sendAsync(player, "pick_entity", JsonObject().apply {
            addProperty("entityId", entityId); addProperty("includeData", includeData)
        })

    fun slotStateChange(player: Player, windowId: Int, slotId: Int, state: Boolean): CompletableFuture<ResponseMessage> =
        BlackBoxApi.sendAsync(player, "slot_state_change", JsonObject().apply {
            addProperty("windowId", windowId); addProperty("slotId", slotId); addProperty("state", state)
        })

    fun creativeSetSlot(player: Player, slot: Int): CompletableFuture<ResponseMessage> =
        BlackBoxApi.sendAsync(player, "creative_set_slot", JsonObject().apply {
            addProperty("slot", slot)
        })

    fun pickItemFromBlock(player: Player, x: Int, y: Int, z: Int, includeData: Boolean = false): CompletableFuture<ResponseMessage> =
        BlackBoxApi.sendAsync(player, "pick_item_from_block", JsonObject().apply {
            addProperty("x", x); addProperty("y", y); addProperty("z", z)
            addProperty("includeData", includeData)
        })

    fun pickItemFromEntity(player: Player, entityId: Int, includeData: Boolean = false): CompletableFuture<ResponseMessage> =
        BlackBoxApi.sendAsync(player, "pick_item_from_entity", JsonObject().apply {
            addProperty("entityId", entityId); addProperty("includeData", includeData)
        })

    fun bundleSelectedSlot(player: Player, slotId: Int, selectedIndex: Int): CompletableFuture<ResponseMessage> =
        BlackBoxApi.sendAsync(player, "bundle_selected_slot", JsonObject().apply {
            addProperty("slotId", slotId); addProperty("selectedIndex", selectedIndex)
        })
}
