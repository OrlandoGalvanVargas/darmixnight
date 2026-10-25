package com.darmix.darmixnight.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.darmix.darmixnight.ui.theme.DnTheme

private const val CHAIN_SECONDS_PER_STEP = 0.93f

private fun chainProgress(t: Float, count: Int): Float {
    val span = count + 2f
    val period = span * CHAIN_SECONDS_PER_STEP
    return (t % period) / period * span
}

private fun chainFade(s: Float, count: Int): Float {
    val hold = count + 0.4f
    val span = count + 2f
    return if (s > hold) (1f - (s - hold) / (span - hold)).coerceIn(0f, 1f) else 1f
}

private fun nodeLit(s: Float, index: Int, count: Int): Float = ((s - index) * 4f).coerceIn(0f, 1f) * chainFade(s, count)

private fun nodeFlash(s: Float, index: Int): Float = if (s < index) 0f else (1f - (s - index) * 1.2f).coerceIn(0f, 1f)

class ConstellationApp(val glyph: DnGlyph, val label: String)

@Composable
fun AppConstellation(apps: List<ConstellationApp>, modifier: Modifier = Modifier) {
    val colors = DnTheme.colors
    val clock = rememberSceneClock()
    val node = 44.dp
    val lift = 16.dp
    Box(modifier.fillMaxWidth()) {
        Spacer(
            Modifier
                .fillMaxWidth()
                .height(node + lift)
                .drawBehind {
                    val cell = size.width / apps.size
                    val half = node.toPx() / 2f
                    val points = apps.indices.map { i ->
                        Offset(cell * (i + 0.5f), half + if (i % 2 == 1) lift.toPx() else 0f)
                    }
                    val s = chainProgress(clock.value, apps.size)
                    val fade = chainFade(s, apps.size)
                    for (i in 0 until points.lastIndex) {
                        drawLine(
                            colors.accent.copy(alpha = 0.35f),
                            points[i],
                            points[i + 1],
                            strokeWidth = 1.4.dp.toPx(),
                            cap = StrokeCap.Round,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(1.dp.toPx(), 5.dp.toPx()))
                        )
                        val progress = (s - i).coerceIn(0f, 1f)
                        if (progress > 0f && fade > 0f) {
                            val head = Offset(
                                points[i].x + (points[i + 1].x - points[i].x) * progress,
                                points[i].y + (points[i + 1].y - points[i].y) * progress
                            )
                            drawLine(
                                colors.accent,
                                points[i],
                                head,
                                strokeWidth = 2.2.dp.toPx(),
                                cap = StrokeCap.Round,
                                alpha = fade
                            )
                            if (progress < 1f) {
                                drawCircle(colors.accent, 9.dp.toPx(), head, alpha = 0.18f * fade)
                                drawCircle(colors.accent, 3.5.dp.toPx(), head, alpha = fade)
                            }
                        }
                    }
                    points.forEachIndexed { i, p ->
                        val flash = nodeFlash(s, i) * fade
                        if (flash > 0f) {
                            drawCircle(
                                colors.accent,
                                half + (6f + 16f * (1f - flash)).dp.toPx(),
                                p,
                                alpha = 0.4f * flash,
                                style = Stroke(2.dp.toPx() * flash)
                            )
                        }
                        val lit = nodeLit(s, i, apps.size)
                        if (lit > 0f) {
                            drawCircle(colors.accent, half + 5.dp.toPx(), p, alpha = 0.16f * lit)
                        }
                    }
                }
        )
        Row(Modifier.fillMaxWidth()) {
            apps.forEachIndexed { i, app ->
                Column(
                    Modifier
                        .weight(1f)
                        .offset(y = if (i % 2 == 1) lift else 0.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        Modifier
                            .size(node)
                            .graphicsLayer {
                                val s = chainProgress(clock.value, apps.size)
                                val pop = 1f + 0.14f * nodeFlash(s, i) * chainFade(s, apps.size)
                                scaleX = pop
                                scaleY = pop
                            }
                            .padding(3.dp)
                            .background(colors.card, CircleShape)
                            .border(1.dp, colors.accent.copy(alpha = 0.45f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Glyph(app.glyph, colors.accent, size = 19.dp, strokeWidth = 1.9f)
                        Box(
                            Modifier
                                .matchParentSize()
                                .graphicsLayer { alpha = nodeLit(chainProgress(clock.value, apps.size), i, apps.size) }
                                .background(colors.accent, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Glyph(app.glyph, colors.onAccent, size = 19.dp, strokeWidth = 2.1f)
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        app.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.textDim,
                        maxLines = 1
                    )
                }
            }
        }
    }
    Spacer(Modifier.height(lift))
}
