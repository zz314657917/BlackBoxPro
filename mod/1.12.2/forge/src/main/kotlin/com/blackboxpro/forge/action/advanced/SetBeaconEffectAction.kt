package com.blackboxpro.forge.action.advanced

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.blackboxpro.forge.util.getIntOrDefault
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.network.PacketBuffer
import net.minecraft.network.play.client.CPacketCustomPayload
import io.netty.buffer.Unpooled

class SetBeaconEffectAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val primaryEffect = params.getIntOrDefault("primaryEffect", -1)
        val secondaryEffect = params.getIntOrDefault("secondaryEffect", -1)

        val connection = Minecraft.getMinecraft().connection
            ?: return ActionResult.fail("Not connected to server")

        val buf = PacketBuffer(Unpooled.buffer())
        buf.writeInt(primaryEffect)
        buf.writeInt(secondaryEffect)
        connection.sendPacket(CPacketCustomPayload("MC|Beacon", buf))

        return ActionResult.ok("Set beacon effects: primary=$primaryEffect, secondary=$secondaryEffect")
    }
}
