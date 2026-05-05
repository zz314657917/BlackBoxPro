package com.blackboxpro.forge.action.advanced

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.blackboxpro.forge.util.requireInt
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.network.protocol.game.ServerboundEntityTagQuery

class QueryEntityNbtAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val transactionId = params.requireInt("transactionId")
        val entityId = params.requireInt("entityId")

        val networkHandler = Minecraft.getInstance().connection
            ?: return ActionResult.fail("Not connected to server")

        networkHandler.send(
            ServerboundEntityTagQuery(transactionId, entityId)
        )
        return ActionResult.ok("Queried NBT for entity $entityId (transaction=$transactionId)")
    }
}

