package com.blackboxpro.forge.action.player

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.blackboxpro.forge.util.requireString
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.network.protocol.game.ServerboundTeleportToEntityPacket
import java.util.UUID

class SpectatorTeleportAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val uuidStr = params.requireString("targetUuid")

        val uuid = try {
            UUID.fromString(uuidStr)
        } catch (e: IllegalArgumentException) {
            return ActionResult.fail("Invalid UUID: $uuidStr")
        }

        val networkHandler = Minecraft.getInstance().connection
            ?: return ActionResult.fail("Not connected to server")

        networkHandler.send(ServerboundTeleportToEntityPacket(uuid))
        return ActionResult.ok("Teleported to player $uuidStr")
    }
}

