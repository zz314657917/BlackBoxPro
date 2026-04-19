package com.blackboxpro.forge.action.advanced

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.blackboxpro.forge.util.requireInt
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.network.PacketBuffer
import net.minecraft.network.play.client.CPacketCustomPayload
import io.netty.buffer.Unpooled

class SelectTradeAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val selectedSlot = params.requireInt("selectedSlot")
        if (selectedSlot < 0) {
            return ActionResult.fail("selectedSlot must be >= 0, got: $selectedSlot")
        }

        val connection = Minecraft.getMinecraft().connection
            ?: return ActionResult.fail("Not connected to server")

        val buf = PacketBuffer(Unpooled.buffer())
        buf.writeInt(selectedSlot)
        connection.sendPacket(CPacketCustomPayload("MC|TrSel", buf))

        return ActionResult.ok("Selected trade slot $selectedSlot")
    }
}
