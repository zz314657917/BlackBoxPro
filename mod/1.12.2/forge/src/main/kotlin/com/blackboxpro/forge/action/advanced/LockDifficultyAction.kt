package com.blackboxpro.forge.action.advanced

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.blackboxpro.forge.util.requireBoolean
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.network.PacketBuffer
import net.minecraft.network.play.client.CPacketCustomPayload
import io.netty.buffer.Unpooled

class LockDifficultyAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val locked = params.requireBoolean("locked")

        val connection = Minecraft.getMinecraft().connection
            ?: return ActionResult.fail("Not connected to server")

        val buf = PacketBuffer(Unpooled.buffer())
        buf.writeByte(if (locked) 1 else 0)
        connection.sendPacket(CPacketCustomPayload("MC|Lock", buf))

        return ActionResult.ok("Difficulty lock set to $locked")
    }
}
