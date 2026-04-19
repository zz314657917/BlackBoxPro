package com.blackboxpro.plugin.api.action

import com.blackboxpro.plugin.api.BlackBoxApi
import com.blackboxpro.common.protocol.ResponseMessage
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import org.bukkit.entity.Player
import java.util.concurrent.CompletableFuture

/**
 * 进阶交互行为快捷 API。
 */
object AdvancedActions {

    fun editBook(player: Player, slot: Int, pages: List<String>): CompletableFuture<ResponseMessage> =
        BlackBoxApi.sendAsync(player, "edit_book", JsonObject().apply {
            addProperty("slot", slot)
            add("pages", JsonArray().apply { pages.forEach { add(it) } })
        })

    fun signBook(player: Player, slot: Int, pages: List<String>, title: String): CompletableFuture<ResponseMessage> =
        BlackBoxApi.sendAsync(player, "sign_book", JsonObject().apply {
            addProperty("slot", slot); addProperty("title", title)
            add("pages", JsonArray().apply { pages.forEach { add(it) } })
        })

    fun updateSign(player: Player, x: Int, y: Int, z: Int, lines: List<String>, isFrontText: Boolean = true): CompletableFuture<ResponseMessage> =
        BlackBoxApi.sendAsync(player, "update_sign", JsonObject().apply {
            addProperty("x", x); addProperty("y", y); addProperty("z", z)
            addProperty("isFrontText", isFrontText)
            add("lines", JsonArray().apply { lines.forEach { add(it) } })
        })

    fun updateCommandBlock(player: Player, x: Int, y: Int, z: Int, command: String, mode: Int = 2, trackOutput: Boolean = true, conditional: Boolean = false, alwaysActive: Boolean = false): CompletableFuture<ResponseMessage> =
        BlackBoxApi.sendAsync(player, "update_command_block", JsonObject().apply {
            addProperty("x", x); addProperty("y", y); addProperty("z", z)
            addProperty("command", command); addProperty("mode", mode)
            addProperty("trackOutput", trackOutput); addProperty("conditional", conditional)
            addProperty("alwaysActive", alwaysActive)
        })

    fun selectRecipe(player: Player, windowId: Int, recipeIndex: Int, makeAll: Boolean = false): CompletableFuture<ResponseMessage> =
        BlackBoxApi.sendAsync(player, "select_recipe", JsonObject().apply {
            addProperty("windowId", windowId); addProperty("recipeIndex", recipeIndex); addProperty("makeAll", makeAll)
        })

    fun queryEntityNbt(player: Player, transactionId: Int, entityId: Int): CompletableFuture<ResponseMessage> =
        BlackBoxApi.sendAsync(player, "query_entity_nbt", JsonObject().apply {
            addProperty("transactionId", transactionId); addProperty("entityId", entityId)
        })

    fun queryBlockNbt(player: Player, transactionId: Int, x: Int, y: Int, z: Int): CompletableFuture<ResponseMessage> =
        BlackBoxApi.sendAsync(player, "query_block_nbt", JsonObject().apply {
            addProperty("transactionId", transactionId)
            addProperty("x", x); addProperty("y", y); addProperty("z", z)
        })

    fun setBeaconEffect(player: Player, primaryEffect: Int = -1, secondaryEffect: Int = -1): CompletableFuture<ResponseMessage> =
        BlackBoxApi.sendAsync(player, "set_beacon_effect", JsonObject().apply {
            addProperty("primaryEffect", primaryEffect); addProperty("secondaryEffect", secondaryEffect)
        })

    fun renameItem(player: Player, name: String): CompletableFuture<ResponseMessage> =
        BlackBoxApi.sendAsync(player, "rename_item", JsonObject().apply {
            addProperty("name", name)
        })

    fun selectTrade(player: Player, selectedSlot: Int): CompletableFuture<ResponseMessage> =
        BlackBoxApi.sendAsync(player, "select_trade", JsonObject().apply {
            addProperty("selectedSlot", selectedSlot)
        })

    fun lockDifficulty(player: Player, locked: Boolean): CompletableFuture<ResponseMessage> =
        BlackBoxApi.sendAsync(player, "lock_difficulty", JsonObject().apply {
            addProperty("locked", locked)
        })

    fun updateCommandBlockMinecart(player: Player, entityId: Int, command: String, trackOutput: Boolean = true): CompletableFuture<ResponseMessage> =
        BlackBoxApi.sendAsync(player, "update_command_block_minecart", JsonObject().apply {
            addProperty("entityId", entityId); addProperty("command", command)
            addProperty("trackOutput", trackOutput)
        })

    fun updateStructureBlock(
        player: Player, x: Int, y: Int, z: Int,
        action: Int, mode: String, name: String,
        offsetX: Int = 0, offsetY: Int = 0, offsetZ: Int = 0,
        sizeX: Int = 0, sizeY: Int = 0, sizeZ: Int = 0,
        mirror: String = "none", rotation: String = "none",
        metadata: String = "", integrity: Float = 1.0f, seed: Long = 0L, flags: Int = 0
    ): CompletableFuture<ResponseMessage> =
        BlackBoxApi.sendAsync(player, "update_structure_block", JsonObject().apply {
            addProperty("x", x); addProperty("y", y); addProperty("z", z)
            addProperty("action", action); addProperty("mode", mode); addProperty("name", name)
            addProperty("offsetX", offsetX); addProperty("offsetY", offsetY); addProperty("offsetZ", offsetZ)
            addProperty("sizeX", sizeX); addProperty("sizeY", sizeY); addProperty("sizeZ", sizeZ)
            addProperty("mirror", mirror); addProperty("rotation", rotation)
            addProperty("metadata", metadata); addProperty("integrity", integrity)
            addProperty("seed", seed); addProperty("flags", flags)
        })

    fun updateJigsawBlock(
        player: Player, x: Int, y: Int, z: Int,
        name: String, target: String, pool: String,
        finalState: String = "", jointType: String = "rollable",
        selectionPriority: Int = 0, placementPriority: Int = 0
    ): CompletableFuture<ResponseMessage> =
        BlackBoxApi.sendAsync(player, "update_jigsaw_block", JsonObject().apply {
            addProperty("x", x); addProperty("y", y); addProperty("z", z)
            addProperty("name", name); addProperty("target", target); addProperty("pool", pool)
            addProperty("finalState", finalState); addProperty("jointType", jointType)
            addProperty("selectionPriority", selectionPriority); addProperty("placementPriority", placementPriority)
        })

    fun recipeBookToggle(player: Player, category: String, open: Boolean, filtering: Boolean): CompletableFuture<ResponseMessage> =
        BlackBoxApi.sendAsync(player, "recipe_book_toggle", JsonObject().apply {
            addProperty("category", category); addProperty("open", open); addProperty("filtering", filtering)
        })

    fun recipeBookSeen(player: Player, recipeIndex: Int): CompletableFuture<ResponseMessage> =
        BlackBoxApi.sendAsync(player, "recipe_book_seen", JsonObject().apply {
            addProperty("recipeIndex", recipeIndex)
        })

    fun advancementTab(player: Player, action: String, tabId: String? = null): CompletableFuture<ResponseMessage> =
        BlackBoxApi.sendAsync(player, "advancement_tab", JsonObject().apply {
            addProperty("action", action)
            tabId?.let { addProperty("tabId", it) }
        })
}
