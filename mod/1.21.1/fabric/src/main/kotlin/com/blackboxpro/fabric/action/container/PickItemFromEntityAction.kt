package com.blackboxpro.fabric.action.container

import com.blackboxpro.fabric.action.ActionExecutor
import com.blackboxpro.fabric.action.ActionResult
import com.blackboxpro.fabric.util.getBooleanOrDefault
import com.blackboxpro.fabric.util.requireInt
import com.google.gson.JsonObject
import net.minecraft.client.MinecraftClient
import net.minecraft.network.packet.c2s.play.PickFromInventoryC2SPacket

class PickItemFromEntityAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val entityId = params.requireInt("entityId")

        val client = MinecraftClient.getInstance()
        val player = client.player ?: return ActionResult.fail("Player not available")
        val networkHandler = client.networkHandler
            ?: return ActionResult.fail("Not connected to server")
        val world = client.world
            ?: return ActionResult.fail("World not available")
        world.getEntityById(entityId)
            ?: return ActionResult.fail("Entity not found: $entityId")

        networkHandler.sendPacket(PickFromInventoryC2SPacket(player.inventory.selectedSlot))
        return ActionResult.ok("Picked item from entity $entityId")
    }
}
