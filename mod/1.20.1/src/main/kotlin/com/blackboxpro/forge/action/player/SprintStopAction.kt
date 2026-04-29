package com.blackboxpro.forge.action.player

import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket

class SprintStopAction : PlayerCommandAction(
    ServerboundPlayerCommandPacket.Action.STOP_SPRINTING,
    "Stopped sprinting"
)

