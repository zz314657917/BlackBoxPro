package com.blackboxpro.neoforge.action.advanced

import com.blackboxpro.neoforge.action.ActionExecutor
import com.blackboxpro.neoforge.action.ActionResult
import com.blackboxpro.neoforge.util.getStringOrNull
import com.blackboxpro.neoforge.util.requireString
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.network.protocol.game.ServerboundSeenAdvancementsPacket
import net.minecraft.resources.Identifier

class AdvancementTabAction : ActionExecutor {

    override fun execute(params: JsonObject): ActionResult {
        val actionStr = params.requireString("action")

        val networkHandler = Minecraft.getInstance().connection
            ?: return ActionResult.fail("Not connected to server")

        when (actionStr.lowercase()) {
            "open" -> {
                val tabId = params.getStringOrNull("tabId")
                    ?: return ActionResult.fail("Missing required field: tabId (for open action)")
                val resourceLocation = Identifier.tryParse(tabId)
                    ?: return ActionResult.fail("Invalid resource location: $tabId")
                val advancement = networkHandler.advancements.get(resourceLocation)
                    ?: return ActionResult.fail("Advancement not found: $tabId")
                networkHandler.send(ServerboundSeenAdvancementsPacket.openedTab(advancement))
                return ActionResult.ok("Opened advancement tab: $tabId")
            }
            "close" -> {
                networkHandler.send(ServerboundSeenAdvancementsPacket.closedScreen())
                return ActionResult.ok("Closed advancement tab")
            }
            else -> return ActionResult.fail("Unknown action: $actionStr (valid: open, close)")
        }
    }
}
