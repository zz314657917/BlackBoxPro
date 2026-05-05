package com.blackboxpro.forge.action.player

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.blackboxpro.forge.util.getIntOrDefault
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket

open class PlayerCommandAction(
    private val mode: ServerboundPlayerCommandPacket.Action,
    private val description: String
) : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val client = Minecraft.getInstance()
        val networkHandler = client.connection
            ?: return ActionResult.fail("Not connected to server")
        val player = client.player
            ?: return ActionResult.fail("Player not available")

        val hasExplicitEntity = params.has("entityId")
        val entityId = params.getIntOrDefault("entityId", player.id)
        val entity = if (hasExplicitEntity) {
            client.level?.getEntity(entityId)
                ?: return ActionResult.fail("Entity not found: $entityId")
        } else {
            player
        }

        // jumpBoost 鍙 START_RIDING_JUMP 鏈夋剰涔夛紝鍏朵粬 mode 浼?0
        val jumpBoost = if (mode == ServerboundPlayerCommandPacket.Action.START_RIDING_JUMP) {
            params.getIntOrDefault("jumpBoost", 100)
        } else {
            0
        }

        networkHandler.send(ServerboundPlayerCommandPacket(entity, mode, jumpBoost))
        return ActionResult.ok(description)
    }
}

