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

class UpdateStructureBlockAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val x = params.requireInt("x")
        val y = params.requireInt("y")
        val z = params.requireInt("z")
        val action = params.getIntOrDefault("action", 0)
        val mode = params.getIntOrDefault("mode", 0)
        val name = params.getStringOrNull("name") ?: ""
        val offsetX = params.getIntOrDefault("offsetX", 0)
        val offsetY = params.getIntOrDefault("offsetY", 0)
        val offsetZ = params.getIntOrDefault("offsetZ", 0)
        val sizeX = params.getIntOrDefault("sizeX", 1)
        val sizeY = params.getIntOrDefault("sizeY", 1)
        val sizeZ = params.getIntOrDefault("sizeZ", 1)
        val mirror = params.getIntOrDefault("mirror", 0)
        val rotation = params.getIntOrDefault("rotation", 0)
        val metadata = params.getStringOrNull("metadata") ?: ""
        val ignoreEntities = params.getBooleanOrDefault("ignoreEntities", false)
        val showAir = params.getBooleanOrDefault("showAir", false)
        val showBoundingBox = params.getBooleanOrDefault("showBoundingBox", true)
        val integrity = params.getFloatOrDefault("integrity", 1.0f)
        val seed = params.getLongOrDefault("seed", 0L)

        val connection = Minecraft.getMinecraft().connection
            ?: return ActionResult.fail("Not connected to server")

        val flags: Byte = ((if (ignoreEntities) 0x01 else 0) or
                (if (showAir) 0x02 else 0) or
                (if (showBoundingBox) 0x04 else 0)).toByte()

        val buf = PacketBuffer(Unpooled.buffer())
        buf.writeBlockPos(BlockPos(x, y, z))
        buf.writeByte(action)
        buf.writeByte(mode)
        buf.writeString(name)
        buf.writeByte(offsetX)
        buf.writeByte(offsetY)
        buf.writeByte(offsetZ)
        buf.writeByte(sizeX)
        buf.writeByte(sizeY)
        buf.writeByte(sizeZ)
        buf.writeByte(mirror)
        buf.writeByte(rotation)
        buf.writeString(metadata)
        buf.writeByte(flags.toInt())
        buf.writeFloat(integrity)
        buf.writeVarLong(seed)
        connection.sendPacket(CPacketCustomPayload("MC|Struct", buf))

        return ActionResult.ok("Updated structure block at ($x, $y, $z) name='$name'")
    }
}
