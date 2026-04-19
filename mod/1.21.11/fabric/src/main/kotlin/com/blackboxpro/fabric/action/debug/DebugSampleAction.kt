package com.blackboxpro.fabric.action.debug

import com.blackboxpro.fabric.action.ActionExecutor
import com.blackboxpro.fabric.action.ActionResult
import com.blackboxpro.fabric.util.requireString
import com.google.gson.JsonObject
import net.minecraft.client.MinecraftClient
import net.minecraft.network.packet.c2s.play.DebugSubscriptionRequestC2SPacket
import net.minecraft.world.debug.DebugSubscriptionType
import net.minecraft.world.debug.DebugSubscriptionTypes
import org.slf4j.LoggerFactory
import org.tabooproject.reflex.Reflex.Companion.getProperty

class DebugSampleAction : ActionExecutor {

    companion object {
        private val logger = LoggerFactory.getLogger("BlackBoxPro-DebugSample")

        /**
         * 字符串 → DebugSubscriptionType 映射。
         * 通过反射遍历 DebugSubscriptionTypes 的静态字段获取所有已知类型。
         * Fabric Loom remapping 保证 dev/production 环境下字段名一致（Yarn 名称）。
         */
        private val TYPE_MAP: Map<String, DebugSubscriptionType<*>> by lazy {
            buildMap {
                DebugSubscriptionTypes::class.java.declaredFields.forEach { field ->
                    if (DebugSubscriptionType::class.java.isAssignableFrom(field.type)) {
                        try {
                            val type = DebugSubscriptionTypes::class.java.getProperty<DebugSubscriptionType<*>>(field.name, isStatic = true)
                            if (type != null) {
                                put(field.name.lowercase(), type)
                            }
                        } catch (_: Exception) {
                            // 忽略不可访问的字段
                        }
                    }
                }
            }.also { map ->
                logger.debug("Loaded {} debug subscription types: {}", map.size, map.keys)
            }
        }
    }

    override fun execute(params: JsonObject): ActionResult {
        val typeStr = params.requireString("type")

        val type = TYPE_MAP[typeStr.lowercase()]
            ?: return ActionResult.fail("Unknown debug subscription type: $typeStr (valid: ${TYPE_MAP.keys})")

        val handler = MinecraftClient.getInstance().networkHandler
            ?: return ActionResult.fail("Not connected to server")

        handler.sendPacket(DebugSubscriptionRequestC2SPacket(setOf(type)))
        return ActionResult.ok("Subscribed to debug sample: $typeStr")
    }
}
