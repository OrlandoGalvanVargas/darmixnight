package com.darmix.darmixnight.ui.main.settings

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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.imageResource
import com.darmix.darmixnight.R
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
import com.darmix.darmixnight.ui.main.night.NightMode
import kotlin.math.sin

private const val ROOM_W = 1440f
private const val ROOM_H = 1800f

private val Moon = Offset(812f, 522f)
private val LeftCurtain = Offset(210f, 760f)
private val RightCurtain = Offset(1230f, 760f)
private val Sill = Offset(780f, 1104f)
private val TreeSway = SceneSway(Rect(556f, 680f, 1003f, 1100f), amplitude = 3f, speed = 0.6f)
private val Shooting = ShootingSky(
    paths = arrayOf(
        Offset(990f, 170f) to Offset(840f, 250f),
        Offset(985f, 400f) to Offset(930f, 520f),
        Offset(680f, 330f) to Offset(600f, 440f),
        Offset(870f, 150f) to Offset(760f, 230f),
        Offset(720f, 600f) to Offset(590f, 680f)
    ),
    period = 3f,
    start = 0.6f,
    length = 0.9f,
    width = 2f
)

private val Stars = floatArrayOf(
    641f, 391f, 783f, 330f, 901f, 198f, 956f, 484f, 605f, 557f, 839f, 249f, 634f, 480f, 728f, 335f,
    862f, 385f, 640f, 592f, 636f, 647f, 948f, 584f, 682f, 401f, 972f, 326f, 783f, 682f, 624f, 751f,
    723f, 702f, 682f, 763f, 933f, 307f, 904f, 296f, 707f, 813f, 778f, 759f, 921f, 606f, 736f, 765f,
    638f, 704f, 698f, 743f, 607f, 799f, 925f, 524f, 743f, 657f, 667f, 676f, 941f, 361f, 640f, 766f,
    669f, 668f, 968f, 543f, 756f, 818f, 712f, 373f
)

private val MoonLight = Color(0xFFFFE6CC)
private val MoonBeam = Color(0xFFFFD9B0)

@Composable
internal fun SettingsScene(mode: NightMode, modifier: Modifier = Modifier) {
    val litAlpha by animateFloatAsState(if (mode == NightMode.BEFORE) 1f else 0f, tween(1400), label = "lit")
    val pausedAlpha by animateFloatAsState(if (mode == NightMode.PAUSED) 1f else 0f, tween(1400), label = "paused")
    val beamAlpha by animateFloatAsState(if (mode == NightMode.BLOCKING) 1f else 0f, tween(1800), label = "beam")
    val live by animateFloatAsState(if (mode == NightMode.PAUSED) 0f else 1f, tween(1400), label = "live")

    val dark = ImageBitmap.imageResource(R.drawable.settings_window_dark)
    val lit = if (mode == NightMode.BEFORE || litAlpha > 0f) ImageBitmap.imageResource(R.drawable.settings_window_lit) else null
    val still = if (mode == NightMode.PAUSED || pausedAlpha > 0f) ImageBitmap.imageResource(R.drawable.settings_window_paused) else null
    val clock = rememberSceneClock()
    val shader = rememberSwayShader()

    Box(modifier) {
        Spacer(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    compositingStrategy = CompositingStrategy.Offscreen
                    applySway(shader, TreeSway, { clock.value }, ROOM_W, live)
                }
                .drawBehind {
                    drawRoom(dark, ROOM_H / ROOM_W, lit to litAlpha, still to pausedAlpha)
                }
        )
        WindowLife(
            clock = clock,
            lamp = { litAlpha },
            beam = { beamAlpha },
            live = { live },
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun WindowLife(
    clock: State<Float>,
    lamp: () -> Float,
    beam: () -> Float,
    live: () -> Float,
    modifier: Modifier = Modifier
) {
    Spacer(
        modifier.drawWithCache {
            val unit = size.width / ROOM_W
            val curtainGlow = unitRadial(0f to SceneLampLight, 0.5f to SceneLampLight.copy(alpha = 0.3f), 1f to SceneLampLight.copy(alpha = 0f))
            val moonGlow = unitRadial(0.3f to MoonLight.copy(alpha = 0.7f), 1f to MoonLight.copy(alpha = 0f))
            val starGlow = unitRadial(0f to SceneStarLight, 0.35f to SceneStarLight.copy(alpha = 0.45f), 1f to SceneStarLight.copy(alpha = 0f))
            val beamGlow = unitRadial(0f to MoonBeam, 0.55f to MoonBeam.copy(alpha = 0.4f), 1f to MoonBeam.copy(alpha = 0f))
            onDrawBehind {
                val t = clock.value
                val alive = live()
                val lampLevel = lamp() * alive
                if (lampLevel > 0f) {
                    val flicker = lampFlicker(t)
                    val level = lampLevel * (0.03f + 0.07f * flicker)
                    drawGlow(curtainGlow, LeftCurtain * unit, 300f * unit, 560f * unit, level)
                    drawGlow(curtainGlow, RightCurtain * unit, 300f * unit, 560f * unit, level * 0.8f)
                }
                val beamLevel = beam() * alive
                if (beamLevel > 0f) {
                    val breath = 0.78f + 0.22f * sin(t * 0.9f)
                    drawGlow(beamGlow, Sill * unit, 260f * unit, 22f * unit, beamLevel * 0.22f * breath)
                    drawGlow(beamGlow, Offset(720f, 1170f) * unit, 330f * unit, 50f * unit, beamLevel * 0.12f * breath)
                }
                drawGlow(moonGlow, Moon * unit, 170f * unit, 170f * unit, alive * (0.08f + 0.06f * sin(t * 1.05f + 1f)))
                drawTwinkles(starGlow, Stars, t, unit, 1f, 3.4f)
                drawShootingStar(Shooting, t, unit, 1f)
            }
        }
    )
}
