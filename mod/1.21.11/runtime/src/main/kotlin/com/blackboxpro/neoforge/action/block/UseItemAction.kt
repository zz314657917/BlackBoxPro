package com.blackboxpro.neoforge.action.block

import com.blackboxpro.neoforge.action.ActionExecutor
import com.blackboxpro.neoforge.action.ActionResult
import com.blackboxpro.neoforge.util.HandUtil
import com.blackboxpro.neoforge.util.getStringOrNull
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft

class UseItemAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val hand = HandUtil.fromString(params.getStringOrNull("hand") ?: "main_hand")

        val client = Minecraft.getInstance()
        val player = client.player
            ?: return ActionResult.fail("Player not available")
        val gameMode = client.gameMode
            ?: return ActionResult.fail("GameMode not available")

        // 调用原版客户端交互 API，完整模拟右键使用物品：
        // 1. 如果对准方块，先触发 useItemOn（发送 UseItemOnPacket → 服务端 PlayerInteractEvent）
        // 2. 仅当 useItemOn 未消费交互时，才 fallback 到 useItem（与原版客户端行为一致）
        val hitResult = client.hitResult
        var consumed = false
        if (hitResult != null && hitResult.type == net.minecraft.world.phys.HitResult.Type.BLOCK) {
            val blockHit = hitResult as net.minecraft.world.phys.BlockHitResult
            val result = gameMode.useItemOn(player, hand, blockHit)
            consumed = result.consumesAction()
        }
        if (!consumed) {
            gameMode.useItem(player, hand)
        }

        return ActionResult.ok("Used item")
    }
}
