package com.blackboxpro.forge.action.container

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.blackboxpro.forge.util.getBooleanOrDefault
import com.blackboxpro.forge.util.requireInt
import com.blackboxpro.forge.util.PickItemSupport
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft

class PickItemFromEntityAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val entityId = params.requireInt("entityId")
        val includeData = params.getBooleanOrDefault("includeData", false)

        val client = Minecraft.getInstance()
        val entity = client.level?.getEntity(entityId)
            ?: return ActionResult.fail("Entity not found: $entityId")

        return PickItemSupport.pickEntity(entity, includeData, "Picked item from entity $entityId")
    }
}
