package com.blackboxpro.fabric.action.container

import com.blackboxpro.fabric.action.ActionExecutor
import com.blackboxpro.fabric.action.ActionResult
import com.blackboxpro.fabric.util.getBooleanOrDefault
import com.blackboxpro.fabric.util.requireInt
import com.google.gson.JsonObject
import net.minecraft.client.MinecraftClient
import net.minecraft.network.packet.c2s.play.PickFromInventoryC2SPacket

class PickItemFromBlockAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val client = MinecraftClient.getInstance()
        val player = client.player ?: return ActionResult.fail("Player not available")
        val networkHandler = client.networkHandler
            ?: return ActionResult.fail("Not connected to server")

        val x = params.requireInt("x")
        val y = params.requireInt("y")
        val z = params.requireInt("z")

        networkHandler.sendPacket(PickFromInventoryC2SPacket(player.inventory.selectedSlot))
        return ActionResult.ok("Picked item from block at $x, $y, $z")
    }
}
