package com.blackboxpro.forge.action.entity

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.blackboxpro.forge.util.getBooleanOrDefault
import com.blackboxpro.forge.util.getStringOrNull
import com.blackboxpro.forge.util.requireDouble
import com.blackboxpro.forge.util.requireInt
import com.blackboxpro.forge.util.HandUtil
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.network.protocol.game.ServerboundInteractPacket
import net.minecraft.world.phys.Vec3

class InteractEntityAtAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val entityId = params.requireInt("entityId")
        val targetX = params.requireDouble("targetX")
        val targetY = params.requireDouble("targetY")
        val targetZ = params.requireDouble("targetZ")
        val hand = HandUtil.fromString(params.getStringOrNull("hand") ?: "main_hand")
        val sneaking = params.getBooleanOrDefault("sneaking", false)

        val world = Minecraft.getInstance().level
            ?: return ActionResult.fail("World not available")
        val entity = world.getEntity(entityId)
            ?: return ActionResult.fail("Entity not found: $entityId")

        val target = Vec3(targetX, targetY, targetZ)
        val packet = ServerboundInteractPacket.createInteractionPacket(entity, sneaking, hand, target)
        Minecraft.getInstance().connection?.send(packet)
            ?: return ActionResult.fail("Not connected to server")

        return ActionResult.ok("Interacted with entity $entityId at ($targetX, $targetY, $targetZ)")
    }
}

