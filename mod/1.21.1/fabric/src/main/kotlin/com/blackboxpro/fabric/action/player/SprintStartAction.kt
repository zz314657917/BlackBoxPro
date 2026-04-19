package com.blackboxpro.fabric.action.player

import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket

class SprintStartAction : PlayerCommandAction(
    ClientCommandC2SPacket.Mode.START_SPRINTING,
    "Started sprinting"
)
