package com.blackboxpro.forge.action.player

import net.minecraft.network.play.client.CPacketEntityAction

class HorseJumpStopAction : PlayerCommandAction(
    CPacketEntityAction.Action.STOP_RIDING_JUMP,
    "Stopped horse jump"
)
