package com.blackboxpro.forge.action.player

import net.minecraft.network.play.client.CPacketEntityAction

class LeaveBedAction : PlayerCommandAction(
    CPacketEntityAction.Action.STOP_SLEEPING,
    "Left bed"
)
