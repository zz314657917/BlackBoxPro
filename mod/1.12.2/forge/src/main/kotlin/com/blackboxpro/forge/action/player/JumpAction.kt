package com.blackboxpro.forge.action.player

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft

/**
 * 模拟玩家跳跃。
 * Action ID: "jump"
 */
class JumpAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val player = Minecraft.getMinecraft().player
            ?: return ActionResult.fail("Player not available")

        if (!player.onGround) {
            return ActionResult.fail("Player is not on ground")
        }

        val fromY = player.posY
        player.jump()

        val data = JsonObject().apply {
            addProperty("jumped", true)
            addProperty("fromY", fromY)
            addProperty("velocity", player.motionY)
        }

        return ActionResult.ok("Player jumped", data)
    }
}
