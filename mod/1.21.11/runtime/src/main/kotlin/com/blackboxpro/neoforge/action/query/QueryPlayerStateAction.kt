package com.blackboxpro.neoforge.action.query

import com.blackboxpro.neoforge.action.ActionExecutor
import com.blackboxpro.neoforge.action.ActionResult
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.core.registries.BuiltInRegistries

/**
 * 读取玩家完整状态信息。
 * Action ID: "query_player_state"
 */
class QueryPlayerStateAction : ActionExecutor {

    override fun execute(params: JsonObject): ActionResult {
        val client = Minecraft.getInstance()
        val player = client.player
            ?: return ActionResult.fail("Player not available")

        val data = JsonObject().apply {
            addProperty("x", player.x)
            addProperty("y", player.y)
            addProperty("z", player.z)
            addProperty("yaw", player.yRot)
            addProperty("pitch", player.xRot)
            addProperty("health", player.health)
            addProperty("maxHealth", player.maxHealth)
            addProperty("food", player.foodData.foodLevel)
            addProperty("saturation", player.foodData.saturationLevel)
            addProperty("gameMode", client.gameMode?.playerMode?.getName() ?: "unknown")
            addProperty("onGround", player.onGround())
            addProperty("sneaking", player.isShiftKeyDown)
            addProperty("sprinting", player.isSprinting)
            addProperty("flying", player.abilities.flying)
            addProperty("dead", player.isDeadOrDying)
            addProperty("selectedSlot", player.inventory.getSelectedSlot())
            addProperty("experienceLevel", player.experienceLevel)
            addProperty("experienceProgress", player.experienceProgress)

            // v1.3.0 新增字段
            addProperty("absorption", player.absorptionAmount)
            addProperty("armorValue", player.armorValue)
            addProperty("airSupply", player.airSupply)
            addProperty("maxAirSupply", player.maxAirSupply)
            addProperty("isSwimming", player.isSwimming)
            addProperty("isUsingItem", player.isUsingItem)
            addProperty("isFallFlying", player.isFallFlying)
            addProperty("fallDistance", player.fallDistance)
            addProperty("vehicleId", player.vehicle?.id ?: -1)
            addProperty("dimension", player.level().dimension().identifier().toString())
            addProperty("biome", player.level().getBiome(player.blockPosition()).unwrapKey()
                .map { it.identifier().toString() }.orElse("unknown"))
            val mainItem = player.mainHandItem
            addProperty("mainHandItem", if (mainItem.isEmpty) "empty"
                else BuiltInRegistries.ITEM.getKey(mainItem.item).toString())
        }

        return ActionResult.ok("Player state queried", data)
    }
}
