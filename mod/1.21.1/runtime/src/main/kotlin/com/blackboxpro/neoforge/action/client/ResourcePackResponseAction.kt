package com.blackboxpro.neoforge.action.client

import com.blackboxpro.neoforge.action.ActionExecutor
import com.blackboxpro.neoforge.action.ActionResult
import com.blackboxpro.neoforge.util.requireString
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.network.protocol.common.ServerboundResourcePackPacket
import java.util.UUID

class ResourcePackResponseAction : ActionExecutor {

    companion object {
        private val STATUS_MAP = mapOf(
            "accepted" to ServerboundResourcePackPacket.Action.ACCEPTED,
            "declined" to ServerboundResourcePackPacket.Action.DECLINED,
            "successfully_loaded" to ServerboundResourcePackPacket.Action.SUCCESSFULLY_LOADED,
            "failed_download" to ServerboundResourcePackPacket.Action.FAILED_DOWNLOAD,
            "failed_reload" to ServerboundResourcePackPacket.Action.FAILED_RELOAD,
            "discarded" to ServerboundResourcePackPacket.Action.DISCARDED,
            "invalid_url" to ServerboundResourcePackPacket.Action.INVALID_URL,
            "downloaded" to ServerboundResourcePackPacket.Action.DOWNLOADED,
        )
    }

    override fun execute(params: JsonObject): ActionResult {
        val uuidStr = params.requireString("uuid")
        val resultStr = params.requireString("result")

        val uuid = try {
            UUID.fromString(uuidStr)
        } catch (e: IllegalArgumentException) {
            return ActionResult.fail("Invalid UUID: $uuidStr")
        }

        val status = STATUS_MAP[resultStr.lowercase()]
            ?: return ActionResult.fail("Unknown result: $resultStr. Valid: ${STATUS_MAP.keys}")

        val networkHandler = Minecraft.getInstance().connection
            ?: return ActionResult.fail("Not connected to server")

        networkHandler.send(ServerboundResourcePackPacket(uuid, status))
        return ActionResult.ok("Sent resource pack response: $resultStr")
    }
}
