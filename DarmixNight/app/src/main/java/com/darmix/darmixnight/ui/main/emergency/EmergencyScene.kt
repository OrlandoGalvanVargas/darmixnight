package com.darmix.darmixnight.ui.main.emergency

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.imageResource
import com.darmix.darmixnight.R
import com.darmix.darmixnight.ui.components.SceneLampCore
import com.darmix.darmixnight.ui.components.SceneLampLight
import com.darmix.darmixnight.ui.components.drawGlow
import com.darmix.darmixnight.ui.components.drawRoom
import com.darmix.darmixnight.ui.components.lampFlicker
import com.darmix.darmixnight.ui.components.rememberSceneClock
import com.darmix.darmixnight.ui.components.unitRadial
import com.darmix.darmixnight.ui.main.night.NightMode
import kotlin.math.PI
import kotlin.math.sin

private const val ROOM_W = 1440f
private const val ROOM_H = 1800f
private const val GRAIN_COUNT = 10
private const val GRAIN_FALL_SECONDS = 1.4f
private const val STREAK_COUNT = 5
private const val STREAK_SECONDS = 1.1f

private val Lamp = Offset(372f, 478f)
private val Neck = Offset(1095f, 664f)
private val Pile = Offset(1095f, 832f)
private val PileGlow = Offset(1095f, 872f)

private val WarmSand = Color(0xFFFFF1DC)
private val MoonSand = Color(0xFFF4F0EA)

@Composable
internal fun EmergencyScene(mode: NightMode, modifier: Modifier = Modifier) {
    val litAlpha by animateFloatAsState(if (mode == NightMode.BEFORE) 1f else 0f, tween(1400), label = "lit")
    val pausedAlpha by animateFloatAsState(if (mode == NightMode.PAUSED) 1f else 0f, tween(1400), label = "paused")
    val live by animateFloatAsState(if (mode == NightMode.PAUSED) 0f else 1f, tween(1400), label = "live")

    val dark = ImageBitmap.imageResource(R.drawable.emergency_room_dark)
    val lit = if (mode == NightMode.BEFORE || litAlpha > 0f) ImageBitmap.imageResource(R.drawable.emergency_room_lit) else null
    val still = if (mode == NightMode.PAUSED || pausedAlpha > 0f) ImageBitmap.imageResource(R.drawable.emergency_room_paused) else null
    val clock = if (mode != NightMode.PAUSED || live > 0f) rememberSceneClock() else null

    Box(modifier) {
        Spacer(
            Modifier
                .fillMaxSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawBehind {
                    drawRoom(dark, ROOM_H / ROOM_W, lit to litAlpha, still to pausedAlpha)
                }
        )
        if (clock != null) {
            SandLife(
                clock = clock,
                lamp = { litAlpha },
                live = { live },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun SandLife(
    clock: State<Float>,
    lamp: () -> Float,
    live: () -> Float,
    modifier: Modifier = Modifier
) {
    Spacer(
        modifier.drawWithCache {
            val unit = size.width / ROOM_W
            val lampGlow = unitRadial(0f to SceneLampLight, 0.45f to SceneLampLight.copy(alpha = 0.35f), 1f to SceneLampLight.copy(alpha = 0f))
            val lampCore = unitRadial(0f to SceneLampCore, 1f to SceneLampCore.copy(alpha = 0f))
            val warmGrain = unitRadial(0f to WarmSand, 0.4f to WarmSand.copy(alpha = 0.4f), 1f to WarmSand.copy(alpha = 0f))
            val moonGrain = unitRadial(0f to MoonSand, 0.4f to MoonSand.copy(alpha = 0.35f), 1f to MoonSand.copy(alpha = 0f))
            val warmPile = unitRadial(0f to WarmSand.copy(alpha = 0.8f), 1f to WarmSand.copy(alpha = 0f))
            val moonPile = unitRadial(0f to MoonSand.copy(alpha = 0.6f), 1f to MoonSand.copy(alpha = 0f))
            onDrawBehind {
                val t = clock.value
                val alive = live()
                if (alive <= 0f) return@onDrawBehind
                val warm = lamp()
                val lampLevel = warm * alive
                if (lampLevel > 0f) {
                    val flicker = lampFlicker(t)
                    drawGlow(lampGlow, Lamp * unit, 420f * unit, 380f * unit, lampLevel * (0.04f + 0.1f * flicker))
                    drawGlow(lampCore, Lamp * unit, 150f * unit, 70f * unit, lampLevel * (0.05f + 0.08f * flicker))
                }
                val pulse = 0.7f + 0.3f * sin(t * 1.3f)
                drawGlow(warmPile, PileGlow * unit, 120f * unit, 34f * unit, warm * alive * 0.22f * pulse)
                drawGlow(moonPile, PileGlow * unit, 120f * unit, 34f * unit, (1f - warm) * alive * 0.16f * pulse)
                val sandTone = if (warm >= 0.5f) WarmSand else MoonSand
                val streakLevel = alive * (0.55f + 0.2f * warm)
                for (i in 0 until STREAK_COUNT) {
                    val u = ((t / STREAK_SECONDS) + i.toFloat() / STREAK_COUNT) % 1f
                    val y = Neck.y + (Pile.y - Neck.y) * u * (0.6f + 0.4f * u)
                    val edge = sin(PI.toFloat() * u)
                    val top = Offset(Neck.x, (y - 16f).coerceAtLeast(Neck.y)) * unit
                    val bottom = Offset(Neck.x, (y + 16f).coerceAtMost(Pile.y)) * unit
                    drawLine(
                        Brush.verticalGradient(
                            0f to sandTone.copy(alpha = 0f),
                            0.5f to sandTone,
                            1f to sandTone.copy(alpha = 0f),
                            startY = top.y,
                            endY = bottom.y
                        ),
                        start = top,
                        end = bottom,
                        strokeWidth = 2.8f * unit,
                        cap = StrokeCap.Round,
                        alpha = streakLevel * edge,
                        blendMode = BlendMode.Screen
                    )
                }
                for (i in 0 until GRAIN_COUNT) {
                    val u = ((t / GRAIN_FALL_SECONDS) + i.toFloat() / GRAIN_COUNT) % 1f
                    val fall = u * (0.4f + 0.6f * u)
                    val y = Neck.y + (Pile.y - Neck.y) * fall
                    val drift = sin(t * 5f + i * 1.7f) * 1.6f
                    val center = Offset(Neck.x + drift, y) * unit
                    val fade = sin(PI.toFloat() * u)
                    val r = (3.2f + (i % 3) * 0.6f) * unit
                    drawGlow(warmGrain, center, r, r * 1.4f, warm * alive * 0.85f * fade)
                    drawGlow(moonGrain, center, r, r * 1.4f, (1f - warm) * alive * 0.7f * fade)
                }
            }
        }
    )
}
