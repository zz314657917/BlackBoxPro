package com.blackboxpro.neoforge.action.query

import com.blackboxpro.neoforge.action.ActionExecutor
import com.blackboxpro.neoforge.action.ActionResult
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.effect.MobEffectInstance

/**
 * 读取玩家当前药水效果列表。
 * Action ID: "query_active_effects"
 */
class QueryActiveEffectsAction : ActionExecutor {

    override fun execute(params: JsonObject): ActionResult {
        val client = Minecraft.getInstance()
        val player = client.player
            ?: return ActionResult.fail("Player not available")

        val arr = JsonArray()
        player.activeEffects.forEach { instance: MobEffectInstance ->
            arr.add(JsonObject().apply {
                addProperty("id", BuiltInRegistries.MOB_EFFECT.getKey(instance.effect.value())?.toString() ?: "unknown")
                addProperty("amplifier", instance.amplifier)
                addProperty("duration", instance.duration)
                addProperty("ambient", instance.isAmbient)
                addProperty("visible", instance.isVisible)
            })
        }

        val data = JsonObject().apply { add("effects", arr) }
        return ActionResult.ok("Active effects queried", data)
    }
}
