package com.blackboxpro.neoforge.action.player

import com.blackboxpro.neoforge.action.ActionExecutor
import com.blackboxpro.neoforge.action.ActionResult
import com.blackboxpro.neoforge.util.HandUtil
import com.blackboxpro.neoforge.util.getStringOrNull
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.core.Direction
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult

/**
 * 模拟左键点击（空气或方块），触发服务端 PlayerInteractEvent(LEFT_CLICK_AIR/LEFT_CLICK_BLOCK)。
 * swing_arm 只发送动画包，不会触发 Bukkit 的 PlayerInteractEvent。
 */
class LeftClickAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val hand = HandUtil.fromString(params.getStringOrNull("hand") ?: "main_hand")

        val client = Minecraft.getInstance()
        val handler = client.connection
            ?: return ActionResult.fail("Not connected to server")
        val player = client.player
            ?: return ActionResult.fail("Player not available")

        val hitResult = client.hitResult
        if (hitResult is BlockHitResult && hitResult.type == HitResult.Type.BLOCK) {
            // 对准方块：发送 START_DESTROY_BLOCK 触发 LEFT_CLICK_BLOCK
            handler.send(
                ServerboundPlayerActionPacket(
                    ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,
                    hitResult.blockPos,
                    hitResult.direction,
                    0
                )
            )
            // 立即取消挖掘，避免实际破坏方块
            handler.send(
                ServerboundPlayerActionPacket(
                    ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK,
                    hitResult.blockPos,
                    hitResult.direction,
                    0
                )
            )
        } else {
            // 未对准方块：对准脚下方块发送 START_DESTROY_BLOCK 触发 LEFT_CLICK 事件
            handler.send(
                ServerboundPlayerActionPacket(
                    ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,
                    player.blockPosition(),
                    Direction.DOWN,
                    0
                )
            )
            handler.send(
                ServerboundPlayerActionPacket(
                    ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK,
                    player.blockPosition(),
                    Direction.DOWN,
                    0
                )
            )
        }

        // 同时发送挥手动画
        player.swing(hand)
        return ActionResult.ok("Left clicked")
    }
}
