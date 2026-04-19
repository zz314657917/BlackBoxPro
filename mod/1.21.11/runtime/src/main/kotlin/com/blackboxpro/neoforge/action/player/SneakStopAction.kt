package com.blackboxpro.neoforge.action.player

import com.blackboxpro.neoforge.action.ActionExecutor
import com.blackboxpro.neoforge.action.ActionResult
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.world.entity.player.Input

class SneakStopAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val client = Minecraft.getInstance()
        val player = client.player
            ?: return ActionResult.fail("Player not available")

        // 清除客户端本地 sneak 状态
        val kp = player.input.keyPresses
        player.input.keyPresses = Input(kp.forward(), kp.backward(), kp.left(), kp.right(), kp.jump(), false, kp.sprint())
        player.setShiftKeyDown(false)
        return ActionResult.ok("Stopped sneaking")
    }
}
