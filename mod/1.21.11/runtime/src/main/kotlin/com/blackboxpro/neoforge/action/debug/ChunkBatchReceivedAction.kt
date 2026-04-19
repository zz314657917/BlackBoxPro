package com.blackboxpro.neoforge.action.debug

import com.blackboxpro.neoforge.action.ActionExecutor
import com.blackboxpro.neoforge.action.ActionResult
import com.blackboxpro.neoforge.util.getFloatOrDefault
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.network.protocol.game.ServerboundChunkBatchReceivedPacket

class ChunkBatchReceivedAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val desiredChunksPerTick = params.getFloatOrDefault("desiredChunksPerTick", 7.0f)

        val networkHandler = Minecraft.getInstance().connection
            ?: return ActionResult.fail("Not connected to server")

        networkHandler.send(ServerboundChunkBatchReceivedPacket(desiredChunksPerTick))
        return ActionResult.ok("Acknowledged chunk batch (desiredChunksPerTick=$desiredChunksPerTick)")
    }
}
