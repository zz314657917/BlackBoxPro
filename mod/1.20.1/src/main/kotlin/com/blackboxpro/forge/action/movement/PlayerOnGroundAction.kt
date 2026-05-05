package com.blackboxpro.forge.action.movement

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.blackboxpro.forge.util.requireBoolean
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket

class PlayerOnGroundAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val onGround = params.requireBoolean("onGround")

        val networkHandler = Minecraft.getInstance().connection
            ?: return ActionResult.fail("Not connected to server")

        networkHandler.send(ServerboundMovePlayerPacket.StatusOnly(onGround))
        return ActionResult.ok()
    }
}

