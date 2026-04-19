package com.blackboxpro.forge.action.entity

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.blackboxpro.forge.util.requireInt
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft

class AttackEntityAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val entityId = params.requireInt("entityId")

        val mc = Minecraft.getMinecraft()
        val player = mc.player
            ?: return ActionResult.fail("Player not available")
        val world = mc.world
            ?: return ActionResult.fail("World not available")
        val entity = world.getEntityByID(entityId)
            ?: return ActionResult.fail("Entity not found: $entityId")

        mc.playerController.attackEntity(player, entity)
        return ActionResult.ok("Attacked entity $entityId")
    }
}
