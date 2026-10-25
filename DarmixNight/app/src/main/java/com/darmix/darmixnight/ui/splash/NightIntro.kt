package com.darmix.darmixnight.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.darmix.darmixnight.ui.components.SkyBackdrop
import com.darmix.darmixnight.ui.panda.PandaMood
import com.darmix.darmixnight.ui.panda.PandaMotion
import com.darmix.darmixnight.ui.panda.drawPanda
import com.darmix.darmixnight.ui.theme.DarmixNightTheme
import com.darmix.darmixnight.ui.theme.DnType
import com.darmix.darmixnight.ui.theme.NightBg
import com.darmix.darmixnight.ui.theme.NightGlow
import com.darmix.darmixnight.ui.theme.NightText
import com.darmix.darmixnight.ui.theme.NightTextDim
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt

private const val INTRO_MILLIS = 3000
private const val INTRO_SECONDS = INTRO_MILLIS / 1000f
private const val EXIT_START = 0.86f
private const val WORD = "DarmixNight"

private val Smooth = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)
private val Glide = CubicBezierEasing(0.65f, 0f, 0.35f, 1f)

private val MoonTop = Color(0xFFFFF4D6)
private val MoonMid = Color(0xFFF0D49C)
private val MoonLow = Color(0xFFC99A55)
private val PandaRim = Color(0xFF4A5680)

private val StartBiteAngle = atan2(-8.5f, 9.5f)
private const val END_BITE_ANGLE = -1.5707964f
private const val START_BITE_DISTANCE = 0.531f
private const val END_BITE_DISTANCE = 0.41f
private const val START_BITE_RADIUS = 0.833f
private const val END_BITE_RADIUS = 1f

private fun segment(p: Float, start: Float, end: Float): Float =
    ((p - start) / (end - start)).coerceIn(0f, 1f)

private class CradleState(
    val center: Offset,
    val radius: Float,
    val biteOffset: Offset,
    val biteRadius: Float,
    val morph: Float,
    val sway: Float,
    val pivot: Offset
)

private fun cradleState(p: Float, width: Float, height: Float, density: Density): CradleState {
    val u = density.density
    val m = Glide.transform(segment(p, 0.12f, 0.42f))
    val radius = (64f + (104f - 64f) * m) * u
    val center = Offset(width / 2f, height / 2f - 34f * u * m)
    val angle = StartBiteAngle + (END_BITE_ANGLE - StartBiteAngle) * m
    val distance = (START_BITE_DISTANCE + (END_BITE_DISTANCE - START_BITE_DISTANCE) * m) * radius
    val biteRadius = (START_BITE_RADIUS + (END_BITE_RADIUS - START_BITE_RADIUS) * m) * radius
    val seconds = p * INTRO_SECONDS
    val sway = 3.5f * sin(seconds * 2.2f) * segment(p, 0.42f, 0.6f)
    return CradleState(
        center = center,
        radius = radius,
        biteOffset = Offset(distance * cos(angle), distance * sin(angle)),
        biteRadius = biteRadius,
        morph = m,
        sway = sway,
        pivot = Offset(center.x, center.y - 220f * u)
    )
}

@Composable
fun NightIntro(
    start: Boolean,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
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

    DarmixNightTheme(darkTheme = true) {
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
                .graphicsLayer { alpha = 1f - segment(progress.value, 0.93f, 1f) }
                .drawWithContent {
                    drawContent()
                    val exit = Glide.transform(segment(progress.value, EXIT_START, 1f))
                    if (exit > 0f) {
                        val state = cradleState(progress.value, size.width, size.height, this)
                        val reach = (hypot(size.width, size.height) * 1.15f * exit).coerceAtLeast(1f)
                        drawCircle(
                            Brush.radialGradient(
                                0f to MoonTop.copy(alpha = 0.98f),
                                0.65f to NightGlow.copy(alpha = 0.9f),
                                1f to NightGlow.copy(alpha = 0f),
                                center = state.center,
                                radius = reach
                            ),
                            radius = reach,
                            center = state.center
                        )
                    }
                }
        ) {
            Spacer(
                Modifier
                    .fillMaxSize()
                    .background(NightBg)
            )
            SkyBackdrop(
                Modifier.graphicsLayer { alpha = Smooth.transform(segment(progress.value, 0f, 0.3f)) }
            ) {}
            Spacer(
                Modifier
                    .fillMaxSize()
                    .drawBehind { drawMoonCradle(progress.value) }
            )
            Spacer(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = segment(progress.value, 0.42f, 0.48f) }
                    .drawBehind { drawFallingPanda(progress.value) }
            )
            Wordmark(progress)
        }
    }
}

private fun DrawScope.drawMoonCradle(p: Float) {
    val u = density
    val s = cradleState(p, size.width, size.height, this)
    val c = s.center
    val glow = 0.35f + 0.4f * s.morph
    val haloRadius = s.radius * 2.4f
    drawCircle(
        Brush.radialGradient(
            0f to NightGlow.copy(alpha = 0.6f * glow),
            0.45f to NightGlow.copy(alpha = 0.18f * glow),
            1f to Color.Transparent,
            center = c,
            radius = haloRadius
        ),
        radius = haloRadius,
        center = c
    )
    rotate(s.sway, s.pivot) {
        val hang = segment(p, 0.36f, 0.5f)
        if (hang > 0f) {
            val half = s.biteOffset.y / 2f
            val hornX = sqrt((s.radius * s.radius - half * half).coerceAtLeast(0f))
            for (hx in floatArrayOf(c.x - hornX, c.x + hornX)) {
                drawLine(
                    NightGlow.copy(alpha = 0.35f),
                    Offset(hx, c.y + half),
                    Offset(hx, c.y + half - 260f * u * hang),
                    strokeWidth = 1f * u,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(2f * u, 4f * u))
                )
            }
        }
        val outer = Path().apply { addOval(Rect(c, s.radius)) }
        val bite = Path().apply { addOval(Rect(Offset(c.x + s.biteOffset.x, c.y + s.biteOffset.y), s.biteRadius)) }
        val crescent = Path().apply { op(outer, bite, PathOperation.Difference) }
        val gold = segment(s.morph, 0.4f, 0.8f)
        if (gold < 1f) drawPath(crescent, NightGlow, alpha = 1f - gold)
        if (gold > 0f) {
            drawPath(
                crescent,
                Brush.verticalGradient(
                    0f to MoonTop,
                    0.6f to MoonMid,
                    1f to MoonLow,
                    startY = c.y - s.radius,
                    endY = c.y + s.radius
                ),
                alpha = gold
            )
            val k = s.radius / (112f * u)
            floatArrayOf(-62f, 26f, 8f, -26f, 56f, 5f, 36f, 60f, 7f, 70f, 26f, 4f).toList().chunked(3).forEach { (dx, dy, r) ->
                drawCircle(MoonLow, r * u * k, Offset(c.x + dx * u * k, c.y + dy * u * k), alpha = 0.35f * gold)
            }
        }
    }
}

private fun DrawScope.drawFallingPanda(p: Float) {
    val drop = segment(p, 0.42f, 0.62f)
    if (drop <= 0f) return
    val u = density
    val s = cradleState(p, size.width, size.height, this)
    val c = s.center
    val eased = 1f - (1f - drop) * (1f - drop)
    val bounce = sin(drop * Math.PI.toFloat() * 3f) * (1f - drop) * 14f * u
    val pandaScale = 0.86f * u
    val rest = c.y + s.radius + s.biteOffset.y / 2f - 194f * pandaScale + 6f * u
    val top = rest - 260f * u * (1f - eased) - bounce
    rotate(s.sway, s.pivot) {
        translate(c.x - 106f * pandaScale, top) {
            scale(pandaScale, pandaScale, Offset.Zero) {
                drawPanda(PandaMood.SLEEP, PandaMotion(p * INTRO_SECONDS / 2f, 0f, 0f, 0f, PandaRim))
            }
        }
    }
}

@Composable
private fun Wordmark(progress: Animatable<Float, *>) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.offset(y = 186.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row {
                WORD.forEachIndexed { i, ch ->
                    Text(
                        ch.toString(),
                        style = DnType.wordmark,
                        color = if (i < 6) NightText else NightGlow,
                        modifier = Modifier.graphicsLayer {
                            val write = segment(progress.value, 0.55f, 0.78f)
                            val k = Smooth.transform(segment(write, i / 14f, i / 14f + 0.35f))
                            alpha = k
                            translationY = (1f - k) * 16.dp.toPx()
                        }
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Box(
                Modifier
                    .width(56.dp)
                    .height(2.dp)
                    .graphicsLayer { scaleX = Smooth.transform(segment(progress.value, 0.72f, 0.84f)) }
                    .background(NightGlow.copy(alpha = 0.7f), RoundedCornerShape(1.dp))
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Apaga la pantalla. Enciende el descanso.",
                style = MaterialTheme.typography.bodyMedium,
                color = NightTextDim,
                modifier = Modifier.graphicsLayer {
                    val e = Smooth.transform(segment(progress.value, 0.72f, 0.84f))
                    alpha = e
                    translationY = (1f - e) * 10.dp.toPx()
                }
            )
        }
    }
}
