package com.blackboxpro.plugin.api.action

import com.blackboxpro.plugin.api.BlackBoxApi
import com.blackboxpro.common.protocol.ResponseMessage
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import org.bukkit.entity.Player
import java.util.concurrent.CompletableFuture

/**
 * 复合行为（宏指令）快捷 API。
 */
object CompositeActions {

    fun lookAt(player: Player, x: Double, y: Double, z: Double): CompletableFuture<ResponseMessage> =
        BlackBoxApi.sendAsync(player, "look_at", JsonObject().apply {
            addProperty("x", x); addProperty("y", y); addProperty("z", z)
        })

    fun lookAtEntity(player: Player, entityId: Int): CompletableFuture<ResponseMessage> =
        BlackBoxApi.sendAsync(player, "look_at_entity", JsonObject().apply {
            addProperty("entityId", entityId)
        })

    fun breakBlock(player: Player, x: Int, y: Int, z: Int): CompletableFuture<ResponseMessage> =
        BlackBoxApi.sendAsync(player, "break_block", JsonObject().apply {
            addProperty("x", x); addProperty("y", y); addProperty("z", z)
        })

    fun placeBlockAt(player: Player, x: Int, y: Int, z: Int, face: String = "top", hand: String = "main_hand"): CompletableFuture<ResponseMessage> =
        BlackBoxApi.sendAsync(player, "place_block_at", JsonObject().apply {
            addProperty("x", x); addProperty("y", y); addProperty("z", z)
            addProperty("face", face); addProperty("hand", hand)
        })

    fun attack(player: Player, entityId: Int): CompletableFuture<ResponseMessage> =
        BlackBoxApi.sendAsync(player, "attack", JsonObject().apply {
            addProperty("entityId", entityId)
        })

    fun use(player: Player, hand: String = "main_hand"): CompletableFuture<ResponseMessage> =
        BlackBoxApi.sendAsync(player, "use", JsonObject().apply {
            addProperty("hand", hand)
        })

    fun openContainer(player: Player, x: Int, y: Int, z: Int): CompletableFuture<ResponseMessage> =
        BlackBoxApi.sendAsync(player, "open_container", JsonObject().apply {
            addProperty("x", x); addProperty("y", y); addProperty("z", z)
        })

    fun containerTransfer(player: Player, windowId: Int, stateId: Int, slot: Int): CompletableFuture<ResponseMessage> =
        BlackBoxApi.sendAsync(player, "container_transfer", JsonObject().apply {
            addProperty("windowId", windowId); addProperty("stateId", stateId); addProperty("slot", slot)
        })

    fun dropInventory(player: Player, slot: Int): CompletableFuture<ResponseMessage> =
        BlackBoxApi.sendAsync(player, "drop_inventory", JsonObject().apply {
            addProperty("slot", slot)
        })

    fun pathfindTo(player: Player, x: Double, y: Double, z: Double, speed: Double = 1.0): CompletableFuture<ResponseMessage> =
        BlackBoxApi.sendAsync(player, "pathfind_to", JsonObject().apply {
            addProperty("x", x); addProperty("y", y); addProperty("z", z)
            addProperty("speed", speed)
        }, timeoutMs = 30000)

    fun wait(player: Player, ticks: Int): CompletableFuture<ResponseMessage> =
        BlackBoxApi.sendAsync(player, "wait", JsonObject().apply {
            addProperty("ticks", ticks)
        })

    fun respawn(player: Player): CompletableFuture<ResponseMessage> =
        BlackBoxApi.sendAsync(player, "respawn")

    fun batch(player: Player, actions: List<Pair<String, JsonObject>>): CompletableFuture<ResponseMessage> {
        val batchParams = JsonObject().apply {
            add("actions", JsonArray().apply {
                actions.forEach { (action, params) ->
                    add(JsonObject().apply {
                        addProperty("action", action)
                        add("params", params)
                    })
                }
            })
        }
        return BlackBoxApi.sendAsync(player, "batch", batchParams)
    }

    fun craftRecipe(player: Player, windowId: Int, recipeIndex: Int, makeAll: Boolean = false): CompletableFuture<ResponseMessage> =
        BlackBoxApi.sendAsync(player, "craft_recipe", JsonObject().apply {
            addProperty("windowId", windowId); addProperty("recipeIndex", recipeIndex); addProperty("makeAll", makeAll)
        })
}
