package com.blackboxpro.plugin.api.action

import com.blackboxpro.plugin.api.BlackBoxApi
import com.blackboxpro.common.protocol.ResponseMessage
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import org.bukkit.entity.Player
import java.util.concurrent.CompletableFuture

/**
 * 查询行为快捷 API。
 */
object QueryActions {

    fun queryHeldItem(player: Player, hand: String = "main_hand"): CompletableFuture<ResponseMessage> =
        BlackBoxApi.sendAsync(player, "query_held_item", JsonObject().apply {
            addProperty("hand", hand)
        })

    fun queryInventorySlot(player: Player, slot: Int? = null): CompletableFuture<ResponseMessage> =
        BlackBoxApi.sendAsync(player, "query_inventory_slot", JsonObject().apply {
            if (slot != null) addProperty("slot", slot)
        })

    fun queryChatHistory(
        player: Player,
        count: Int = 10,
        filter: String? = null,
        since: Long? = null
    ): CompletableFuture<ResponseMessage> =
        BlackBoxApi.sendAsync(player, "query_chat_history", JsonObject().apply {
            addProperty("count", count)
            if (filter != null) addProperty("filter", filter)
            if (since != null) addProperty("since", since)
        })

    fun queryNearbyEntities(
        player: Player,
        radius: Double = 10.0,
        type: String? = null,
        limit: Int = 20
    ): CompletableFuture<ResponseMessage> =
        BlackBoxApi.sendAsync(player, "query_nearby_entities", JsonObject().apply {
            addProperty("radius", radius)
            if (type != null) addProperty("type", type)
            addProperty("limit", limit)
        })

    fun queryContainerState(player: Player): CompletableFuture<ResponseMessage> =
        BlackBoxApi.sendAsync(player, "query_container_state")

    fun queryPlayerState(player: Player): CompletableFuture<ResponseMessage> =
        BlackBoxApi.sendAsync(player, "query_player_state")

    fun queryContainerSlots(
        player: Player,
        windowId: Int = 0,
        slots: List<Int>? = null
    ): CompletableFuture<ResponseMessage> =
        BlackBoxApi.sendAsync(player, "query_container_slots", JsonObject().apply {
            addProperty("windowId", windowId)
            if (slots != null) add("slots", JsonArray().apply { slots.forEach { add(it) } })
        })

    fun queryActiveEffects(player: Player): CompletableFuture<ResponseMessage> =
        BlackBoxApi.sendAsync(player, "query_active_effects")

    // === v1.3.0 新增查询 ===

    fun queryBlockState(player: Player, x: Int, y: Int, z: Int): CompletableFuture<ResponseMessage> =
        BlackBoxApi.sendAsync(player, "query_block_state", JsonObject().apply {
            addProperty("x", x); addProperty("y", y); addProperty("z", z)
        })

    fun queryWorldState(player: Player): CompletableFuture<ResponseMessage> =
        BlackBoxApi.sendAsync(player, "query_world_state")

    fun queryTabList(player: Player, limit: Int = 100): CompletableFuture<ResponseMessage> =
        BlackBoxApi.sendAsync(player, "query_tab_list", JsonObject().apply {
            addProperty("limit", limit)
        })

    fun queryScoreboard(player: Player, objective: String? = null): CompletableFuture<ResponseMessage> =
        BlackBoxApi.sendAsync(player, "query_scoreboard", JsonObject().apply {
            if (objective != null) addProperty("objective", objective)
        })

    fun queryScreenState(player: Player): CompletableFuture<ResponseMessage> =
        BlackBoxApi.sendAsync(player, "query_screen_state")

    fun queryBossBar(player: Player): CompletableFuture<ResponseMessage> =
        BlackBoxApi.sendAsync(player, "query_boss_bar")

    fun queryTooltipState(player: Player): CompletableFuture<ResponseMessage> =
        BlackBoxApi.sendAsync(player, "query_tooltip_state")

    fun queryChatStyle(
        player: Player,
        match: String,
        index: Int = 0
    ): CompletableFuture<ResponseMessage> =
        BlackBoxApi.sendAsync(player, "query_chat_style", JsonObject().apply {
            addProperty("match", match)
            addProperty("index", index)
        })

    fun querySlotTooltip(
        player: Player,
        slot: Int,
        advanced: Boolean = false
    ): CompletableFuture<ResponseMessage> =
        BlackBoxApi.sendAsync(player, "query_slot_tooltip", JsonObject().apply {
            addProperty("slot", slot)
            addProperty("advanced", advanced)
        })
}
