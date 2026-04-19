package com.blackboxpro.neoforge.action.composite

import com.blackboxpro.neoforge.action.ActionExecutor
import com.blackboxpro.neoforge.action.ActionResult
import com.blackboxpro.neoforge.dispatcher.ActionRegistry
import com.google.gson.JsonObject

class RespawnAction : ActionExecutor {

    override fun execute(params: JsonObject): ActionResult {
        val respawn = ActionRegistry.find("perform_respawn")
            ?: return ActionResult.fail("perform_respawn action not registered")
        val result = respawn.execute(JsonObject())
        if (!result.success) return ActionResult.fail("Failed to respawn: ${result.message}")

        return ActionResult.ok("Respawned successfully")
    }
}
