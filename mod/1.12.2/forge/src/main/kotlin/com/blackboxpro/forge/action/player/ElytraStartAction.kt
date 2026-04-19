package com.blackboxpro.forge.action.player

import net.minecraft.network.play.client.CPacketEntityAction

class ElytraStartAction : PlayerCommandAction(
    CPacketEntityAction.Action.START_FALL_FLYING,
    "Started elytra flight"
)
