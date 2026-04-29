package com.blackboxpro.fabric.action.player

import com.blackboxpro.fabric.action.ActionExecutor
import com.blackboxpro.fabric.action.ActionResult
import com.blackboxpro.fabric.util.HandUtil
import com.blackboxpro.fabric.util.getStringOrNull
import com.google.gson.JsonObject
import net.minecraft.client.MinecraftClient
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.Direction

/**
 * 模拟左键点击（空气或方块），触发服务端 PlayerInteractEvent(LEFT_CLICK_AIR/LEFT_CLICK_BLOCK)。
 * swing_arm 只发送动画包，不会触发 Bukkit 的 PlayerInteractEvent。
 */
class LeftClickAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val hand = HandUtil.fromString(params.getStringOrNull("hand") ?: "main_hand")

        val client = MinecraftClient.getInstance()
        val handler = client.networkHandler
            ?: return ActionResult.fail("Not connected to server")
        val player = client.player
            ?: return ActionResult.fail("Player not available")

        val hitResult = client.crosshairTarget
        if (hitResult is BlockHitResult && hitResult.type == HitResult.Type.BLOCK) {
            handler.sendPacket(
                PlayerActionC2SPacket(
                    PlayerActionC2SPacket.Action.START_DESTROY_BLOCK,
                    hitResult.blockPos,
                    hitResult.side
                )
            )
            handler.sendPacket(
                PlayerActionC2SPacket(
                    PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK,
                    hitResult.blockPos,
                    hitResult.side
                )
            )
        } else {
            handler.sendPacket(
                PlayerActionC2SPacket(
                    PlayerActionC2SPacket.Action.START_DESTROY_BLOCK,
                    player.blockPos,
                    Direction.DOWN
                )
            )
            handler.sendPacket(
                PlayerActionC2SPacket(
                    PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK,
                    player.blockPos,
                    Direction.DOWN
                )
            )
        }

        player.swingHand(hand)
        return ActionResult.ok("Left clicked")
    }
}
