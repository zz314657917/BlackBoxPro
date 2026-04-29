package com.blackboxpro.forge.action.player

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket

class SneakStopAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val client = Minecraft.getInstance()
        val networkHandler = client.connection
            ?: return ActionResult.fail("Not connected to server")

        // 1.21.1: sneak 涓嶅啀閫氳繃 PlayerCommand 鎺у埗锛屾敼鐢?PlayerInput
        networkHandler.send(ServerboundPlayerInputPacket(0.0f, 0.0f, false, false))
        return ActionResult.ok("Stopped sneaking")
    }
}

