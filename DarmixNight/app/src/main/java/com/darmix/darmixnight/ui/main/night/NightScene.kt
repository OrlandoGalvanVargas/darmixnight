package com.darmix.darmixnight.ui.main.night

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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.imageResource
import com.darmix.darmixnight.R
import com.darmix.darmixnight.ScheduleUtils
import com.darmix.darmixnight.ui.components.SceneLampCore
import com.darmix.darmixnight.ui.components.SceneLampLight
import com.darmix.darmixnight.ui.components.SceneStarLight
import com.darmix.darmixnight.ui.components.SceneSway
import com.darmix.darmixnight.ui.components.ShootingSky
import com.darmix.darmixnight.ui.components.applySway
import com.darmix.darmixnight.ui.components.drawGlow
import com.darmix.darmixnight.ui.components.drawRoom
import com.darmix.darmixnight.ui.components.drawShootingStar
import com.darmix.darmixnight.ui.components.drawTwinkles
import com.darmix.darmixnight.ui.components.lampFlicker
import com.darmix.darmixnight.ui.components.rememberSceneClock
import com.darmix.darmixnight.ui.components.rememberSwayShader
import com.darmix.darmixnight.ui.components.unitRadial
import kotlin.math.sin

internal enum class NightMode { BEFORE, BLOCKING, PAUSED }

internal fun nightModeOf(nowMinutes: Int, startMinutes: Int, endMinutes: Int, paused: Boolean): NightMode = when {
    paused -> NightMode.PAUSED
    ScheduleUtils.isWithinBlockRange(nowMinutes, startMinutes, endMinutes) -> NightMode.BLOCKING
    else -> NightMode.BEFORE
}

private const val ROOM_W = 1122f
private const val ROOM_H = 1402f

private val Lamp = Offset(240f, 492f)
private val Moon = Offset(952f, 258f)
private val PlantSway = SceneSway(Rect(10f, 340f, 200f, 556f), amplitude = 4.2f)
private val Shooting = ShootingSky(
    paths = arrayOf(
        Offset(1034f, 104f) to Offset(884f, 178f),
        Offset(1030f, 292f) to Offset(880f, 332f),
        Offset(930f, 140f) to Offset(846f, 290f),
        Offset(866f, 118f) to Offset(1004f, 168f),
        Offset(990f, 80f) to Offset(852f, 196f)
    ),
    period = 3f,
    start = 0.6f,
    length = 0.85f,
    width = 1.3f
)

private val Stars = floatArrayOf(
    967f, 114f, 908f, 160f, 979f, 186f, 937f, 156f, 883f, 386f, 859f, 324f, 853f, 379f, 1028f, 237f,
    884f, 194f, 858f, 236f, 1000f, 86f, 862f, 188f, 843f, 276f, 861f, 295f, 1024f, 291f, 1037f, 154f,
    1010f, 302f, 1001f, 138f, 937f, 342f, 905f, 353f, 1012f, 259f, 860f, 354f, 935f, 383f, 1016f, 144f,
    912f, 387f, 875f, 339f, 916f, 329f, 892f, 374f, 1035f, 269f, 862f, 387f
)

private val MoonLight = Color(0xFFFFE6CC)
private val MoonBeam = Color(0xFFFFD9B0)

@Composable
internal fun NightScene(mode: NightMode, modifier: Modifier = Modifier) {
    val litAlpha by animateFloatAsState(if (mode == NightMode.BEFORE) 1f else 0f, tween(1400), label = "lit")
    val pausedAlpha by animateFloatAsState(if (mode == NightMode.PAUSED) 1f else 0f, tween(1400), label = "paused")
    val beamAlpha by animateFloatAsState(if (mode == NightMode.BLOCKING) 1f else 0f, tween(1800), label = "beam")
    val live by animateFloatAsState(if (mode == NightMode.PAUSED) 0f else 1f, tween(1400), label = "live")

    val dark = ImageBitmap.imageResource(R.drawable.night_room_dark)
    val lit = if (mode == NightMode.BEFORE || litAlpha > 0f) ImageBitmap.imageResource(R.drawable.night_room_lit) else null
    val still = if (mode == NightMode.PAUSED || pausedAlpha > 0f) ImageBitmap.imageResource(R.drawable.night_room_paused) else null
    val clock = rememberSceneClock()
    val shader = rememberSwayShader()

    Box(modifier) {
        Spacer(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    compositingStrategy = CompositingStrategy.Offscreen
                    applySway(shader, PlantSway, { clock.value }, ROOM_W, live)
                }
                .drawBehind {
                    drawRoom(dark, ROOM_H / ROOM_W, lit to litAlpha, still to pausedAlpha)
                }
        )
        SceneLife(
            clock = clock,
            lamp = { litAlpha },
            beam = { beamAlpha },
            live = { live },
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun SceneLife(
    clock: State<Float>,
    lamp: () -> Float,
    beam: () -> Float,
    live: () -> Float,
    modifier: Modifier = Modifier
) {
    Spacer(
        modifier.drawWithCache {
            val unit = size.width / ROOM_W
            val lampGlow = unitRadial(0f to SceneLampLight, 0.45f to SceneLampLight.copy(alpha = 0.35f), 1f to SceneLampLight.copy(alpha = 0f))
            val lampCore = unitRadial(0f to SceneLampCore, 1f to SceneLampCore.copy(alpha = 0f))
            val moonGlow = unitRadial(0.3f to MoonLight.copy(alpha = 0.7f), 1f to MoonLight.copy(alpha = 0f))
            val starGlow = unitRadial(0f to SceneStarLight, 0.35f to SceneStarLight.copy(alpha = 0.45f), 1f to SceneStarLight.copy(alpha = 0f))
            val beamGlow = unitRadial(0f to MoonBeam, 0.55f to MoonBeam.copy(alpha = 0.4f), 1f to MoonBeam.copy(alpha = 0f))
            onDrawBehind {
                val t = clock.value
                val alive = live()
                val lampLevel = lamp() * alive
                if (lampLevel > 0f) {
                    val flicker = lampFlicker(t)
                    drawGlow(lampGlow, Lamp * unit, 340f * unit, 300f * unit, lampLevel * (0.04f + 0.1f * flicker))
                    drawGlow(lampCore, Lamp * unit, 110f * unit, 60f * unit, lampLevel * (0.05f + 0.08f * flicker))
                }
                val beamLevel = beam() * alive
                if (beamLevel > 0f) {
                    val breath = 0.78f + 0.22f * sin(t * 0.9f)
                    rotate(-9f, Offset(720f, 700f) * unit) {
                        drawGlow(beamGlow, Offset(720f, 700f) * unit, 430f * unit, 105f * unit, beamLevel * 0.16f * breath)
                    }
                    drawGlow(beamGlow, Offset(610f, 612f) * unit, 270f * unit, 52f * unit, beamLevel * 0.13f * breath)
                    drawGlow(beamGlow, Offset(934f, 566f) * unit, 150f * unit, 18f * unit, beamLevel * 0.2f * breath)
                }
                drawGlow(moonGlow, Moon * unit, 118f * unit, 118f * unit, alive * (0.08f + 0.06f * sin(t * 1.05f + 1f)))
                drawTwinkles(starGlow, Stars, t, unit, 1f, 2.6f)
                drawShootingStar(Shooting, t, unit, 1f)
            }
        }
    )
}

