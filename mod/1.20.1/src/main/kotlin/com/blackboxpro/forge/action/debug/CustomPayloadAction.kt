package com.blackboxpro.forge.action.debug

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.blackboxpro.forge.util.requireString
import com.google.gson.JsonObject
import io.netty.buffer.Unpooled
import net.minecraft.client.Minecraft
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.protocol.game.ServerboundCustomPayloadPacket
import net.minecraft.resources.ResourceLocation
import org.slf4j.LoggerFactory
import java.util.Base64

class CustomPayloadAction : ActionExecutor {
    companion object {
        private val logger = LoggerFactory.getLogger("BlackBoxPro-CustomPayload")
    }

    override fun execute(params: JsonObject): ActionResult {
        val channel = params.requireString("channel")
        val dataBase64 = params.requireString("data")

        val handler = Minecraft.getInstance().connection
            ?: return ActionResult.fail("Not connected to server")

        val data = try {
            Base64.getDecoder().decode(dataBase64)
        } catch (e: IllegalArgumentException) {
            return ActionResult.fail("Invalid base64 data: ${e.message}")
        }

        if (data.size > 32768) {
            return ActionResult.fail("Payload too large: ${data.size} > 32768")
        }

        val identifier = ResourceLocation.tryParse(channel)
            ?: return ActionResult.fail("Invalid channel identifier: $channel")
        val payload = FriendlyByteBuf(Unpooled.wrappedBuffer(data))

        return try {
            handler.send(ServerboundCustomPayloadPacket(identifier, payload))
            ActionResult.ok("Sent custom payload to $channel (${data.size} bytes)")
        } catch (e: Exception) {
            logger.warn("Failed to send custom payload to {}: {}", channel, e.message)
            ActionResult.fail("Failed to send custom payload: ${e.message}")
        }
    }
}
