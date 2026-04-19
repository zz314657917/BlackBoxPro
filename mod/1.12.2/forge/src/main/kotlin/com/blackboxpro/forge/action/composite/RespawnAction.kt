package com.blackboxpro.forge.action.composite

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.blackboxpro.forge.dispatcher.ActionRegistry
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
