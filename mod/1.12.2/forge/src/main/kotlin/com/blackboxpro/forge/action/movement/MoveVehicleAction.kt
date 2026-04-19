package com.blackboxpro.forge.action.movement

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.blackboxpro.forge.util.requireDouble
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.network.play.client.CPacketVehicleMove

class MoveVehicleAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val x = params.requireDouble("x")
        val y = params.requireDouble("y")
        val z = params.requireDouble("z")
        val yaw = params.requireDouble("yaw").toFloat()
        val pitch = params.requireDouble("pitch").toFloat()

        val mc = Minecraft.getMinecraft()
        val player = mc.player
            ?: return ActionResult.fail("Player not available")
        val vehicle = player.ridingEntity
            ?: return ActionResult.fail("Player is not riding a vehicle")

        // 1.12.2: set vehicle position/rotation directly, then send packet
        vehicle.setPositionAndRotation(x, y, z, yaw, pitch)

        val connection = mc.connection
            ?: return ActionResult.fail("Not connected to server")

        val packet = CPacketVehicleMove(vehicle)
        connection.sendPacket(packet)
        return ActionResult.ok()
    }
}
