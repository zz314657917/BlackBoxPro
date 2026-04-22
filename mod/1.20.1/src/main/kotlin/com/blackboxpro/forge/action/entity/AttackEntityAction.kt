package com.blackboxpro.forge.action.entity

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.blackboxpro.forge.util.getBooleanOrDefault
import com.blackboxpro.forge.util.requireInt
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.network.protocol.game.ServerboundInteractPacket

class AttackEntityAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val entityId = params.requireInt("entityId")
        val sneaking = params.getBooleanOrDefault("sneaking", false)

        val world = Minecraft.getInstance().level
            ?: return ActionResult.fail("World is not available")
        val entity = world.getEntity(entityId)
            ?: return ActionResult.fail("Entity not found: $entityId")

        val packet = ServerboundInteractPacket.createAttackPacket(entity, sneaking)
        Minecraft.getInstance().connection?.send(packet)
            ?: return ActionResult.fail("Network handler is not available")

        return ActionResult.ok("Attacked entity $entityId")
    }
}

