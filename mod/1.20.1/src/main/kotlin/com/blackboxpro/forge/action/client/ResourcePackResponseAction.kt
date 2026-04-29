package com.blackboxpro.forge.action.client

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.blackboxpro.forge.util.requireString
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.network.protocol.game.ServerboundResourcePackPacket

class ResourcePackResponseAction : ActionExecutor {

    companion object {
        private val STATUS_MAP = mapOf(
            "accepted" to ServerboundResourcePackPacket.Action.ACCEPTED,
            "declined" to ServerboundResourcePackPacket.Action.DECLINED,
            "successfully_loaded" to ServerboundResourcePackPacket.Action.SUCCESSFULLY_LOADED,
            "failed_download" to ServerboundResourcePackPacket.Action.FAILED_DOWNLOAD,
        )
    }

    override fun execute(params: JsonObject): ActionResult {
        params.requireString("uuid") // 保持协议兼容，1.20.1 发包时不再携带 uuid。
        val resultStr = params.requireString("result")

        val status = STATUS_MAP[resultStr.lowercase()]
            ?: return ActionResult.fail("Unknown result: $resultStr. Valid: ${STATUS_MAP.keys}")

        val networkHandler = Minecraft.getInstance().connection
            ?: return ActionResult.fail("Not connected to server")

        networkHandler.send(ServerboundResourcePackPacket(status))
        return ActionResult.ok("Sent resource pack response: $resultStr")
    }
}
