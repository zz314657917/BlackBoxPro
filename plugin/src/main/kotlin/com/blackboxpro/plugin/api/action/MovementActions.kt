package com.blackboxpro.plugin.api.action

import com.blackboxpro.plugin.api.BlackBoxApi
import com.blackboxpro.common.protocol.ResponseMessage
import com.google.gson.JsonObject
import org.bukkit.entity.Player
import java.util.concurrent.CompletableFuture

/**
 * 移动与位置行为快捷 API。
 */
object MovementActions {

    fun playerMove(
        player: Player,
        x: Double, y: Double, z: Double,
        speed: Double = 1.0,
        timeout: Int = 200
    ): CompletableFuture<ResponseMessage> =
        BlackBoxApi.sendAsync(player, "player_move", JsonObject().apply {
            addProperty("x", x); addProperty("y", y); addProperty("z", z)
            addProperty("speed", speed); addProperty("timeout", timeout)
        }, timeoutMs = (timeout * 50L + 5000L))

    fun playerMoveLook(
        player: Player,
        x: Double, y: Double, z: Double,
        pitch: Float,
        speed: Double = 1.0,
        timeout: Int = 200
    ): CompletableFuture<ResponseMessage> =
        BlackBoxApi.sendAsync(player, "player_move_look", JsonObject().apply {
            addProperty("x", x); addProperty("y", y); addProperty("z", z)
            addProperty("pitch", pitch)
            addProperty("speed", speed); addProperty("timeout", timeout)
        }, timeoutMs = (timeout * 50L + 5000L))

    fun playerLook(player: Player, yaw: Float, pitch: Float, onGround: Boolean = true): CompletableFuture<ResponseMessage> =
        BlackBoxApi.sendAsync(player, "player_look", JsonObject().apply {
            addProperty("yaw", yaw); addProperty("pitch", pitch)
            addProperty("onGround", onGround)
        })

    fun playerOnGround(player: Player, onGround: Boolean): CompletableFuture<ResponseMessage> =
        BlackBoxApi.sendAsync(player, "player_on_ground", JsonObject().apply {
            addProperty("onGround", onGround)
        })

    fun confirmTeleportation(player: Player, teleportId: Int): CompletableFuture<ResponseMessage> =
        BlackBoxApi.sendAsync(player, "confirm_teleportation", JsonObject().apply {
            addProperty("teleportId", teleportId)
        })

    fun moveVehicle(player: Player, x: Double, y: Double, z: Double, yaw: Float, pitch: Float): CompletableFuture<ResponseMessage> =
        BlackBoxApi.sendAsync(player, "move_vehicle", JsonObject().apply {
            addProperty("x", x); addProperty("y", y); addProperty("z", z)
            addProperty("yaw", yaw); addProperty("pitch", pitch)
        })

    fun paddleBoat(player: Player, leftPaddling: Boolean, rightPaddling: Boolean): CompletableFuture<ResponseMessage> =
        BlackBoxApi.sendAsync(player, "paddle_boat", JsonObject().apply {
            addProperty("leftPaddling", leftPaddling); addProperty("rightPaddling", rightPaddling)
        })

    fun playerInput(
        player: Player,
        forward: Boolean = false,
        backward: Boolean = false,
        left: Boolean = false,
        right: Boolean = false,
        jump: Boolean = false,
        sneak: Boolean = false,
        sprint: Boolean = false
    ): CompletableFuture<ResponseMessage> =
        BlackBoxApi.sendAsync(player, "player_input", JsonObject().apply {
            addProperty("forward", forward); addProperty("backward", backward)
            addProperty("left", left); addProperty("right", right)
            addProperty("jump", jump); addProperty("sneak", sneak)
            addProperty("sprint", sprint)
        })
}
