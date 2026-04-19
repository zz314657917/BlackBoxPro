package com.blackboxpro.plugin.api.action

import com.blackboxpro.plugin.api.BlackBoxApi
import com.blackboxpro.common.protocol.ResponseMessage
import com.google.gson.JsonObject
import org.bukkit.entity.Player
import java.util.concurrent.CompletableFuture

/**
 * 方块交互行为快捷 API。
 */
object BlockActions {

    fun digStart(player: Player, x: Int, y: Int, z: Int, face: String, sequence: Int = 0): CompletableFuture<ResponseMessage> =
        BlackBoxApi.sendAsync(player, "dig_start", JsonObject().apply {
            addProperty("x", x); addProperty("y", y); addProperty("z", z)
            addProperty("face", face); addProperty("sequence", sequence)
        })

    fun digCancel(player: Player, x: Int, y: Int, z: Int, face: String, sequence: Int = 0): CompletableFuture<ResponseMessage> =
        BlackBoxApi.sendAsync(player, "dig_cancel", JsonObject().apply {
            addProperty("x", x); addProperty("y", y); addProperty("z", z)
            addProperty("face", face); addProperty("sequence", sequence)
        })

    fun digFinish(player: Player, x: Int, y: Int, z: Int, face: String, sequence: Int = 0): CompletableFuture<ResponseMessage> =
        BlackBoxApi.sendAsync(player, "dig_finish", JsonObject().apply {
            addProperty("x", x); addProperty("y", y); addProperty("z", z)
            addProperty("face", face); addProperty("sequence", sequence)
        })

    fun placeBlock(
        player: Player, x: Int, y: Int, z: Int, face: String,
        hand: String = "main_hand",
        cursorX: Double = 0.5, cursorY: Double = 0.5, cursorZ: Double = 0.5,
        insideBlock: Boolean = false, sequence: Int = 0
    ): CompletableFuture<ResponseMessage> =
        BlackBoxApi.sendAsync(player, "place_block", JsonObject().apply {
            addProperty("x", x); addProperty("y", y); addProperty("z", z)
            addProperty("face", face); addProperty("hand", hand)
            addProperty("cursorX", cursorX); addProperty("cursorY", cursorY); addProperty("cursorZ", cursorZ)
            addProperty("insideBlock", insideBlock); addProperty("sequence", sequence)
        })

    fun useItem(player: Player, hand: String = "main_hand"): CompletableFuture<ResponseMessage> =
        BlackBoxApi.sendAsync(player, "use_item", JsonObject().apply {
            addProperty("hand", hand)
        })
}
