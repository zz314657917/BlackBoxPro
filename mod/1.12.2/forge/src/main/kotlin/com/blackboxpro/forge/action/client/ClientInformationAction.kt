package com.blackboxpro.forge.action.client

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.blackboxpro.forge.util.getBooleanOrDefault
import com.blackboxpro.forge.util.getIntOrDefault
import com.blackboxpro.forge.util.getStringOrNull
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.entity.player.EntityPlayer.EnumChatVisibility
import net.minecraft.network.play.client.CPacketClientSettings
import net.minecraft.util.EnumHandSide

class ClientInformationAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val lang = params.getStringOrNull("locale") ?: params.getStringOrNull("lang") ?: "en_us"
        val viewDistance = params.getIntOrDefault("viewDistance", 12)
        val chatColors = params.getBooleanOrDefault("chatColors", true)
        val skinParts = params.getIntOrDefault("skinParts", 127)

        // 支持整数（0=full,1=system,2=hidden）或字符串
        val chatModeRaw = params.get("chatMode")
        val chatVisibility = when {
            chatModeRaw == null -> EnumChatVisibility.FULL
            chatModeRaw.isJsonPrimitive && chatModeRaw.asJsonPrimitive.isNumber -> when (chatModeRaw.asInt) {
                0 -> EnumChatVisibility.FULL
                1 -> EnumChatVisibility.SYSTEM
                2 -> EnumChatVisibility.HIDDEN
                else -> return ActionResult.fail("Invalid chatMode: ${chatModeRaw.asInt} (expected 0-2)")
            }
            else -> when (chatModeRaw.asString.lowercase()) {
                "full" -> EnumChatVisibility.FULL
                "commands", "system" -> EnumChatVisibility.SYSTEM
                "hidden" -> EnumChatVisibility.HIDDEN
                else -> return ActionResult.fail("Invalid chatMode: ${chatModeRaw.asString}")
            }
        }

        // 支持整数（0=left,1=right）或字符串
        val mainHandRaw = params.get("mainHand")
        val mainHand = when {
            mainHandRaw == null -> EnumHandSide.RIGHT
            mainHandRaw.isJsonPrimitive && mainHandRaw.asJsonPrimitive.isNumber -> when (mainHandRaw.asInt) {
                0 -> EnumHandSide.LEFT
                1 -> EnumHandSide.RIGHT
                else -> return ActionResult.fail("Invalid mainHand: ${mainHandRaw.asInt} (expected 0=left, 1=right)")
            }
            else -> when (mainHandRaw.asString.lowercase()) {
                "left" -> EnumHandSide.LEFT
                "right" -> EnumHandSide.RIGHT
                else -> return ActionResult.fail("Invalid mainHand: ${mainHandRaw.asString}")
            }
        }

        val connection = Minecraft.getMinecraft().connection
            ?: return ActionResult.fail("Not connected to server")

        connection.sendPacket(
            CPacketClientSettings(lang, viewDistance, chatVisibility, chatColors, skinParts, mainHand)
        )
        return ActionResult.ok("Client information sent")
    }
}
