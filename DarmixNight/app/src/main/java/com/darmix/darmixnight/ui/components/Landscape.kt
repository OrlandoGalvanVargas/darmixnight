package com.darmix.darmixnight.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path

val FarRidge = listOf(0f to .70f, .18f to .62f, .36f to .68f, .55f to .58f, .74f to .66f, .9f to .60f, 1f to .64f)
val MidRidge = listOf(0f to .78f, .2f to .72f, .42f to .80f, .62f to .71f, .82f to .78f, 1f to .73f)
val NearRidge = listOf(0f to .88f, .25f to .84f, .5f to .90f, .75f to .85f, 1f to .89f)

fun ridgePath(points: List<Pair<Float, Float>>, w: Float, h: Float): Path {
    val p = points.map { Offset(it.first * w, it.second * h) }
    return Path().apply {
        moveTo(0f, h)
        lineTo(p[0].x, p[0].y)
        for (i in 0 until p.size - 1) {
            val p0 = if (i > 0) p[i - 1] else p[i]
            val p1 = p[i]
            val p2 = p[i + 1]
            val p3 = if (i + 2 < p.size) p[i + 2] else p[i + 1]
            cubicTo(
                p1.x + (p2.x - p0.x) / 6f, p1.y + (p2.y - p0.y) / 6f,
                p2.x - (p3.x - p1.x) / 6f, p2.y - (p3.y - p1.y) / 6f,
                p2.x, p2.y
            )
        }
        lineTo(w, h)
        close()
    }
}
