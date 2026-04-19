package com.blackboxpro.forge.action.advanced

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.blackboxpro.forge.util.getBooleanOrDefault
import com.blackboxpro.forge.util.requireInt
import com.blackboxpro.forge.util.requireString
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.network.PacketBuffer
import net.minecraft.network.play.client.CPacketCustomPayload
import io.netty.buffer.Unpooled

class UpdateCommandBlockMinecartAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val entityId = params.requireInt("entityId")
        val command = params.requireString("command")
        val trackOutput = params.getBooleanOrDefault("trackOutput", true)

        val connection = Minecraft.getMinecraft().connection
            ?: return ActionResult.fail("Not connected to server")

        val buf = PacketBuffer(Unpooled.buffer())
        buf.writeByte(1) // type = 1 for minecart command block
        buf.writeInt(entityId)
        buf.writeString(command)
        buf.writeBoolean(trackOutput)
        connection.sendPacket(CPacketCustomPayload("MC|AdvCmd", buf))

        return ActionResult.ok("Updated command block minecart entity=$entityId")
    }
}
