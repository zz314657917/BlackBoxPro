package com.blackboxpro.neoforge.action.player

import com.blackboxpro.neoforge.action.ActionExecutor
import com.blackboxpro.neoforge.action.ActionResult
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft

/**
 * 模拟玩家跳跃。
 * Action ID: "jump"
 */
class JumpAction : ActionExecutor {

    override fun execute(params: JsonObject): ActionResult {
        val client = Minecraft.getInstance()
        val player = client.player
            ?: return ActionResult.fail("Player not available")

        if (!player.onGround()) {
            return ActionResult.fail("Player is not on ground")
        }

        val fromY = player.y
        player.jumpFromGround()

        val data = JsonObject().apply {
            addProperty("jumped", true)
            addProperty("fromY", fromY)
            addProperty("velocity", player.deltaMovement.y)
        }

        return ActionResult.ok("Player jumped", data)
    }
}
