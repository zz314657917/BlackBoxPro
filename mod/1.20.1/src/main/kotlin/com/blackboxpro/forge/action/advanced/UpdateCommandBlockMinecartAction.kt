package com.blackboxpro.forge.action.advanced

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.blackboxpro.forge.util.getBooleanOrDefault
import com.blackboxpro.forge.util.requireInt
import com.blackboxpro.forge.util.requireString
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

