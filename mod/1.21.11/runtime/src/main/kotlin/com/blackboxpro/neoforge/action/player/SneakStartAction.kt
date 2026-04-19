package com.blackboxpro.neoforge.action.player

import com.blackboxpro.neoforge.action.ActionExecutor
import com.blackboxpro.neoforge.action.ActionResult
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.world.entity.player.Input

class SneakStartAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val client = Minecraft.getInstance()
        val player = client.player
            ?: return ActionResult.fail("Player not available")

        // 设置客户端本地 sneak 状态，后续 tick 循环会自动在 PlayerInput 中携带 shift=true
        val kp = player.input.keyPresses
        player.input.keyPresses = Input(kp.forward(), kp.backward(), kp.left(), kp.right(), kp.jump(), true, kp.sprint())
        player.setShiftKeyDown(true)
        return ActionResult.ok("Started sneaking")
    }
}
