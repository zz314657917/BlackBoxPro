package com.blackboxpro.neoforge.util

import net.minecraft.world.InteractionHand

object HandUtil {
    fun fromString(hand: String): InteractionHand = when (hand.lowercase()) {
        "main_hand", "mainhand", "main" -> InteractionHand.MAIN_HAND
        "off_hand", "offhand", "off" -> InteractionHand.OFF_HAND
        else -> throw IllegalArgumentException("Unknown hand: $hand")
    }
}
