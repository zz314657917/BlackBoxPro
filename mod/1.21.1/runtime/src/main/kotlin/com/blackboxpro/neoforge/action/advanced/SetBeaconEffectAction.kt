package com.blackboxpro.neoforge.action.advanced

import com.blackboxpro.neoforge.action.ActionExecutor
import com.blackboxpro.neoforge.action.ActionResult
import com.blackboxpro.neoforge.util.getIntOrDefault
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.world.effect.MobEffect
import net.minecraft.network.protocol.game.ServerboundSetBeaconPacket
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.Holder
import java.util.Optional

class SetBeaconEffectAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val primaryEffect = params.getIntOrDefault("primaryEffect", -1)
        val secondaryEffect = params.getIntOrDefault("secondaryEffect", -1)

        val networkHandler = Minecraft.getInstance().connection
            ?: return ActionResult.fail("Not connected to server")

        val primaryOpt = resolveEffect(primaryEffect)
        val secondaryOpt = resolveEffect(secondaryEffect)

        networkHandler.send(
            ServerboundSetBeaconPacket(primaryOpt, secondaryOpt)
        )
        return ActionResult.ok("Set beacon effects: primary=$primaryEffect, secondary=$secondaryEffect")
    }

    private fun resolveEffect(id: Int): Optional<Holder<MobEffect>> {
        if (id < 0) return Optional.empty()
        val effect = BuiltInRegistries.MOB_EFFECT.byId(id) ?: return Optional.empty()
        return Optional.of(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect))
    }
}
