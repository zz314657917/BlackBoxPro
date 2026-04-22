package com.blackboxpro.forge.action.movement

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.blackboxpro.forge.util.requireInt
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket

class ConfirmTeleportationAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val teleportId = params.requireInt("teleportId")

        val networkHandler = Minecraft.getInstance().connection
            ?: return ActionResult.fail("Not connected to server")

        networkHandler.send(ServerboundAcceptTeleportationPacket(teleportId))
        return ActionResult.ok()
    }
}

