package com.blackboxpro.forge.action.query

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.potion.Potion

class QueryActiveEffectsAction : ActionExecutor {

    override fun execute(params: JsonObject): ActionResult {
        val mc = Minecraft.getMinecraft()
        val player = mc.player
            ?: return ActionResult.fail("Player not available")

        val arr = JsonArray()
        player.activePotionEffects.forEach { effect ->
            arr.add(JsonObject().apply {
                val potion = effect.potion
                addProperty("id", Potion.REGISTRY.getNameForObject(potion)?.toString() ?: "unknown")
                addProperty("amplifier", effect.amplifier)
                addProperty("duration", effect.duration)
                addProperty("ambient", effect.isAmbient)
                addProperty("visible", effect.doesShowParticles())
            })
        }

        val data = JsonObject().apply { add("effects", arr) }
        return ActionResult.ok("Active effects queried", data)
    }
}
