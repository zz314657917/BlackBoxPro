package com.blackboxpro.forge.action.player

import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket

class HorseJumpStartAction : PlayerCommandAction(
    ServerboundPlayerCommandPacket.Action.START_RIDING_JUMP,
    "Started horse jump"
)

