package com.blackboxpro.neoforge.action.player

import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket

class HorseJumpStopAction : PlayerCommandAction(
    ServerboundPlayerCommandPacket.Action.STOP_RIDING_JUMP,
    "Stopped horse jump"
)
