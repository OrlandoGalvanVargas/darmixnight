package com.darmix.darmixnight.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.darmix.darmixnight.ui.components.Crescent
import com.darmix.darmixnight.ui.components.buildStars
import com.darmix.darmixnight.ui.theme.DnType
import com.darmix.darmixnight.ui.theme.IntroHorizon
import com.darmix.darmixnight.ui.theme.IntroSkyMid
import com.darmix.darmixnight.ui.theme.IntroSkyTop
import com.darmix.darmixnight.ui.theme.NightBg
import com.darmix.darmixnight.ui.theme.NightGlow
import com.darmix.darmixnight.ui.theme.NightText
import com.darmix.darmixnight.ui.theme.NightTextDim
import kotlinx.coroutines.launch
import kotlin.math.hypot
import kotlin.math.sin

private const val INTRO_MILLIS = 2200
private const val EXIT_START = 0.80f
private val SPLASH_ICON_SIZE = 288.dp
private val MOON_LIFT = 76.dp
private const val MOON_END_SCALE = 0.6f

private val Smooth = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)
private val Glide = CubicBezierEasing(0.65f, 0f, 0.35f, 1f)

private fun segment(p: Float, start: Float, end: Float): Float =
    ((p - start) / (end - start)).coerceIn(0f, 1f)

@Composable
fun NightIntro(
    start: Boolean,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val stars = remember { buildStars(count = 80) }
    val finish by rememberUpdatedState(onFinished)
    var done by remember { mutableStateOf(false) }
    var skipping by remember { mutableStateOf(false) }

    fun complete() {
        if (!done) {
            done = true
            finish()
        }
    }

    LaunchedEffect(start) {
        if (!start) return@LaunchedEffect
        progress.animateTo(1f, tween(INTRO_MILLIS, easing = LinearEasing))
        complete()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .semantics { contentDescription = "DarmixNight" }
            .pointerInput(start) {
                detectTapGestures {
                    if (start && !skipping) {
                        skipping = true
                        scope.launch {
                            if (progress.value < EXIT_START) progress.snapTo(EXIT_START)
                            val remaining = ((1f - progress.value) * INTRO_MILLIS).toInt()
                            progress.animateTo(1f, tween(remaining, easing = LinearEasing))
                            complete()
                        }
                    }
                }
            }
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                val reveal = Glide.transform(segment(progress.value, EXIT_START, 1f))
                if (reveal > 0f) {
                    val lift = MOON_LIFT.toPx()
                    val center = Offset(size.width / 2f, size.height / 2f - lift)
                    val maxRadius = hypot(size.width, size.height)
                    val radius = maxRadius * reveal
                    drawCircle(
                        color = Color.Black,
                        radius = radius,
                        center = center,
                        blendMode = BlendMode.Clear
                    )
                    drawCircle(
                        color = NightGlow,
                        radius = radius,
                        center = center,
                        alpha = (1f - reveal) * 0.9f,
                        style = Stroke(width = 2.dp.toPx() + 10.dp.toPx() * (1f - reveal))
                    )
                }
            }
    ) {
        Spacer(
            Modifier
                .fillMaxSize()
                .drawWithCache {
                    val moon = Crescent.unitPath()
                    val sky = Brush.verticalGradient(
                        0f to IntroSkyTop,
                        0.55f to IntroSkyMid,
                        0.86f to NightBg,
                        1f to IntroHorizon
                    )
                    val iconPx = SPLASH_ICON_SIZE.toPx()
                    val moonRadius = iconPx * (Crescent.OUTER_RADIUS / Crescent.VIEWPORT)
                    val lift = MOON_LIFT.toPx()
                    val trail = 90.dp.toPx()
                    onDrawBehind {
                        val p = progress.value
                        val w = size.width
                        val h = size.height

                        drawRect(NightBg)
                        drawRect(sky, alpha = Smooth.transform(segment(p, 0f, 0.4f)))

                        stars.forEach { s ->
                            val appear = segment(p, s.appearAt, s.appearAt + 0.16f)
                            if (appear > 0f) {
                                val twinkle = 0.55f + 0.45f * sin(p * 16f + s.phase)
                                drawCircle(
                                    color = if (s.warm) NightGlow else Color.White,
                                    radius = s.radiusDp * density,
                                    center = Offset(s.x * w, s.y * h),
                                    alpha = appear * twinkle * 0.9f
                                )
                            }
                        }

                        val shoot = segment(p, 0.34f, 0.58f)
                        if (shoot > 0f && shoot < 1f) {
                            val e = Glide.transform(shoot)
                            val head = Offset(w * (0.92f - 0.62f * e), h * (0.06f + 0.2f * e))
                            val tail = Offset(head.x + trail * 0.94f, head.y - trail * 0.34f)
                            val fade = sin(shoot * Math.PI).toFloat()
                            drawLine(
                                brush = Brush.linearGradient(
                                    listOf(Color.White.copy(alpha = 0.95f * fade), Color.Transparent),
                                    start = head,
                                    end = tail
                                ),
                                start = head,
                                end = tail,
                                strokeWidth = 1.6.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        }

                        val move = Glide.transform(segment(p, 0.3f, 0.66f))
                        val center = Offset(w / 2f, h / 2f - lift * move)
                        val radius = moonRadius * (1f - (1f - MOON_END_SCALE) * move)

                        val halo = Smooth.transform(segment(p, 0.08f, 0.5f))
                        if (halo > 0f) {
                            val pulse = 1f + 0.05f * sin(p * 9f)
                            val haloRadius = radius * (1.6f + 1.4f * halo) * pulse
                            drawCircle(
                                brush = Brush.radialGradient(
                                    0f to NightGlow.copy(alpha = 0.34f * halo),
                                    0.45f to NightGlow.copy(alpha = 0.10f * halo),
                                    1f to Color.Transparent,
                                    center = center,
                                    radius = haloRadius
                                ),
                                radius = haloRadius,
                                center = center
                            )
                        }

                        translate(center.x, center.y) {
                            rotate(degrees = -10f * move, pivot = Offset.Zero) {
                                scale(radius, radius, pivot = Offset.Zero) {
                                    drawPath(moon, NightGlow)
                                }
                            }
                        }
                    }
                }
        )

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = 34.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                modifier = Modifier.graphicsLayer {
                    val e = Smooth.transform(segment(progress.value, 0.46f, 0.7f))
                    alpha = e
                    translationY = (1f - e) * 22.dp.toPx()
                }
            ) {
                Text("Darmix", style = DnType.wordmark, color = NightText)
                Text("Night", style = DnType.wordmark, color = NightGlow)
            }
            Spacer(Modifier.height(14.dp))
            Box(
                Modifier
                    .width(56.dp)
                    .height(2.dp)
                    .graphicsLayer {
                        scaleX = Smooth.transform(segment(progress.value, 0.56f, 0.76f))
                    }
                    .background(NightGlow.copy(alpha = 0.7f), RoundedCornerShape(1.dp))
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = "Apaga la pantalla. Enciende el descanso.",
                style = MaterialTheme.typography.bodyMedium,
                color = NightTextDim,
                modifier = Modifier.graphicsLayer {
                    val e = Smooth.transform(segment(progress.value, 0.6f, 0.78f))
                    alpha = e
                    translationY = (1f - e) * 12.dp.toPx()
                }
            )
        }
    }
}
