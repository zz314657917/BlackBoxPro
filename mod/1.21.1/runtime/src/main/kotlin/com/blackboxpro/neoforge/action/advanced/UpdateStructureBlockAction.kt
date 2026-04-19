package com.blackboxpro.neoforge.action.advanced

import com.blackboxpro.neoforge.action.ActionExecutor
import com.blackboxpro.neoforge.action.ActionResult
import com.blackboxpro.neoforge.util.*
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.core.Vec3i
import net.minecraft.network.protocol.game.ServerboundSetStructureBlockPacket
import net.minecraft.world.level.block.Mirror
import net.minecraft.world.level.block.Rotation
import net.minecraft.world.level.block.entity.StructureBlockEntity
import net.minecraft.world.level.block.state.properties.StructureMode

class UpdateStructureBlockAction : ActionExecutor {

    companion object {
        private val MODE_MAP = mapOf(
            "save" to StructureMode.SAVE,
            "load" to StructureMode.LOAD,
            "corner" to StructureMode.CORNER,
            "data" to StructureMode.DATA
        )

        private val ACTION_MAP = mapOf(
            0 to StructureBlockEntity.UpdateType.UPDATE_DATA,
            1 to StructureBlockEntity.UpdateType.SAVE_AREA,
            2 to StructureBlockEntity.UpdateType.LOAD_AREA,
            3 to StructureBlockEntity.UpdateType.SCAN_AREA
        )

        private val MIRROR_MAP = mapOf(
            "none" to Mirror.NONE,
            "left_right" to Mirror.LEFT_RIGHT,
            "front_back" to Mirror.FRONT_BACK
        )

        private val ROTATION_MAP = mapOf(
            "none" to Rotation.NONE,
            "clockwise_90" to Rotation.CLOCKWISE_90,
            "clockwise_180" to Rotation.CLOCKWISE_180,
            "counterclockwise_90" to Rotation.COUNTERCLOCKWISE_90
        )

        private const val IGNORE_ENTITIES_FLAG = 0x01
        private const val SHOW_AIR_FLAG = 0x02
        private const val SHOW_BOUNDING_BOX_FLAG = 0x04
        private const val STRICT_FLAG = 0x08
    }

    override fun execute(params: JsonObject): ActionResult {
        val x = params.requireInt("x")
        val y = params.requireInt("y")
        val z = params.requireInt("z")
        val actionInt = params.requireInt("action")
        val modeStr = params.requireString("mode")
        val name = params.requireString("name")
        val offsetX = params.getIntOrDefault("offsetX", 0)
        val offsetY = params.getIntOrDefault("offsetY", 0)
        val offsetZ = params.getIntOrDefault("offsetZ", 0)
        val sizeX = params.getIntOrDefault("sizeX", 0)
        val sizeY = params.getIntOrDefault("sizeY", 0)
        val sizeZ = params.getIntOrDefault("sizeZ", 0)
        val mirrorStr = params.getStringOrNull("mirror") ?: "none"
        val rotationStr = params.getStringOrNull("rotation") ?: "none"
        val metadata = params.getStringOrNull("metadata") ?: ""
        val integrity = params.getFloatOrDefault("integrity", 1.0f)
        val seed = params.getLongOrDefault("seed", 0L)
        val flags = params.getIntOrDefault("flags", 0)

        val action = ACTION_MAP[actionInt]
            ?: return ActionResult.fail("Unknown structure block action: $actionInt (valid: 0-3)")
        val mode = MODE_MAP[modeStr.lowercase()]
            ?: return ActionResult.fail("Unknown structure block mode: $modeStr (valid: ${MODE_MAP.keys})")
        val mirror = MIRROR_MAP[mirrorStr.lowercase()]
            ?: return ActionResult.fail("Unknown mirror: $mirrorStr (valid: ${MIRROR_MAP.keys})")
        val rotation = ROTATION_MAP[rotationStr.lowercase()]
            ?: return ActionResult.fail("Unknown rotation: $rotationStr (valid: ${ROTATION_MAP.keys})")

        val ignoreEntities = (flags and IGNORE_ENTITIES_FLAG) != 0
        val showAir = (flags and SHOW_AIR_FLAG) != 0
        val showBoundingBox = (flags and SHOW_BOUNDING_BOX_FLAG) != 0
        val strict = (flags and STRICT_FLAG) != 0

        val handler = Minecraft.getInstance().connection
            ?: return ActionResult.fail("Not connected to server")

        handler.send(
            ServerboundSetStructureBlockPacket(
                BlockPos(x, y, z),
                action,
                mode,
                name,
                BlockPos(offsetX, offsetY, offsetZ),
                Vec3i(sizeX, sizeY, sizeZ),
                mirror,
                rotation,
                metadata,
                ignoreEntities,
                showAir,
                showBoundingBox,
                integrity,
                seed
            )
        )

        return ActionResult.ok("Updated structure block at ($x, $y, $z)")
    }
}
