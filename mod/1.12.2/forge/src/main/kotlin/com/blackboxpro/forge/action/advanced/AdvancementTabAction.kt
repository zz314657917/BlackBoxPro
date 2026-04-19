package com.blackboxpro.forge.action.advanced

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.blackboxpro.forge.util.getStringOrNull
import com.blackboxpro.forge.util.requireString
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.network.play.client.CPacketSeenAdvancements
import net.minecraft.util.ResourceLocation

class AdvancementTabAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val action = params.requireString("action")

        val connection = Minecraft.getMinecraft().connection
            ?: return ActionResult.fail("Not connected to server")

        when (action.lowercase()) {
            "open", "opened_tab" -> {
                val tabId = params.getStringOrNull("tabId")
                    ?: return ActionResult.fail("Missing required field: tabId for open action")
                val resourceLocation = ResourceLocation(tabId)
                connection.sendPacket(
                    CPacketSeenAdvancements(CPacketSeenAdvancements.Action.OPENED_TAB, resourceLocation)
                )
                return ActionResult.ok("Opened advancement tab: $tabId")
            }
            "close", "closed_screen" -> {
                connection.sendPacket(
                    CPacketSeenAdvancements(CPacketSeenAdvancements.Action.CLOSED_SCREEN, null)
                )
                return ActionResult.ok("Closed advancement screen")
            }
            else -> return ActionResult.fail("Invalid action: $action (expected open/close)")
        }
    }
}
