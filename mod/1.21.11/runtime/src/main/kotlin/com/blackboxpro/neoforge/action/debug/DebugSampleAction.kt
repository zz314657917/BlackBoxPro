package com.blackboxpro.neoforge.action.debug

import com.blackboxpro.neoforge.action.ActionExecutor
import com.blackboxpro.neoforge.action.ActionResult
import com.blackboxpro.neoforge.util.requireString
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.network.protocol.game.ServerboundDebugSubscriptionRequestPacket
import net.minecraft.util.debug.DebugSubscription
import net.minecraft.util.debug.DebugSubscriptions
import org.slf4j.LoggerFactory
import org.tabooproject.reflex.Reflex.Companion.getProperty

class DebugSampleAction : ActionExecutor {

    companion object {
        private val logger = LoggerFactory.getLogger("BlackBoxPro-DebugSample")

        private val TYPE_MAP: Map<String, DebugSubscription<*>> by lazy {
            buildMap {
                DebugSubscriptions::class.java.declaredFields.forEach { field ->
                    if (DebugSubscription::class.java.isAssignableFrom(field.type)) {
                        try {
                            val type = DebugSubscriptions::class.java.getProperty<DebugSubscription<*>>(field.name, isStatic = true)
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

        val handler = Minecraft.getInstance().connection
            ?: return ActionResult.fail("Not connected to server")

        handler.send(ServerboundDebugSubscriptionRequestPacket(setOf(type)))
        return ActionResult.ok("Subscribed to debug sample: $typeStr")
    }
}
