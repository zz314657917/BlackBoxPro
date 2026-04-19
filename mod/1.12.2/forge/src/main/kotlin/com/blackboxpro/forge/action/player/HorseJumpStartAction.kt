package com.blackboxpro.forge.action.player

import net.minecraft.network.play.client.CPacketEntityAction

class HorseJumpStartAction : PlayerCommandAction(
    CPacketEntityAction.Action.START_RIDING_JUMP,
    "Started horse jump"
)
