package com.blackboxpro.forge.action.client

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.blackboxpro.forge.util.getBooleanOrDefault
import com.blackboxpro.forge.util.getIntOrDefault
import com.blackboxpro.forge.util.getStringOrNull
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.network.protocol.game.ServerboundClientInformationPacket
import net.minecraft.world.entity.HumanoidArm
import net.minecraft.world.entity.player.ChatVisiblity

class ClientInformationAction : ActionExecutor {

    override fun execute(params: JsonObject): ActionResult {
        val locale = params.getStringOrNull("locale") ?: "en_us"
        val viewDistance = params.getIntOrDefault("viewDistance", 12)
        val chatMode = params.getIntOrDefault("chatMode", 0)
        val chatColors = params.getBooleanOrDefault("chatColors", true)
        val skinParts = params.getIntOrDefault("skinParts", 127)
        val mainHand = params.getIntOrDefault("mainHand", 1)
        val textFiltering = params.getBooleanOrDefault("textFiltering", false)
        val allowServerListings = params.getBooleanOrDefault("allowServerListings", true)

        val client = Minecraft.getInstance()
        val networkHandler = client.connection
            ?: return ActionResult.fail("Not connected to server")

        val arm = when (mainHand) {
            0 -> HumanoidArm.LEFT
            1 -> HumanoidArm.RIGHT
            else -> return ActionResult.fail("Invalid mainHand: $mainHand (expected 0=left, 1=right)")
        }

        val chatVisibility = when (chatMode) {
            0 -> ChatVisiblity.FULL
            1 -> ChatVisiblity.SYSTEM
            2 -> ChatVisiblity.HIDDEN
            else -> return ActionResult.fail("Invalid chatMode: $chatMode (expected 0-2)")
        }

        networkHandler.send(
            ServerboundClientInformationPacket(
                locale,
                viewDistance,
                chatVisibility,
                chatColors,
                skinParts,
                arm,
                textFiltering,
                allowServerListings
            )
        )

        return ActionResult.ok("Client information sent")
    }
}
