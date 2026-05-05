package com.blackboxpro.forge.action.advanced

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.blackboxpro.forge.util.getStringOrNull
import com.blackboxpro.forge.util.requireString
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.network.protocol.game.ServerboundSeenAdvancementsPacket
import net.minecraft.resources.ResourceLocation

class AdvancementTabAction : ActionExecutor {

    override fun execute(params: JsonObject): ActionResult {
        val actionStr = params.requireString("action")

        val networkHandler = Minecraft.getInstance().connection
            ?: return ActionResult.fail("Not connected to server")

        return when (actionStr.lowercase()) {
            "open" -> {
                val tabId = params.getStringOrNull("tabId")
                    ?: return ActionResult.fail("Missing required field: tabId (for open action)")
                val resourceLocation = ResourceLocation.tryParse(tabId)
                    ?: return ActionResult.fail("Invalid resource location: $tabId")
                networkHandler.send(
                    ServerboundSeenAdvancementsPacket(
                        ServerboundSeenAdvancementsPacket.Action.OPENED_TAB,
                        resourceLocation
                    )
                )
                ActionResult.ok("Opened advancement tab: $tabId")
            }

            "close" -> {
                networkHandler.send(ServerboundSeenAdvancementsPacket.closedScreen())
                ActionResult.ok("Closed advancement tab")
            }

            else -> ActionResult.fail("Unknown action: $actionStr (valid: open, close)")
        }
    }
}
