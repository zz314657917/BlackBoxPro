package com.blackboxpro.neoforge.action.advanced

import com.blackboxpro.neoforge.action.ActionExecutor
import com.blackboxpro.neoforge.action.ActionResult
import com.blackboxpro.neoforge.util.*
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.network.protocol.game.ServerboundSetJigsawBlockPacket
import net.minecraft.resources.Identifier
import net.minecraft.world.level.block.entity.JigsawBlockEntity

class UpdateJigsawBlockAction : ActionExecutor {

    companion object {
        private val JOINT_MAP = mapOf(
            "rollable" to JigsawBlockEntity.JointType.ROLLABLE,
            "aligned" to JigsawBlockEntity.JointType.ALIGNED
        )
    }

    override fun execute(params: JsonObject): ActionResult {
        val x = params.requireInt("x")
        val y = params.requireInt("y")
        val z = params.requireInt("z")
        val name = params.requireString("name")
        val target = params.requireString("target")
        val pool = params.requireString("pool")
        val finalState = params.getStringOrNull("finalState") ?: ""
        val jointTypeStr = params.getStringOrNull("jointType") ?: "rollable"
        val selectionPriority = params.getIntOrDefault("selectionPriority", 0)
        val placementPriority = params.getIntOrDefault("placementPriority", 0)

        val jointType = JOINT_MAP[jointTypeStr.lowercase()]
            ?: return ActionResult.fail("Unknown joint type: $jointTypeStr (valid: ${JOINT_MAP.keys})")

        val nameId = Identifier.tryParse(name)
            ?: return ActionResult.fail("Invalid identifier for name: $name")
        val targetId = Identifier.tryParse(target)
            ?: return ActionResult.fail("Invalid identifier for target: $target")
        val poolId = Identifier.tryParse(pool)
            ?: return ActionResult.fail("Invalid identifier for pool: $pool")

        val handler = Minecraft.getInstance().connection
            ?: return ActionResult.fail("Not connected to server")

        handler.send(
            ServerboundSetJigsawBlockPacket(
                BlockPos(x, y, z),
                nameId,
                targetId,
                poolId,
                finalState,
                jointType,
                selectionPriority,
                placementPriority
            )
        )

        return ActionResult.ok("Updated jigsaw block at ($x, $y, $z)")
    }
}
