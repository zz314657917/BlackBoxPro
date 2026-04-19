package com.blackboxpro.plugin.api.action

import com.blackboxpro.plugin.api.BlackBoxApi
import com.blackboxpro.common.protocol.ResponseMessage
import com.google.gson.JsonObject
import org.bukkit.entity.Player
import java.util.concurrent.CompletableFuture

/**
 * 实体交互与战斗行为快捷 API。
 */
object EntityActions {

    fun attackEntity(player: Player, entityId: Int, sneaking: Boolean = false): CompletableFuture<ResponseMessage> =
        BlackBoxApi.sendAsync(player, "attack_entity", JsonObject().apply {
            addProperty("entityId", entityId); addProperty("sneaking", sneaking)
        })

    fun interactEntity(player: Player, entityId: Int, hand: String = "main_hand", sneaking: Boolean = false): CompletableFuture<ResponseMessage> =
        BlackBoxApi.sendAsync(player, "interact_entity", JsonObject().apply {
            addProperty("entityId", entityId); addProperty("hand", hand); addProperty("sneaking", sneaking)
        })

    fun interactEntityAt(
        player: Player, entityId: Int,
        targetX: Double, targetY: Double, targetZ: Double,
        hand: String = "main_hand", sneaking: Boolean = false
    ): CompletableFuture<ResponseMessage> =
        BlackBoxApi.sendAsync(player, "interact_entity_at", JsonObject().apply {
            addProperty("entityId", entityId)
            addProperty("targetX", targetX); addProperty("targetY", targetY); addProperty("targetZ", targetZ)
            addProperty("hand", hand); addProperty("sneaking", sneaking)
        })

    fun swingArm(player: Player, hand: String = "main_hand"): CompletableFuture<ResponseMessage> =
        BlackBoxApi.sendAsync(player, "swing_arm", JsonObject().apply {
            addProperty("hand", hand)
        })
}
