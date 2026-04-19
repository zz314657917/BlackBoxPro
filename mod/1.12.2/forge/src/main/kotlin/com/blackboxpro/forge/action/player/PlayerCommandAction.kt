package com.blackboxpro.forge.action.player

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.blackboxpro.forge.util.getIntOrDefault
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.network.play.client.CPacketEntityAction

open class PlayerCommandAction(
    private val action: CPacketEntityAction.Action,
    private val description: String
) : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val mc = Minecraft.getMinecraft()
        val connection = mc.connection
            ?: return ActionResult.fail("Not connected to server")
        val player = mc.player
            ?: return ActionResult.fail("Player not available")

        val hasExplicitEntity = params.has("entityId")
        val entityId = params.getIntOrDefault("entityId", player.entityId)
        val entity = if (hasExplicitEntity) {
            mc.world?.getEntityByID(entityId)
                ?: return ActionResult.fail("Entity not found: $entityId")
        } else {
            player
        }

        // auxData 只对 START_RIDING_JUMP 有意义，其他 action 传 0
        val auxData = if (action == CPacketEntityAction.Action.START_RIDING_JUMP) {
            params.getIntOrDefault("jumpBoost", 100)
        } else {
            0
        }

        connection.sendPacket(CPacketEntityAction(entity, action, auxData))
        return ActionResult.ok(description)
    }
}
