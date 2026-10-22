package com.darmix.darmixnight.ui.main

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.darmix.darmixnight.ui.components.Crescent
import com.darmix.darmixnight.ui.components.FarRidge
import com.darmix.darmixnight.ui.components.MidRidge
import com.darmix.darmixnight.ui.components.NearRidge
import com.darmix.darmixnight.ui.components.ridgePath
import com.darmix.darmixnight.ui.components.DnGlyph
import com.darmix.darmixnight.ui.components.Glyph
import com.darmix.darmixnight.ui.components.buildStars
import com.darmix.darmixnight.ui.theme.DnType
import com.darmix.darmixnight.ui.theme.Sora
import kotlin.math.PI
import kotlin.math.sin

private class ScenePalette(
    val skyTop: Color,
    val skyMid: Color,
    val skyBottom: Color,
    val far: Color,
    val mid: Color,
    val near: Color,
    val sun: Color,
    val glow: Color
)

private val DaySky = ScenePalette(
    skyTop = Color(0xFF2F7FD8), skyMid = Color(0xFF7DB8F2), skyBottom = Color(0xFFD6ECFF),
    far = Color(0xFFA9CDEF), mid = Color(0xFF7FB0E0), near = Color(0xFF4C86C6),
    sun = Color(0xFFFFD45C), glow = Color(0xFFFFE9A8)
)

private val DuskSky = ScenePalette(
    skyTop = Color(0xFF142A55), skyMid = Color(0xFF3C5D93), skyBottom = Color(0xFFF2A65A),
    far = Color(0xFF2A3F68), mid = Color(0xFF1B2C4E), near = Color(0xFF101C35),
    sun = Color(0xFFFFB347), glow = Color(0xFFFFCF8A)
)

private val NightSky = ScenePalette(
    skyTop = Color(0xFF040913), skyMid = Color(0xFF0B1830), skyBottom = Color(0xFF15305A),
    far = Color(0xFF0F1E38), mid = Color(0xFF0B172C), near = Color(0xFF070F1F),
    sun = Color(0xFFF7E6B5), glow = Color(0xFFF7E6B5)
)

private val PausedSky = ScenePalette(
    skyTop = Color(0xFF2A3140), skyMid = Color(0xFF3E4656), skyBottom = Color(0xFF5A6273),
    far = Color(0xFF4A5262), mid = Color(0xFF3A4150), near = Color(0xFF2A303C),
    sun = Color(0xFFB9C0CC), glow = Color(0xFFB9C0CC)
)

private fun mix(a: ScenePalette, b: ScenePalette, t: Float) = ScenePalette(
    lerp(a.skyTop, b.skyTop, t), lerp(a.skyMid, b.skyMid, t), lerp(a.skyBottom, b.skyBottom, t),
    lerp(a.far, b.far, t), lerp(a.mid, b.mid, t), lerp(a.near, b.near, t),
    lerp(a.sun, b.sun, t), lerp(a.glow, b.glow, t)
)

private fun smoothStep(edge0: Float, edge1: Float, x: Float): Float {
    val t = ((x - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

private const val CLOUD_UNIT_W = 100f
private const val CLOUD_UNIT_H = 42f

private fun cloudShape(): Path {
    var shape = Path().apply {
        addRoundRect(RoundRect(Rect(11f, 28f, 89f, 42f), CornerRadius(7f)))
    }
    listOf(
        Triple(22f, 32f, 11f),
        Triple(36f, 24f, 14f),
        Triple(52f, 17f, 17f),
        Triple(68f, 24f, 13f),
        Triple(80f, 31f, 9f)
    ).forEach { (x, y, r) ->
        val bump = Path().apply { addOval(Rect(Offset(x, y), r)) }
        val merged = Path()
        merged.op(shape, bump, PathOperation.Union)
        shape = merged
    }
    return shape
}

private class CloudSpec(
    val offset: Float,
    val y: Float,
    val widthDp: Float,
    val alpha: Float,
    val speed: Int,
    val near: Boolean
)

private val Clouds = listOf(
    CloudSpec(0.05f, 0.10f, 44f, 0.55f, 1, false),
    CloudSpec(0.42f, 0.05f, 38f, 0.45f, 1, false),
    CloudSpec(0.74f, 0.20f, 50f, 0.50f, 1, false),
    CloudSpec(0.18f, 0.24f, 96f, 0.96f, 2, true),
    CloudSpec(0.66f, 0.34f, 124f, 0.96f, 2, true)
)

private val CloudTop = Color(0xFFFFFFFF)
private val CloudMid = Color(0xFFF7FBFF)
private val CloudBottom = Color(0xFFCFE0F5)
private val CloudDuskTop = Color(0xFFFFE6CC)
private val CloudDuskBottom = Color(0xFFF2B38A)
private val CloudShadow = Color(0xFF1F4E86)

@Composable
fun SkyHero(
    startMinutes: Int,
    endMinutes: Int,
    now: Int,
    blocking: Boolean,
    paused: Boolean,
    modifier: Modifier = Modifier
) {
    val awakeSpan = TimeText.between(endMinutes, startMinutes).coerceAtLeast(1)
    val sleepSpan = TimeText.between(startMinutes, endMinutes).coerceAtLeast(1)
    val dayProgress = (TimeText.between(endMinutes, now).toFloat() / awakeSpan).coerceIn(0f, 1f)
    val nightProgress = (TimeText.between(startMinutes, now).toFloat() / sleepSpan).coerceIn(0f, 1f)

    val nightAmount by animateFloatAsState(if (blocking) 1f else 0f, tween(1200), label = "night")
    val pausedAmount by animateFloatAsState(if (paused) 1f else 0f, tween(700), label = "paused")
    val duskAmount = smoothStep(0.72f, 0.96f, dayProgress)

    val time = rememberInfiniteTransition(label = "sky").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(60_000, easing = LinearEasing), RepeatMode.Restart),
        label = "skyTime"
    )
    val stars = remember { buildStars(count = 70, seed = 11, maxY = 0.6f) }
    val moon = remember { Crescent.unitPath() }
    val cloudPath = remember { cloudShape() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(250.dp)
            .clip(RoundedCornerShape(30.dp))
            .semantics { contentDescription = "Cielo de DarmixNight" }
            .drawWithCache {
                val w = size.width
                val h = size.height
                val far = ridgePath(FarRidge, w, h)
                val mid = ridgePath(MidRidge, w, h)
                val near = ridgePath(NearRidge, w, h)
                val scrim = Brush.verticalGradient(0.5f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.38f))
                onDrawBehind {
                    val base = mix(mix(DaySky, DuskSky, duskAmount), NightSky, nightAmount)
                    val pal = mix(base, PausedSky, pausedAmount * 0.8f)
                    val t = time.value
                    val cloudBrush = Brush.verticalGradient(
                        0f to lerp(CloudTop, CloudDuskTop, duskAmount),
                        0.55f to lerp(CloudMid, CloudDuskTop, duskAmount),
                        1f to lerp(CloudBottom, CloudDuskBottom, duskAmount),
                        startY = 0f,
                        endY = CLOUD_UNIT_H
                    )

                    drawRect(
                        Brush.verticalGradient(
                            0f to pal.skyTop,
                            0.62f to pal.skyMid,
                            1f to pal.skyBottom
                        )
                    )

                    val starAlpha = maxOf(nightAmount, duskAmount * 0.35f) * (1f - pausedAmount * 0.6f)
                    if (starAlpha > 0.01f) {
                        stars.forEach { s ->
                            val twinkle = 0.5f + 0.5f * sin(t * 2f * PI.toFloat() * 6f + s.phase)
                            drawCircle(
                                color = if (s.warm) pal.glow else Color.White,
                                radius = s.radiusDp * density,
                                center = Offset(s.x * w, s.y * h),
                                alpha = starAlpha * (0.35f + 0.6f * twinkle)
                            )
                        }
                    }

                    val bob = sin(t * 2f * PI.toFloat() * 4f) * 2.dp.toPx()
                    val sunP = dayProgress
                    val sun = Offset(w * (0.14f + 0.72f * sunP), h * 0.6f - sin(PI.toFloat() * sunP) * h * 0.42f + bob)
                    val moonP = nightProgress
                    val moonPos = Offset(w * (0.14f + 0.72f * moonP), h * 0.6f - sin(PI.toFloat() * moonP) * h * 0.42f + bob)
                    val bodyAlphaSun = 1f - nightAmount
                    val bodyAlphaMoon = nightAmount
                    val glowRadius = 72.dp.toPx()

                    if (bodyAlphaSun > 0.01f) {
                        drawCircle(
                            Brush.radialGradient(
                                0f to pal.glow.copy(alpha = 0.55f * bodyAlphaSun),
                                1f to Color.Transparent,
                                center = sun,
                                radius = glowRadius
                            ),
                            glowRadius,
                            sun
                        )
                        drawCircle(pal.sun, 16.dp.toPx() + duskAmount * 3.dp.toPx(), sun, alpha = bodyAlphaSun)
                    }
                    if (bodyAlphaMoon > 0.01f) {
                        drawCircle(
                            Brush.radialGradient(
                                0f to pal.glow.copy(alpha = 0.4f * bodyAlphaMoon),
                                1f to Color.Transparent,
                                center = moonPos,
                                radius = glowRadius
                            ),
                            glowRadius,
                            moonPos
                        )
                        val r = 18.dp.toPx()
                        translate(moonPos.x, moonPos.y) {
                            scale(r, r, pivot = Offset.Zero) { drawPath(moon, pal.sun, alpha = bodyAlphaMoon) }
                        }
                    }

                    val cloudAlpha = (1f - duskAmount * 0.85f) * (1f - nightAmount) * (1f - pausedAmount * 0.5f)
                    if (cloudAlpha > 0.01f) {
                        Clouds.forEach { c ->
                            val cw = c.widthDp.dp.toPx()
                            val k = cw / CLOUD_UNIT_W
                            val x = ((c.offset + t * c.speed) % 1f) * (w + cw) - cw
                            val y = h * c.y + sin(t * 2f * PI.toFloat() * 3f + c.offset * 6f) * 1.5.dp.toPx()
                            translate(x, y) {
                                scale(k, k, pivot = Offset.Zero) {
                                    if (c.near) {
                                        translate(0f, 3f) { drawPath(cloudPath, CloudShadow, alpha = 0.10f * cloudAlpha) }
                                    }
                                    drawPath(cloudPath, cloudBrush, alpha = c.alpha * cloudAlpha)
                                }
                            }
                        }
                    }

                    drawPath(far, pal.far)
                    drawPath(mid, pal.mid)
                    drawPath(near, pal.near)
                    drawRect(scrim)
                }
            }
    ) {
        HeroBadge(
            paused = paused,
            blocking = blocking,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        )

        AnimatedContent(
            targetState = Triple(paused, blocking, now),
            transitionSpec = { fadeIn(tween(350)) togetherWith fadeOut(tween(200)) },
            contentKey = { it.first to it.second },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 20.dp, end = 20.dp, bottom = 18.dp),
            label = "heroText"
        ) { (isPaused, isBlocking, minuteNow) ->
            Column {
                val label = when {
                    isPaused -> "SERVICIO EN PAUSA"
                    isBlocking -> "DESCANSANDO · TERMINA EN"
                    else -> "EL DESCANSO EMPIEZA EN"
                }
                Text(label, style = DnType.overline, color = Color.White.copy(alpha = 0.9f))
                Spacer(Modifier.height(4.dp))
                if (isPaused) {
                    Text(
                        "Pausado",
                        style = DnType.clock.copy(fontSize = 46.sp, lineHeight = 52.sp),
                        color = Color.White
                    )
                    Text(
                        "El bloqueo no se aplicará hasta que lo reactives.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                } else {
                    val target = if (isBlocking) endMinutes else startMinutes
                    BigCountdown(TimeText.between(minuteNow, target))
                    Text(
                        "a las ${TimeText.full(target)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }
        }
    }
}

@Composable
private fun BigCountdown(totalMinutes: Int) {
    val unit = SpanStyle(fontFamily = Sora, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color.White.copy(alpha = 0.85f), letterSpacing = 0.sp)
    val h = totalMinutes / 60
    val m = totalMinutes % 60
    val text = buildAnnotatedString {
        when {
            totalMinutes <= 0 -> {
                append("<1")
                withStyle(unit) { append(" min") }
            }
            h == 0 -> {
                append("$m")
                withStyle(unit) { append(" min") }
            }
            else -> {
                append("$h")
                withStyle(unit) { append(" h ") }
                append("%02d".format(m))
                withStyle(unit) { append(" min") }
            }
        }
    }
    Text(text, style = DnType.clock.copy(fontSize = 46.sp, lineHeight = 52.sp), color = Color.White)
}

@Composable
private fun HeroBadge(paused: Boolean, blocking: Boolean, modifier: Modifier = Modifier) {
    val (glyph, label, tone) = when {
        paused -> Triple(DnGlyph.POWER, "En pausa", Color(0xFFC9D0DB))
        blocking -> Triple(DnGlyph.MOON, "Modo descanso", Color(0xFFF2D58A))
        else -> Triple(DnGlyph.SHIELD, "Protección activa", Color(0xFF7BE0B4))
    }
    val pulse = rememberInfiniteTransition(label = "badge").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Restart),
        label = "badgePulse"
    )
    AnimatedContent(
        targetState = Triple(glyph, label, tone),
        transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
        modifier = modifier,
        label = "badge"
    ) { (g, text, color) ->
        Row(
            modifier = Modifier
                .background(Color.White.copy(alpha = 0.16f), CircleShape)
                .border(1.dp, Color.White.copy(alpha = 0.30f), CircleShape)
                .padding(start = 5.dp, end = 14.dp, top = 5.dp, bottom = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(26.dp)
                    .drawBehind {
                        if (!paused) {
                            val p = pulse.value
                            drawCircle(
                                color = color,
                                radius = size.minDimension / 2f + 7.dp.toPx() * p,
                                alpha = 0.45f * (1f - p)
                            )
                        }
                    }
                    .background(color, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Glyph(g, Color(0xFF0B1730), size = 15.dp, strokeWidth = 2.2f)
            }
            Spacer(Modifier.width(9.dp))
            Text(text, style = MaterialTheme.typography.labelLarge, color = Color.White)
        }
    }
}
