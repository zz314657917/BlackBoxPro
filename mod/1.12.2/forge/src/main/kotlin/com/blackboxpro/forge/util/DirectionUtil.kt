package com.blackboxpro.forge.util

import net.minecraft.util.EnumFacing

object DirectionUtil {
    private val map = mapOf(
        "down" to EnumFacing.DOWN,
        "up" to EnumFacing.UP,
        "north" to EnumFacing.NORTH,
        "south" to EnumFacing.SOUTH,
        "west" to EnumFacing.WEST,
        "east" to EnumFacing.EAST,
        "bottom" to EnumFacing.DOWN,
        "top" to EnumFacing.UP
    )

    fun fromString(face: String): EnumFacing =
        map[face.lowercase()] ?: throw IllegalArgumentException("Unknown face: $face")
}
