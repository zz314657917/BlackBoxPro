package com.blackboxpro.neoforge.action.movement

import com.blackboxpro.neoforge.action.ActionExecutor
import com.blackboxpro.neoforge.action.ActionResult
import com.blackboxpro.neoforge.util.requireBoolean
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket

class PlayerOnGroundAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val onGround = params.requireBoolean("onGround")

        val networkHandler = Minecraft.getInstance().connection
            ?: return ActionResult.fail("Not connected to server")

        networkHandler.send(ServerboundMovePlayerPacket.StatusOnly(onGround, false))
        return ActionResult.ok()
    }
}
