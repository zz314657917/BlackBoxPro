package com.blackboxpro.neoforge.action.player

import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket

class LeaveBedAction : PlayerCommandAction(
    ServerboundPlayerCommandPacket.Action.STOP_SLEEPING,
    "Left bed"
)
