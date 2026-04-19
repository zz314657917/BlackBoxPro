package com.blackboxpro.forge.action.player

import net.minecraft.network.play.client.CPacketEntityAction

class SneakStartAction : PlayerCommandAction(
    CPacketEntityAction.Action.START_SNEAKING,
    "Started sneaking"
)
