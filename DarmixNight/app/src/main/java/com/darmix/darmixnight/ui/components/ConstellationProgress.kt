package com.darmix.darmixnight.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.darmix.darmixnight.ui.theme.DnTheme

private val NodeHeights = listOf(0.66f, 0.28f, 0.60f, 0.22f, 0.56f, 0.30f, 0.62f, 0.26f)

private fun nodeLayout(count: Int): List<Offset> {
    if (count <= 1) return listOf(Offset(0.5f, 0.5f))
    return List(count) { i ->
        Offset(i / (count - 1).toFloat(), NodeHeights[i % NodeHeights.size])
    }
}

@Composable
fun ConstellationProgress(
    lit: List<Boolean>,
    modifier: Modifier = Modifier
) {
    val colors = DnTheme.colors
    val count = lit.size
    val anims = remember(count) { List(count) { Animatable(0f) } }
    lit.take(count).forEachIndexed { i, on ->
        LaunchedEffect(on, anims) {
            anims[i].animateTo(
                if (on) 1f else 0f,
                tween(durationMillis = 650, delayMillis = if (on) i * 70 else 0, easing = FastOutSlowInEasing)
            )
        }
    }
    val current = lit.take(count).indexOfFirst { !it }
    val pulse = rememberInfiniteTransition(label = "constellation").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Restart),
        label = "pulse"
    )
    val litCount = lit.count { it }

    Spacer(
        modifier
            .fillMaxWidth()
            .height(70.dp)
            .semantics { contentDescription = "$litCount de ${lit.size} permisos concedidos" }
            .drawWithCache {
                val pad = 14.dp.toPx()
                val points = nodeLayout(count).map {
                    Offset(pad + it.x * (size.width - pad * 2), pad + it.y * (size.height - pad * 2))
                }
                val lineWidth = 1.5.dp.toPx()
                val nodeRadius = 4.5.dp.toPx()
                val haloRadius = 16.dp.toPx()
                val ring = Stroke(width = 1.5.dp.toPx())
                onDrawBehind {
                    for (i in 0 until points.size - 1) {
                        val a = points[i]
                        val b = points[i + 1]
                        drawLine(colors.textDim.copy(alpha = 0.22f), a, b, lineWidth, StrokeCap.Round)
                        val f = minOf(anims[i].value, anims[i + 1].value)
                        if (f > 0f) {
                            drawLine(
                                color = colors.accent.copy(alpha = 0.85f),
                                start = a,
                                end = a + (b - a) * f,
                                strokeWidth = lineWidth,
                                cap = StrokeCap.Round
                            )
                        }
                    }
                    points.forEachIndexed { i, p ->
                        val v = anims[i].value
                        if (v > 0f) {
                            drawCircle(
                                brush = Brush.radialGradient(
                                    0f to colors.glow.copy(alpha = 0.45f * v),
                                    1f to Color.Transparent,
                                    center = p,
                                    radius = haloRadius
                                ),
                                radius = haloRadius,
                                center = p
                            )
                        }
                        drawCircle(colors.textDim.copy(alpha = 0.5f * (1f - v)), nodeRadius * 0.8f, p, style = ring)
                        drawCircle(colors.accent, nodeRadius * v, p)
                        if (i == current) {
                            val t = pulse.value
                            drawCircle(
                                color = colors.accent,
                                radius = nodeRadius + haloRadius * 0.8f * t,
                                center = p,
                                alpha = 0.6f * (1f - t),
                                style = ring
                            )
                        }
                    }
                }
            }
    )
}
