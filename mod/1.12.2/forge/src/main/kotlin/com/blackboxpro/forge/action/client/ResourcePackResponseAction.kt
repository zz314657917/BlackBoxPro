package com.blackboxpro.forge.action.client

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.blackboxpro.forge.util.requireString
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.network.play.client.CPacketResourcePackStatus

class ResourcePackResponseAction : ActionExecutor {

    companion object {
        private val STATUS_MAP = mapOf(
            "accepted" to CPacketResourcePackStatus.Action.ACCEPTED,
            "declined" to CPacketResourcePackStatus.Action.DECLINED,
            "successfully_loaded" to CPacketResourcePackStatus.Action.SUCCESSFULLY_LOADED,
            "failed_download" to CPacketResourcePackStatus.Action.FAILED_DOWNLOAD
        )
    }

    override fun execute(params: JsonObject): ActionResult {
        val resultStr = params.requireString("result")

        val status = STATUS_MAP[resultStr.lowercase()]
            ?: return ActionResult.fail("Unknown result: $resultStr. Valid: ${STATUS_MAP.keys}")

        val connection = Minecraft.getMinecraft().connection
            ?: return ActionResult.fail("Not connected to server")

        connection.sendPacket(CPacketResourcePackStatus(status))
        return ActionResult.ok("Sent resource pack response: $resultStr")
    }
}
