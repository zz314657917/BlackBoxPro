package com.blackboxpro.forge.action.advanced

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.blackboxpro.forge.util.getBooleanOrDefault
import com.blackboxpro.forge.util.getIntOrDefault
import com.blackboxpro.forge.util.requireInt
import com.blackboxpro.forge.util.requireString
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.network.protocol.game.ServerboundSetCommandBlockPacket
import net.minecraft.world.level.block.entity.CommandBlockEntity

class UpdateCommandBlockAction : ActionExecutor {

    companion object {
        private val TYPE_MAP = mapOf(
            0 to CommandBlockEntity.Mode.SEQUENCE,
            1 to CommandBlockEntity.Mode.AUTO,
            2 to CommandBlockEntity.Mode.REDSTONE
        )
    }

    override fun execute(params: JsonObject): ActionResult {
        val x = params.requireInt("x")
        val y = params.requireInt("y")
        val z = params.requireInt("z")
        val command = params.requireString("command")
        val mode = params.getIntOrDefault("mode", 2)
        val trackOutput = params.getBooleanOrDefault("trackOutput", true)
        val conditional = params.getBooleanOrDefault("conditional", false)
        val alwaysActive = params.getBooleanOrDefault("alwaysActive", false)

        val type = TYPE_MAP[mode]
            ?: return ActionResult.fail("Invalid command block mode: $mode (valid: 0=sequence, 1=auto, 2=redstone)")

        val networkHandler = Minecraft.getInstance().connection
            ?: return ActionResult.fail("Not connected to server")

        networkHandler.send(
            ServerboundSetCommandBlockPacket(
                BlockPos(x, y, z),
                command,
                type,
                trackOutput,
                conditional,
                alwaysActive
            )
        )
        return ActionResult.ok("Updated command block at $x, $y, $z")
    }
}

