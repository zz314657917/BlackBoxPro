package com.blackboxpro.forge.action.container

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.blackboxpro.forge.util.getBooleanOrDefault
import com.blackboxpro.forge.util.requireInt
import com.blackboxpro.forge.util.PickItemSupport
import com.google.gson.JsonObject
import net.minecraft.core.BlockPos

class PickItemFromBlockAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val x = params.requireInt("x")
        val y = params.requireInt("y")
        val z = params.requireInt("z")
        val includeData = params.getBooleanOrDefault("includeData", false)
        return PickItemSupport.pickBlock(
            BlockPos(x, y, z),
            includeData,
            "Picked item from block at ($x, $y, $z)"
        )
    }
}
