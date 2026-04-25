package com.blackboxpro.plugin.api.action

import com.blackboxpro.common.protocol.ResponseMessage
import com.blackboxpro.plugin.api.BlackBoxApi
import com.google.gson.JsonObject
import org.bukkit.entity.Player
import java.util.concurrent.CompletableFuture

object MouseActions {

    fun moveMouse(
        player: Player,
        x: Double,
        y: Double
    ): CompletableFuture<ResponseMessage> =
        BlackBoxApi.sendAsync(player, "move_mouse", JsonObject().apply {
            addProperty("x", x)
            addProperty("y", y)
        })

    fun clickMouse(
        player: Player,
        button: Int = 0,
        clickCount: Int = 1
    ): CompletableFuture<ResponseMessage> =
        BlackBoxApi.sendAsync(player, "click_mouse", JsonObject().apply {
            addProperty("button", button)
            addProperty("clickCount", clickCount)
        })

    fun clickScreenAt(
        player: Player,
        x: Double,
        y: Double,
        button: Int = 0,
        clickCount: Int = 1
    ): CompletableFuture<ResponseMessage> =
        BlackBoxApi.sendAsync(player, "click_screen_at", JsonObject().apply {
            addProperty("x", x)
            addProperty("y", y)
            addProperty("button", button)
            addProperty("clickCount", clickCount)
        })

    fun clickChatText(
        player: Player,
        match: String,
        index: Int = 0,
        execute: Boolean = true
    ): CompletableFuture<ResponseMessage> =
        BlackBoxApi.sendAsync(player, "click_chat_text", JsonObject().apply {
            addProperty("match", match)
            addProperty("index", index)
            addProperty("execute", execute)
        })

    fun queryChatStyle(
        player: Player,
        match: String,
        index: Int = 0
    ): CompletableFuture<ResponseMessage> =
        QueryActions.queryChatStyle(player, match, index)

    fun hoverSlot(
        player: Player,
        windowId: Int,
        slot: Int,
        durationTicks: Int = 0
    ): CompletableFuture<ResponseMessage> =
        BlackBoxApi.sendAsync(player, "hover_slot", JsonObject().apply {
            addProperty("windowId", windowId)
            addProperty("slot", slot)
            addProperty("durationTicks", durationTicks)
        })

    fun querySlotTooltip(
        player: Player,
        slot: Int,
        advanced: Boolean = false
    ): CompletableFuture<ResponseMessage> =
        QueryActions.querySlotTooltip(player, slot, advanced)

    fun queryCursorState(player: Player): CompletableFuture<ResponseMessage> =
        QueryActions.queryCursorState(player)

    fun queryTooltipState(player: Player): CompletableFuture<ResponseMessage> =
        QueryActions.queryTooltipState(player)
}
