package com.darmix.darmixnight.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.darmix.darmixnight.ui.theme.DayGlow
import com.darmix.darmixnight.ui.theme.DnTheme
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ThemeToggleButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = DnTheme.colors
    val isDark = colors.isDark
    val sunAmount by animateFloatAsState(
        targetValue = if (isDark) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "sunAmount"
    )
    val moon = remember { Crescent.unitPath() }
    val sunColor = DayGlow
    val moonColor = colors.accent

    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = colors.raised,
        border = BorderStroke(1.dp, colors.outline),
        modifier = modifier
            .size(46.dp)
            .semantics {
                contentDescription = if (isDark) "Cambiar a tema claro" else "Cambiar a tema oscuro"
            }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Spacer(
                Modifier
                    .size(22.dp)
                    .graphicsLayer { rotationZ = 180f * sunAmount }
                    .drawWithCache {
                        val r = size.minDimension / 2f
                        val center = Offset(size.width / 2f, size.height / 2f)
                        onDrawBehind {
                            val s = sunAmount
                            if (s > 0.01f) {
                                drawCircle(sunColor, radius = r * 0.42f * s, center = center, alpha = s)
                                repeat(8) { i ->
                                    val a = (Math.PI / 4.0) * i
                                    val dx = cos(a).toFloat()
                                    val dy = sin(a).toFloat()
                                    drawLine(
                                        color = sunColor,
                                        start = Offset(center.x + dx * r * 0.66f, center.y + dy * r * 0.66f),
                                        end = Offset(center.x + dx * r * (0.66f + 0.3f * s), center.y + dy * r * (0.66f + 0.3f * s)),
                                        strokeWidth = r * 0.18f,
                                        cap = StrokeCap.Round,
                                        alpha = s
                                    )
                                }
                            }
                            val m = 1f - s
                            if (m > 0.01f) {
                                translate(center.x, center.y) {
                                    rotate(-180f * s, pivot = Offset.Zero) {
                                        scale(r * 0.92f * m, r * 0.92f * m, pivot = Offset.Zero) {
                                            drawPath(moon, moonColor, alpha = m)
                                        }
                                    }
                                }
                            }
                        }
                    }
            )
        }
    }
}
