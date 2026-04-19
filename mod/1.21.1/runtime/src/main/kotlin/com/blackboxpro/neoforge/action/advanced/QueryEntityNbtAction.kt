package com.blackboxpro.neoforge.action.advanced

import com.blackboxpro.neoforge.action.ActionExecutor
import com.blackboxpro.neoforge.action.ActionResult
import com.blackboxpro.neoforge.util.requireInt
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.network.protocol.game.ServerboundEntityTagQueryPacket

class QueryEntityNbtAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val transactionId = params.requireInt("transactionId")
        val entityId = params.requireInt("entityId")

        val networkHandler = Minecraft.getInstance().connection
            ?: return ActionResult.fail("Not connected to server")

        networkHandler.send(
            ServerboundEntityTagQueryPacket(transactionId, entityId)
        )
        return ActionResult.ok("Queried NBT for entity $entityId (transaction=$transactionId)")
    }
}
