package com.blackboxpro.neoforge.action.advanced

import com.blackboxpro.neoforge.action.ActionExecutor
import com.blackboxpro.neoforge.action.ActionResult
import com.blackboxpro.neoforge.util.getBooleanOrDefault
import com.blackboxpro.neoforge.util.requireInt
import com.blackboxpro.neoforge.util.requireString
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.network.protocol.game.ServerboundSetCommandMinecartPacket

class UpdateCommandBlockMinecartAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val entityId = params.requireInt("entityId")
        val command = params.requireString("command")
        val trackOutput = params.getBooleanOrDefault("trackOutput", true)

        val networkHandler = Minecraft.getInstance().connection
            ?: return ActionResult.fail("Not connected to server")

        networkHandler.send(
            ServerboundSetCommandMinecartPacket(entityId, command, trackOutput)
        )
        return ActionResult.ok("Updated command block minecart entity $entityId")
    }
}
