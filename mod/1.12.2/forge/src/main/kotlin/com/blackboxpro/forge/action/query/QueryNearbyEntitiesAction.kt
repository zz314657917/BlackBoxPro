package com.blackboxpro.forge.action.query

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.blackboxpro.forge.util.getDoubleOrDefault
import com.blackboxpro.forge.util.getIntOrDefault
import com.blackboxpro.forge.util.getStringOrNull
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.entity.EntityLivingBase
import net.minecraft.util.math.AxisAlignedBB
import kotlin.math.sqrt

class QueryNearbyEntitiesAction : ActionExecutor {

    override fun execute(params: JsonObject): ActionResult {
        val mc = Minecraft.getMinecraft()
        val player = mc.player
            ?: return ActionResult.fail("Player not available")
        val world = mc.world
            ?: return ActionResult.fail("World not available")

        val radius = params.getDoubleOrDefault("radius", 10.0).coerceIn(0.1, 64.0)
        val typeFilter = params.getStringOrNull("type")
        val limit = params.getIntOrDefault("limit", 20).coerceIn(1, 100)

        val box = player.entityBoundingBox.grow(radius)
        var entities = world.getEntitiesInAABBexcluding(player, box, null)

        if (typeFilter != null) {
            entities = entities.filter {
                net.minecraft.entity.EntityList.getKey(it)?.toString() == typeFilter
            }
        }

        val sorted = entities
            .map { it to it.getDistanceSq(player) }
            .sortedBy { it.second }
            .take(limit)

        val arr = JsonArray()
        sorted.forEach { (entity, distSq) ->
            arr.add(JsonObject().apply {
                addProperty("entityId", entity.entityId)
                addProperty("uuid", entity.uniqueID.toString())
                addProperty("type", net.minecraft.entity.EntityList.getKey(entity)?.toString() ?: "unknown")
                addProperty("name", entity.name)
                addProperty("x", entity.posX)
                addProperty("y", entity.posY)
                addProperty("z", entity.posZ)
                addProperty("distance", sqrt(distSq))
                if (entity is EntityLivingBase) {
                    addProperty("health", entity.health)
                    addProperty("maxHealth", entity.maxHealth)
                }
            })
        }

        val data = JsonObject().apply {
            add("entities", arr)
            addProperty("count", arr.size())
        }
        return ActionResult.ok("Found ${arr.size()} entities", data)
    }
}
