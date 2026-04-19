package com.blackboxpro.forge.action.entity

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.blackboxpro.forge.util.HandUtil
import com.blackboxpro.forge.util.getStringOrNull
import com.blackboxpro.forge.util.requireDouble
import com.blackboxpro.forge.util.requireInt
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.util.math.RayTraceResult
import net.minecraft.util.math.Vec3d

class InteractEntityAtAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val entityId = params.requireInt("entityId")
        val targetX = params.requireDouble("targetX")
        val targetY = params.requireDouble("targetY")
        val targetZ = params.requireDouble("targetZ")
        val hand = HandUtil.fromString(params.getStringOrNull("hand") ?: "main_hand")

        val mc = Minecraft.getMinecraft()
        val player = mc.player
            ?: return ActionResult.fail("Player not available")
        val world = mc.world
            ?: return ActionResult.fail("World not available")
        val entity = world.getEntityByID(entityId)
            ?: return ActionResult.fail("Entity not found: $entityId")

        val hitVec = Vec3d(targetX, targetY, targetZ)
        val rayTrace = RayTraceResult(entity, hitVec)
        mc.playerController.interactWithEntity(player, entity, rayTrace, hand)
        return ActionResult.ok("Interacted with entity $entityId at ($targetX, $targetY, $targetZ)")
    }
}
