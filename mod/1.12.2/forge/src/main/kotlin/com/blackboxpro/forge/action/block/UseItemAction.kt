package com.blackboxpro.forge.action.block

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.blackboxpro.forge.util.HandUtil
import com.blackboxpro.forge.util.getStringOrNull
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.util.math.RayTraceResult

class UseItemAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val hand = HandUtil.fromString(params.getStringOrNull("hand") ?: "main_hand")

        val mc = Minecraft.getMinecraft()
        val player = mc.player
            ?: return ActionResult.fail("Player not available")
        val playerController = mc.playerController
            ?: return ActionResult.fail("PlayerController not available")

        // 调用原版客户端交互 API，完整模拟右键使用物品：
        // 1. 如果对准方块，先触发 processRightClickBlock（发送 UseItemOnBlockPacket → 服务端 PlayerInteractEvent）
        // 2. 仅当 processRightClickBlock 未消费交互时，才 fallback 到 processRightClick（与原版客户端行为一致）
        val hitResult = mc.objectMouseOver
        var consumed = false
        if (hitResult != null && hitResult.typeOfHit == RayTraceResult.Type.BLOCK) {
            val result = playerController.processRightClickBlock(
                player, mc.world, hitResult.blockPos, hitResult.sideHit, hitResult.hitVec, hand
            )
            consumed = result != net.minecraft.util.EnumActionResult.PASS
        }
        if (!consumed) {
            playerController.processRightClick(player, mc.world, hand)
        }

        return ActionResult.ok("Used item")
    }
}
