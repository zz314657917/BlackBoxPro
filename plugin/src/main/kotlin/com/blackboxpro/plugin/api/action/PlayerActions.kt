package com.blackboxpro.plugin.api.action

import com.blackboxpro.plugin.api.BlackBoxApi
import com.blackboxpro.common.protocol.ResponseMessage
import com.google.gson.JsonObject
import org.bukkit.entity.Player
import java.util.concurrent.CompletableFuture

/**
 * 玩家状态与动作行为快捷 API。
 */
object PlayerActions {

    fun sneakStart(player: Player): CompletableFuture<ResponseMessage> =
        BlackBoxApi.sendAsync(player, "sneak_start")

    fun sneakStop(player: Player): CompletableFuture<ResponseMessage> =
        BlackBoxApi.sendAsync(player, "sneak_stop")

    fun sprintStart(player: Player): CompletableFuture<ResponseMessage> =
        BlackBoxApi.sendAsync(player, "sprint_start")

    fun sprintStop(player: Player): CompletableFuture<ResponseMessage> =
        BlackBoxApi.sendAsync(player, "sprint_stop")

    fun leaveBed(player: Player): CompletableFuture<ResponseMessage> =
        BlackBoxApi.sendAsync(player, "leave_bed")

    fun horseJumpStart(player: Player, jumpBoost: Int = 100): CompletableFuture<ResponseMessage> =
        BlackBoxApi.sendAsync(player, "horse_jump_start", JsonObject().apply {
            addProperty("jumpBoost", jumpBoost)
        })

    fun horseJumpStop(player: Player): CompletableFuture<ResponseMessage> =
        BlackBoxApi.sendAsync(player, "horse_jump_stop")

    fun openHorseInventory(player: Player): CompletableFuture<ResponseMessage> =
        BlackBoxApi.sendAsync(player, "open_horse_inventory")

    fun elytraStart(player: Player): CompletableFuture<ResponseMessage> =
        BlackBoxApi.sendAsync(player, "elytra_start")

    fun dropItem(player: Player): CompletableFuture<ResponseMessage> =
        BlackBoxApi.sendAsync(player, "drop_item")

    fun dropItemStack(player: Player): CompletableFuture<ResponseMessage> =
        BlackBoxApi.sendAsync(player, "drop_item_stack")

    fun finishUsing(player: Player, sequence: Int = 0): CompletableFuture<ResponseMessage> =
        BlackBoxApi.sendAsync(player, "finish_using", JsonObject().apply {
            addProperty("sequence", sequence)
        })

    fun spectatorTeleport(player: Player, targetUuid: String): CompletableFuture<ResponseMessage> =
        BlackBoxApi.sendAsync(player, "spectator_teleport", JsonObject().apply {
            addProperty("targetUuid", targetUuid)
        })

    fun swapHands(player: Player): CompletableFuture<ResponseMessage> =
        BlackBoxApi.sendAsync(player, "swap_hands")

    fun performRespawn(player: Player): CompletableFuture<ResponseMessage> =
        BlackBoxApi.sendAsync(player, "perform_respawn")

    fun jump(player: Player): CompletableFuture<ResponseMessage> =
        BlackBoxApi.sendAsync(player, "jump")
}
