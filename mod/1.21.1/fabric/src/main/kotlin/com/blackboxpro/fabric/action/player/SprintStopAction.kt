package com.blackboxpro.fabric.action.player

import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket

class SprintStopAction : PlayerCommandAction(
    ClientCommandC2SPacket.Mode.STOP_SPRINTING,
    "Stopped sprinting"
)
