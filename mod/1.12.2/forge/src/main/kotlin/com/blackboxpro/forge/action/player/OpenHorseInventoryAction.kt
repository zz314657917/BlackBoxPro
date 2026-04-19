package com.blackboxpro.forge.action.player

import net.minecraft.network.play.client.CPacketEntityAction

class OpenHorseInventoryAction : PlayerCommandAction(
    CPacketEntityAction.Action.OPEN_INVENTORY,
    "Opened horse inventory"
)
