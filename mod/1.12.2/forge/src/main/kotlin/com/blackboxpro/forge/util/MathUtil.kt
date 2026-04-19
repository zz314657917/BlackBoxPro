package com.blackboxpro.forge.util

import kotlin.math.atan2
import kotlin.math.sqrt

fun calculateYawPitch(dx: Double, dy: Double, dz: Double): Pair<Float, Float> {
    val horizontalDist = sqrt(dx * dx + dz * dz)
    val yaw = (-atan2(dx, dz) * 180.0 / Math.PI).toFloat()
    val pitch = (-atan2(dy, horizontalDist) * 180.0 / Math.PI).toFloat().coerceIn(-90f, 90f)
    return yaw to pitch
}
