package com.blackboxpro.forge.action.debug

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.blackboxpro.forge.util.requireString
import com.google.gson.JsonObject
import io.netty.buffer.Unpooled
import net.minecraft.client.Minecraft
import net.minecraft.network.PacketBuffer
import net.minecraft.network.play.client.CPacketCustomPayload
import java.util.Base64

class CustomPayloadAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val channel = params.requireString("channel")
        val dataBase64 = params.requireString("data")

        val connection = Minecraft.getMinecraft().connection
            ?: return ActionResult.fail("Not connected to server")

        val data = try {
            Base64.getDecoder().decode(dataBase64)
        } catch (e: IllegalArgumentException) {
            return ActionResult.fail("Invalid base64 data: ${e.message}")
        }

        val maxSize = 32767
        if (data.size > maxSize) {
            return ActionResult.fail("Payload too large: ${data.size} > $maxSize")
        }

        val buf = PacketBuffer(Unpooled.wrappedBuffer(data))
        connection.sendPacket(CPacketCustomPayload(channel, buf))

        return ActionResult.ok("Sent custom payload to $channel (${data.size} bytes)")
    }
}
