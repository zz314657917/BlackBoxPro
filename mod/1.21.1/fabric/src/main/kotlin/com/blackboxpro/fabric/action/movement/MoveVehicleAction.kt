package com.blackboxpro.fabric.action.movement

import com.blackboxpro.fabric.action.ActionExecutor
import com.blackboxpro.fabric.action.ActionResult
import com.blackboxpro.fabric.util.getBooleanOrDefault
import com.blackboxpro.fabric.util.requireDouble
import com.google.gson.JsonObject
import net.minecraft.client.MinecraftClient
import net.minecraft.network.packet.c2s.play.VehicleMoveC2SPacket

class MoveVehicleAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val x = params.requireDouble("x")
        val y = params.requireDouble("y")
        val z = params.requireDouble("z")
        val yaw = params.requireDouble("yaw").toFloat()
        val pitch = params.requireDouble("pitch").toFloat()
        val onGround = params.getBooleanOrDefault("onGround", true)

        val client = MinecraftClient.getInstance()
        val vehicle = client.player?.vehicle ?: return ActionResult.fail("Vehicle not available")
        val networkHandler = client.networkHandler
            ?: return ActionResult.fail("Not connected to server")

        vehicle.setPos(x, y, z)
        vehicle.yaw = yaw
        vehicle.pitch = pitch
        networkHandler.sendPacket(VehicleMoveC2SPacket(vehicle))
        return ActionResult.ok()
    }
}
