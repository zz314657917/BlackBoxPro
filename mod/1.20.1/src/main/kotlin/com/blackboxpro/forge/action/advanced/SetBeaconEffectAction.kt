package com.blackboxpro.forge.action.advanced

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.blackboxpro.forge.util.getIntOrDefault
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.protocol.game.ServerboundSetBeaconPacket
import net.minecraft.world.effect.MobEffect
import java.util.Optional

class SetBeaconEffectAction : ActionExecutor {

    override fun execute(params: JsonObject): ActionResult {
        val primaryEffect = params.getIntOrDefault("primaryEffect", -1)
        val secondaryEffect = params.getIntOrDefault("secondaryEffect", -1)

        val networkHandler = Minecraft.getInstance().connection
            ?: return ActionResult.fail("Not connected to server")

        val primaryOpt = resolveEffect(primaryEffect)
        val secondaryOpt = resolveEffect(secondaryEffect)

        networkHandler.send(ServerboundSetBeaconPacket(primaryOpt, secondaryOpt))
        return ActionResult.ok("Set beacon effects: primary=$primaryEffect, secondary=$secondaryEffect")
    }

    private fun resolveEffect(id: Int): Optional<MobEffect> {
        if (id < 0) return Optional.empty()
        return Optional.ofNullable(BuiltInRegistries.MOB_EFFECT.byId(id))
    }
}
