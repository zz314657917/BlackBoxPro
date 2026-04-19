package com.blackboxpro.forge.action.advanced

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.blackboxpro.forge.util.requireString
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.network.PacketBuffer
import net.minecraft.network.play.client.CPacketCustomPayload
import io.netty.buffer.Unpooled

class RenameItemAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val name = params.requireString("name")
        if (name.length > 50) {
            return ActionResult.fail("Name too long: ${name.length} > 50")
        }

        val connection = Minecraft.getMinecraft().connection
            ?: return ActionResult.fail("Not connected to server")

        val buf = PacketBuffer(Unpooled.buffer())
        buf.writeString(name)
        connection.sendPacket(CPacketCustomPayload("MC|ItemName", buf))

        return ActionResult.ok("Renamed item to '$name'")
    }
}
