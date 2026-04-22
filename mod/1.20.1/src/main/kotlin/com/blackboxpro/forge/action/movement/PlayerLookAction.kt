package com.blackboxpro.forge.action.movement

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.blackboxpro.forge.util.getBooleanOrDefault
import com.blackboxpro.forge.util.requireDouble
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket

class PlayerLookAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val yaw = params.requireDouble("yaw").toFloat()
        val pitch = params.requireDouble("pitch").toFloat()
        val onGround = params.getBooleanOrDefault("onGround", true)

        val client = Minecraft.getInstance()
        val player = client.player
            ?: return ActionResult.fail("Player not available")
        val connection = client.connection
            ?: return ActionResult.fail("Not connected to server")

        player.yRot = yaw
        player.xRot = pitch
        connection.send(ServerboundMovePlayerPacket.Rot(yaw, pitch, onGround))
        return ActionResult.ok()
    }
}

