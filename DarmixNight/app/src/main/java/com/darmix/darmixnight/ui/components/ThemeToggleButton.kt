package com.darmix.darmixnight.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.darmix.darmixnight.ui.theme.DnTheme
import kotlin.math.cos
import kotlin.math.sin

private val DaySky = Color(0xFF8EC1EC)
private val NightSky = Color(0xFF0B1430)
private val SunCore = Color(0xFFFFD36B)
private val MoonCore = Color(0xFFF4E3BC)
private val StarTint = Color(0xFFF4E3BC)

private class TrackStar(val fx: Float, val fy: Float, val radiusDp: Float, val phase: Float)

private val TrackStars = listOf(
    TrackStar(0.20f, 0.30f, 1.3f, 0f),
    TrackStar(0.36f, 0.70f, 0.9f, 1.7f),
    TrackStar(0.50f, 0.26f, 1.1f, 3.1f),
    TrackStar(0.17f, 0.74f, 0.8f, 4.4f)
)

@Composable
fun ThemeToggleButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = DnTheme.colors
    val isDark = colors.isDark
    val haptics = LocalHapticFeedback.current
    val clock = rememberSceneClock()
    val k by animateFloatAsState(
        targetValue = if (isDark) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessLow),
        label = "skySwitch"
    )

    Spacer(
        modifier
            .size(width = 64.dp, height = 34.dp)
            .clip(CircleShape)
            .semantics {
                role = Role.Switch
                contentDescription = if (isDark) "Cambiar a tema claro" else "Cambiar a tema oscuro"
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .drawBehind {
                val t = clock.value
                val w = size.width
                val h = size.height
                val r = h / 2f
                val track = lerp(DaySky, NightSky, k)
                drawRoundRect(track, cornerRadius = CornerRadius(r))

                if (k > 0.01f) {
                    TrackStars.forEach { s ->
                        val twinkle = 0.55f + 0.45f * sin(t * 2.2f + s.phase)
                        drawCircle(StarTint, s.radiusDp.dp.toPx(), Offset(w * s.fx, h * s.fy), alpha = k * twinkle)
                    }
                }

                val cloudAlpha = (1f - k).coerceIn(0f, 1f)
                if (cloudAlpha > 0.01f) {
                    val drift = 2.dp.toPx() * sin(t * 0.8f)
                    val cx = w * 0.70f + 14.dp.toPx() * k + drift
                    val cy = h * 0.62f
                    val u = 1.dp.toPx()
                    drawCircle(Color.White, 4.5f * u, Offset(cx - 5f * u, cy + 1f * u), alpha = 0.9f * cloudAlpha)
                    drawCircle(Color.White, 6f * u, Offset(cx + 1f * u, cy - 2f * u), alpha = 0.9f * cloudAlpha)
                    drawCircle(Color.White, 4f * u, Offset(cx + 7f * u, cy + 1.5f * u), alpha = 0.9f * cloudAlpha)
                    drawRoundRect(
                        Color.White,
                        topLeft = Offset(cx - 9f * u, cy + 0.5f * u),
                        size = androidx.compose.ui.geometry.Size(19f * u, 5f * u),
                        cornerRadius = CornerRadius(2.5f * u),
                        alpha = 0.9f * cloudAlpha
                    )
                }

                val knobR = r - 4.dp.toPx()
                val knob = Offset(r + (w - 2f * r) * k, r)
                drawCircle(lerp(SunCore, MoonCore, k), knobR + 3.dp.toPx(), knob, alpha = 0.22f)

                if (k < 0.5f) {
                    val raysAlpha = 1f - k * 2f
                    val spin = t * 0.6f
                    for (i in 0 until 8) {
                        val a = spin + i * 0.7853982f
                        drawLine(
                            SunCore,
                            Offset(knob.x + (knobR + 1.dp.toPx()) * cos(a), knob.y + (knobR + 1.dp.toPx()) * sin(a)),
                            Offset(knob.x + (knobR + 3.dp.toPx()) * cos(a), knob.y + (knobR + 3.dp.toPx()) * sin(a)),
                            strokeWidth = 1.6.dp.toPx(),
                            cap = StrokeCap.Round,
                            alpha = raysAlpha
                        )
                    }
                }

                val body = Path().apply { addOval(Rect(knob, knobR)) }
                val shape = if (k > 0.01f) {
                    val cut = Path().apply {
                        addOval(
                            Rect(
                                Offset(knob.x + knobR * (2.1f - 1.45f * k), knob.y - knobR * 0.45f * k),
                                knobR * 0.85f
                            )
                        )
                    }
                    Path().apply { op(body, cut, PathOperation.Difference) }
                } else {
                    body
                }
                drawPath(shape, lerp(SunCore, MoonCore, k))

                drawRoundRect(
                    Color.White.copy(alpha = 0.16f),
                    cornerRadius = CornerRadius(r),
                    style = Stroke(1.dp.toPx())
                )
            }
    )
}
