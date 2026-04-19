package com.blackboxpro.neoforge.action.container

import com.blackboxpro.neoforge.action.ActionExecutor
import com.blackboxpro.neoforge.action.ActionResult
import com.blackboxpro.neoforge.util.getBooleanOrDefault
import com.blackboxpro.neoforge.util.requireInt
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.network.protocol.game.ServerboundPickItemFromEntityPacket

class PickEntityAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val entityId = params.requireInt("entityId")
        val includeData = params.getBooleanOrDefault("includeData", false)

        val client = Minecraft.getInstance()
        val handler = client.connection
            ?: return ActionResult.fail("Not connected to server")

        val world = client.level
            ?: return ActionResult.fail("World not loaded")
        world.getEntity(entityId)
            ?: return ActionResult.fail("Entity not found: $entityId")

        handler.send(ServerboundPickItemFromEntityPacket(entityId, includeData))
        return ActionResult.ok("Picked entity $entityId")
    }
}
