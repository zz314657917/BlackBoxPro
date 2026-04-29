package com.blackboxpro.forge.action.player

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft

/**
 * 妯℃嫙鐜╁璺宠穬銆?
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

