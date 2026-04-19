package com.blackboxpro.neoforge.action.player

import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket

class ElytraStartAction : PlayerCommandAction(
    ServerboundPlayerCommandPacket.Action.START_FALL_FLYING,
    "Started elytra flight"
)
