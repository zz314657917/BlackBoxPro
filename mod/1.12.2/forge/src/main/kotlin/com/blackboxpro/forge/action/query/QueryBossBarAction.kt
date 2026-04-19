package com.blackboxpro.forge.action.query

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.world.BossInfo
import org.tabooproject.reflex.Reflex.Companion.getProperty

class QueryBossBarAction : ActionExecutor {

    override fun execute(params: JsonObject): ActionResult {
        val mc = Minecraft.getMinecraft()
        val bossOverlay = mc.ingameGUI.bossOverlay

        val bars = JsonArray()

        try {
            @Suppress("UNCHECKED_CAST")
            val bossInfos = bossOverlay.getProperty<Map<*, *>>("mapBossInfos") ?: emptyMap<Any, Any>()

            bossInfos.values.forEach { bar ->
                if (bar is BossInfo) {
                    bars.add(JsonObject().apply {
                        addProperty("name", bar.name.unformattedText)
                        addProperty("percent", bar.percent)
                        addProperty("color", bar.color.name.lowercase())
                        addProperty("style", bar.overlay.name.lowercase())
                        addProperty("darkenSky", bar.shouldDarkenSky())
                        addProperty("playMusic", bar.shouldPlayEndBossMusic())
                        addProperty("createFog", bar.shouldCreateFog())
                    })
                }
            }
        } catch (e: Exception) {
            // 反射失败时返回空列表，记录日志
            org.apache.logging.log4j.LogManager.getLogger("BlackBoxPro").warn("QueryBossBarAction reflection failed: {}", e.message)
        }

        val data = JsonObject().apply {
            add("bossBars", bars)
            addProperty("count", bars.size())
        }

        return ActionResult.ok("Boss bars queried", data)
    }
}
