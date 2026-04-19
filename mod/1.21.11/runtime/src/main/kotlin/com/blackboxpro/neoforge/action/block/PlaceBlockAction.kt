package com.blackboxpro.neoforge.action.block

import com.blackboxpro.neoforge.action.ActionExecutor
import com.blackboxpro.neoforge.action.ActionResult
import com.blackboxpro.neoforge.util.*
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.Vec3

class PlaceBlockAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val x = params.requireInt("x")
        val y = params.requireInt("y")
        val z = params.requireInt("z")
        val direction = DirectionUtil.fromString(params.requireString("face"))
        val hand = HandUtil.fromString(params.getStringOrNull("hand") ?: "main_hand")
        val cursorX = params.getDoubleOrDefault("cursorX", 0.5)
        val cursorY = params.getDoubleOrDefault("cursorY", 0.5)
        val cursorZ = params.getDoubleOrDefault("cursorZ", 0.5)
        val insideBlock = params.getBooleanOrDefault("insideBlock", false)
        val sequence = params.getIntOrDefault("sequence", 0)

        val player = Minecraft.getInstance().connection
            ?: return ActionResult.fail("Not connected to server")

        val hitResult = BlockHitResult(
            Vec3(x + cursorX, y + cursorY, z + cursorZ),
            direction,
            BlockPos(x, y, z),
            insideBlock
        )

        player.send(ServerboundUseItemOnPacket(hand, hitResult, sequence))
        return ActionResult.ok("Placed block at $x, $y, $z")
    }
}
