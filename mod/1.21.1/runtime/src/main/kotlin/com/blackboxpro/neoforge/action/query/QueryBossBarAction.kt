package com.blackboxpro.neoforge.action.query

import com.blackboxpro.neoforge.action.ActionExecutor
import com.blackboxpro.neoforge.action.ActionResult
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.world.BossEvent
import org.tabooproject.reflex.Reflex.Companion.getProperty

/**
 * 查询当前活跃的 Boss Bar 信息。
 * Action ID: "query_boss_bar"
 */
class QueryBossBarAction : ActionExecutor {

    override fun execute(params: JsonObject): ActionResult {
        val client = Minecraft.getInstance()
        val bossOverlay = client.gui.bossOverlay

        val bars = JsonArray()

        try {
            // BossHealthOverlay.events 是 Map<UUID, LerpingBossEvent>
            val events = bossOverlay.getProperty<Map<*, *>>("events") ?: emptyMap<Any, Any>()

            events.values.forEach { bar ->
                if (bar is BossEvent) {
                    bars.add(JsonObject().apply {
                        addProperty("name", bar.name.string)
                        addProperty("percent", bar.progress)
                        addProperty("color", bar.color.name.lowercase())
                        addProperty("style", bar.overlay.name.lowercase())
                        addProperty("darkenSky", bar.shouldDarkenScreen())
                        addProperty("playMusic", bar.shouldPlayBossMusic())
                        addProperty("createFog", bar.shouldCreateWorldFog())
                    })
                }
            }
        } catch (_: Exception) {
            // 反射失败时返回空列表
        }

        val data = JsonObject().apply {
            add("bossBars", bars)
            addProperty("count", bars.size())
        }

        return ActionResult.ok("Boss bars queried", data)
    }
}
