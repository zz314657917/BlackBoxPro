package com.blackboxpro.neoforge.action.player

import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket

class OpenHorseInventoryAction : PlayerCommandAction(
    ServerboundPlayerCommandPacket.Action.OPEN_INVENTORY,
    "Opened horse inventory"
)
