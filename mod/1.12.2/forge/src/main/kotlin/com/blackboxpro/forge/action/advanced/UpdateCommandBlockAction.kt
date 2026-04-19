package com.blackboxpro.forge.action.advanced

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.blackboxpro.forge.util.*
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.network.PacketBuffer
import net.minecraft.network.play.client.CPacketCustomPayload
import net.minecraft.util.math.BlockPos
import io.netty.buffer.Unpooled

class UpdateCommandBlockAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val x = params.requireInt("x")
        val y = params.requireInt("y")
        val z = params.requireInt("z")
        val command = params.requireString("command")
        val trackOutput = params.getBooleanOrDefault("trackOutput", true)
        val mode = params.getStringOrNull("mode") ?: "REDSTONE"
        val conditional = params.getBooleanOrDefault("conditional", false)
        val alwaysActive = params.getBooleanOrDefault("alwaysActive", false)

        val connection = Minecraft.getMinecraft().connection
            ?: return ActionResult.fail("Not connected to server")

        val modeFlag: Byte = when (mode.uppercase()) {
            "SEQUENCE" -> 0
            "AUTO" -> 1
            "REDSTONE" -> 2
            else -> return ActionResult.fail("Invalid mode: $mode (expected SEQUENCE, AUTO, REDSTONE)")
        }

        val flags: Byte = ((if (trackOutput) 0x01 else 0) or
                (if (conditional) 0x02 else 0) or
                (if (alwaysActive) 0x04 else 0)).toByte()

        val buf = PacketBuffer(Unpooled.buffer())
        buf.writeBlockPos(BlockPos(x, y, z))
        buf.writeString(command)
        buf.writeByte(modeFlag.toInt())
        buf.writeByte(flags.toInt())
        connection.sendPacket(CPacketCustomPayload("MC|AutoCmd", buf))

        return ActionResult.ok("Updated command block at ($x, $y, $z) mode=$mode")
    }
}
