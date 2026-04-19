package com.blackboxpro.plugin.api.action

import com.blackboxpro.common.protocol.ResponseMessage
import com.blackboxpro.plugin.api.BlackBoxApi
import com.google.gson.JsonObject
import org.bukkit.entity.Player
import java.util.concurrent.CompletableFuture

object MouseActions {

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

    fun queryTooltipState(player: Player): CompletableFuture<ResponseMessage> =
        QueryActions.queryTooltipState(player)
}
