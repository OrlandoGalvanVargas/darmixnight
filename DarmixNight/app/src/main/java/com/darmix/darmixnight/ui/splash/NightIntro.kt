package com.darmix.darmixnight.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.darmix.darmixnight.ui.components.SkyBackdrop
import com.darmix.darmixnight.ui.main.night.NightMode
import com.darmix.darmixnight.ui.main.night.NightScene
import com.darmix.darmixnight.ui.theme.BrasaBg
import com.darmix.darmixnight.ui.theme.BrasaClayDim
import com.darmix.darmixnight.ui.theme.BrasaCream
import com.darmix.darmixnight.ui.theme.BrasaEmber
import com.darmix.darmixnight.ui.theme.DarmixNightTheme
import com.darmix.darmixnight.ui.theme.DnType
import kotlinx.coroutines.launch

private const val INTRO_MILLIS = 3200
private const val LAMP_OFF_AT = 0.16f
private const val EXIT_START = 0.84f
private const val ROOM_ASPECT = 1402f / 1122f
private const val SCENE_SHARE = 0.66f

private val Smooth = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)

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
    val finish by rememberUpdatedState(onFinished)
    var done by remember { mutableStateOf(false) }
    var skipping by remember { mutableStateOf(false) }
    var lampOff by remember { mutableStateOf(false) }

    fun complete() {
        if (!done) {
            done = true
            finish()
        }
    }

    LaunchedEffect(start) {
        if (!start) return@LaunchedEffect
        launch {
            progress.animateTo(LAMP_OFF_AT, tween((INTRO_MILLIS * LAMP_OFF_AT).toInt(), easing = LinearEasing))
            lampOff = true
        }.join()
        progress.animateTo(1f, tween((INTRO_MILLIS * (1f - LAMP_OFF_AT)).toInt(), easing = LinearEasing))
        complete()
    }

    DarmixNightTheme {
        BoxWithConstraints(
            modifier = modifier
                .fillMaxSize()
                .background(BrasaBg)
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
                .graphicsLayer {
                    val p = progress.value
                    alpha = Smooth.transform(segment(p, 0f, 0.12f)) * (1f - segment(p, EXIT_START, 1f))
                }
        ) {
            val sceneWidth = maxOf(maxWidth, maxHeight * SCENE_SHARE / ROOM_ASPECT)
            SkyBackdrop(Modifier.fillMaxSize()) {
                NightScene(
                    mode = if (lampOff) NightMode.BLOCKING else NightMode.BEFORE,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .requiredWidth(sceneWidth)
                        .fillMaxHeight()
                )
            }
            Wordmark(
                progress = progress,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = maxHeight * 0.17f)
            )
        }
    }
}

@Composable
private fun Wordmark(progress: Animatable<Float, *>, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.graphicsLayer {
                val k = Smooth.transform(segment(progress.value, 0.38f, 0.58f))
                alpha = k
                translationY = (1f - k) * 14.dp.toPx()
            }
        ) {
            Text(
                "Darmix",
                style = DnType.brand.copy(fontSize = 34.sp, lineHeight = 44.sp),
                color = BrasaCream,
                modifier = Modifier.alignByBaseline()
            )
            Text(
                "Night",
                style = DnType.brandAccent.copy(fontSize = 42.sp, lineHeight = 44.sp),
                color = BrasaEmber,
                modifier = Modifier
                    .alignByBaseline()
                    .padding(start = 1.dp)
            )
        }
        Spacer(Modifier.height(12.dp))
        Spacer(
            Modifier
                .width(72.dp)
                .height(1.dp)
                .graphicsLayer { scaleX = Smooth.transform(segment(progress.value, 0.5f, 0.66f)) }
                .background(
                    Brush.horizontalGradient(
                        listOf(BrasaEmber.copy(alpha = 0f), BrasaEmber.copy(alpha = 0.7f), BrasaEmber.copy(alpha = 0f))
                    )
                )
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "buenas noches",
            style = DnType.note.copy(fontSize = 20.sp, lineHeight = 24.sp),
            color = BrasaClayDim,
            modifier = Modifier.graphicsLayer {
                val k = Smooth.transform(segment(progress.value, 0.54f, 0.7f))
                alpha = k
                translationY = (1f - k) * 8.dp.toPx()
            }
        )
    }
}
