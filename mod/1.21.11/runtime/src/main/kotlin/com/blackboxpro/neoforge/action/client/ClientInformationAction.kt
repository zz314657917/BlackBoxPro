package com.blackboxpro.neoforge.action.client

import com.blackboxpro.neoforge.action.ActionExecutor
import com.blackboxpro.neoforge.action.ActionResult
import com.blackboxpro.neoforge.util.getBooleanOrDefault
import com.blackboxpro.neoforge.util.getIntOrDefault
import com.blackboxpro.neoforge.util.getStringOrNull
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.server.level.ClientInformation
import net.minecraft.network.protocol.common.ServerboundClientInformationPacket

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

        // 显式映射 mainHand，不依赖 enum ordinal
        val arm = when (mainHand) {
            0 -> net.minecraft.world.entity.HumanoidArm.LEFT
            1 -> net.minecraft.world.entity.HumanoidArm.RIGHT
            else -> return ActionResult.fail("Invalid mainHand: $mainHand (expected 0=left, 1=right)")
        }

        val chatVisibility = when (chatMode) {
            0 -> net.minecraft.world.entity.player.ChatVisiblity.FULL
            1 -> net.minecraft.world.entity.player.ChatVisiblity.SYSTEM
            2 -> net.minecraft.world.entity.player.ChatVisiblity.HIDDEN
            else -> return ActionResult.fail("Invalid chatMode: $chatMode (expected 0-2)")
        }

        val syncedOptions = ClientInformation(
            locale,
            viewDistance,
            chatVisibility,
            chatColors,
            skinParts,
            arm,
            textFiltering,
            allowServerListings,
            net.minecraft.server.level.ParticleStatus.ALL
        )
        networkHandler.send(ServerboundClientInformationPacket(syncedOptions))

        return ActionResult.ok("Client information sent")
    }
}
