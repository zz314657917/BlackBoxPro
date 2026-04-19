package com.blackboxpro.forge.action.player

import net.minecraft.network.play.client.CPacketEntityAction

class SneakStopAction : PlayerCommandAction(
    CPacketEntityAction.Action.STOP_SNEAKING,
    "Stopped sneaking"
)
