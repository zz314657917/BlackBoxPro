package com.blackboxpro.neoforge.action.entity

import com.blackboxpro.neoforge.action.ActionExecutor
import com.blackboxpro.neoforge.action.ActionResult
import com.blackboxpro.neoforge.util.getBooleanOrDefault
import com.blackboxpro.neoforge.util.getStringOrNull
import com.blackboxpro.neoforge.util.requireInt
import com.blackboxpro.neoforge.util.HandUtil
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.network.protocol.game.ServerboundInteractPacket

class InteractEntityAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val entityId = params.requireInt("entityId")
        val hand = HandUtil.fromString(params.getStringOrNull("hand") ?: "main_hand")
        val sneaking = params.getBooleanOrDefault("sneaking", false)

        val world = Minecraft.getInstance().level
            ?: return ActionResult.fail("World is not available")
        val entity = world.getEntity(entityId)
            ?: return ActionResult.fail("Entity not found: $entityId")

        val packet = ServerboundInteractPacket.createInteractionPacket(entity, sneaking, hand)
        Minecraft.getInstance().connection?.send(packet)
            ?: return ActionResult.fail("Network handler is not available")

        return ActionResult.ok("Interacted with entity $entityId")
    }
}
