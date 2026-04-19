package com.blackboxpro.forge.util

import net.minecraft.util.EnumHand

object HandUtil {
    fun fromString(hand: String): EnumHand = when (hand.lowercase()) {
        "main_hand", "mainhand", "main" -> EnumHand.MAIN_HAND
        "off_hand", "offhand", "off" -> EnumHand.OFF_HAND
        else -> throw IllegalArgumentException("Unknown hand: $hand")
    }
}
