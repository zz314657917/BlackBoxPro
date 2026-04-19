package com.blackboxpro.forge.action.query

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft

class QueryPlayerStateAction : ActionExecutor {

    override fun execute(params: JsonObject): ActionResult {
        val mc = Minecraft.getMinecraft()
        val player = mc.player
            ?: return ActionResult.fail("Player not available")

        val data = JsonObject().apply {
            addProperty("x", player.posX)
            addProperty("y", player.posY)
            addProperty("z", player.posZ)
            addProperty("yaw", player.rotationYaw)
            addProperty("pitch", player.rotationPitch)
            addProperty("health", player.health)
            addProperty("maxHealth", player.maxHealth)
            addProperty("food", player.foodStats.foodLevel)
            addProperty("saturation", player.foodStats.saturationLevel)
            addProperty("gameMode", mc.playerController?.currentGameType?.getName() ?: "unknown")
            addProperty("onGround", player.onGround)
            addProperty("sneaking", player.isSneaking)
            addProperty("sprinting", player.isSprinting)
            addProperty("flying", player.capabilities.isFlying)
            addProperty("dead", player.isDead)
            addProperty("selectedSlot", player.inventory.currentItem)
            addProperty("experienceLevel", player.experienceLevel)
            addProperty("experienceProgress", player.experience)

            // v1.3.0 新增字段
            addProperty("absorption", player.absorptionAmount)
            addProperty("armorValue", player.totalArmorValue)
            addProperty("airSupply", player.air)
            addProperty("maxAirSupply", 300) // 1.12.2 固定值
            addProperty("isSwimming", false) // 1.12.2 无游泳机制
            addProperty("isUsingItem", player.isHandActive)
            addProperty("isFallFlying", player.isElytraFlying)
            addProperty("fallDistance", player.fallDistance)
            addProperty("vehicleId", player.ridingEntity?.entityId ?: -1)
            addProperty("dimension", dimensionToString(player.dimension))
            val biome = mc.world?.getBiome(player.position)
            addProperty("biome", biome?.registryName?.toString() ?: "unknown")
            val mainItem = player.heldItemMainhand
            addProperty("mainHandItem", if (mainItem.isEmpty) "empty"
                else (mainItem.item.registryName?.toString() ?: "unknown"))
        }

        return ActionResult.ok("Player state queried", data)
    }

    private fun dimensionToString(dim: Int): String = when (dim) {
        0 -> "minecraft:overworld"
        -1 -> "minecraft:the_nether"
        1 -> "minecraft:the_end"
        else -> "unknown:dim_$dim"
    }
}
