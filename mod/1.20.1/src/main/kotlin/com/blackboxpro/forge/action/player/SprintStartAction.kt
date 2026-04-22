package com.blackboxpro.forge.action.player

import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket

class SprintStartAction : PlayerCommandAction(
    ServerboundPlayerCommandPacket.Action.START_SPRINTING,
    "Started sprinting"
)

