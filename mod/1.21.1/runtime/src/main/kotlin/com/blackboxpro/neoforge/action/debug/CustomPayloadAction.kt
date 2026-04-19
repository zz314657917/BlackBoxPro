package com.blackboxpro.neoforge.action.debug

import com.blackboxpro.neoforge.action.ActionExecutor
import com.blackboxpro.neoforge.action.ActionResult
import com.blackboxpro.neoforge.util.requireString
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation
import org.slf4j.LoggerFactory
import java.util.Base64

class CustomPayloadAction : ActionExecutor {

    companion object {
        private val logger = LoggerFactory.getLogger("BlackBoxPro-CustomPayload")
        private val registeredChannels = HashMap<String, CustomPacketPayload.Type<DynamicPayload>>()
    }

    private class DynamicPayload(
        val payloadType: CustomPacketPayload.Type<DynamicPayload>,
        val data: ByteArray
    ) : CustomPacketPayload {
        override fun type(): CustomPacketPayload.Type<DynamicPayload> = payloadType
    }

    override fun execute(params: JsonObject): ActionResult {
        val channel = params.requireString("channel")
        val dataBase64 = params.requireString("data")

        val client = Minecraft.getInstance()
        val handler = client.connection
            ?: return ActionResult.fail("Not connected to server")

        val data = try {
            Base64.getDecoder().decode(dataBase64)
        } catch (e: IllegalArgumentException) {
            return ActionResult.fail("Invalid base64 data: ${e.message}")
        }

        val maxSize = 32768
        if (data.size > maxSize) {
            return ActionResult.fail("Payload too large: ${data.size} > $maxSize")
        }

        val identifier = ResourceLocation.tryParse(channel)
            ?: return ActionResult.fail("Invalid channel identifier: $channel")

        val payloadType = registeredChannels.getOrPut(channel) {
            val type = CustomPacketPayload.Type<DynamicPayload>(identifier)
            logger.debug("Created dynamic payload type: {}", channel)
            type
        }

        val payload = DynamicPayload(payloadType, data)

        return try {
            handler.send(ServerboundCustomPayloadPacket(payload))
            ActionResult.ok("Sent custom payload to $channel (${data.size} bytes)")
        } catch (e: Exception) {
            logger.warn("Failed to send custom payload to {}: {}", channel, e.message)
            ActionResult.fail("Failed to send custom payload: ${e.message}")
        }
    }
}
