package com.darmix.darmixnight.ui.components

import kotlin.random.Random

class Star(
    val x: Float,
    val y: Float,
    val radiusDp: Float,
    val phase: Float,
    val appearAt: Float,
    val warm: Boolean
)

fun buildStars(count: Int, seed: Int = 7, maxY: Float = 0.82f): List<Star> {
    val random = Random(seed)
    return List(count) {
        Star(
            x = random.nextFloat(),
            y = random.nextFloat() * maxY,
            radiusDp = 0.5f + random.nextFloat() * random.nextFloat() * 1.4f,
            phase = random.nextFloat() * 6.2832f,
            appearAt = 0.04f + random.nextFloat() * 0.38f,
            warm = random.nextFloat() < 0.18f
        )
    }
}
