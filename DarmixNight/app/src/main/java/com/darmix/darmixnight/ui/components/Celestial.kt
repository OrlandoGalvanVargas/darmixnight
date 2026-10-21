package com.darmix.darmixnight.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import kotlin.random.Random

object Crescent {
    const val VIEWPORT = 108f
    const val OUTER_RADIUS = 24f
    private const val INNER_RADIUS = 20f
    private const val INNER_DX = 9.5f
    private const val INNER_DY = -8.5f

    fun unitPath(): Path {
        val outer = Path().apply { addOval(Rect(Offset.Zero, 1f)) }
        val inner = Path().apply {
            addOval(
                Rect(
                    Offset(INNER_DX / OUTER_RADIUS, INNER_DY / OUTER_RADIUS),
                    INNER_RADIUS / OUTER_RADIUS
                )
            )
        }
        return Path().apply { op(outer, inner, PathOperation.Difference) }
    }
}

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
