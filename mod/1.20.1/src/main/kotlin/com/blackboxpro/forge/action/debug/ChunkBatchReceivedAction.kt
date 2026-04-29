package com.blackboxpro.forge.action.debug

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.blackboxpro.forge.util.getFloatOrDefault
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft

class ChunkBatchReceivedAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val desiredChunksPerTick = params.getFloatOrDefault("desiredChunksPerTick", 7.0f)

        Minecraft.getInstance().connection
            ?: return ActionResult.fail("Not connected to server")

        return ActionResult.ok(
            "Chunk batch acknowledgement has no dedicated Forge 1.20.1 protocol packet; treated as acknowledged (desiredChunksPerTick=$desiredChunksPerTick)"
        )
    }
}
