package com.darmix.darmixnight.ui.panda

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import com.darmix.darmixnight.ui.theme.DnTheme
import kotlinx.coroutines.launch

private val RimNight = Color(0xFF4A5680)
private val RimDay = Color(0xFF1C2133)

@Composable
fun PandaCharacter(
    mood: PandaMood,
    clock: State<Float>,
    onTap: () -> Unit,
    onPet: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = DnTheme.colors
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val hop = remember { Animatable(0f) }
    val joy = remember { Animatable(0f) }
    val hearts = remember { Animatable(0f) }
    val rim = if (colors.isDark) RimNight else RimDay
    val tapAction by rememberUpdatedState(onTap)
    val petAction by rememberUpdatedState(onPet)

    Box(
        modifier = modifier
            .semantics {
                role = Role.Button
                contentDescription = "Panda de DarmixNight"
            }
            .pointerInput(mood) {
                detectTapGestures(
                    onTap = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        scope.launch {
                            hop.snapTo(0f)
                            hop.animateTo(1f, tween(520, easing = FastOutSlowInEasing))
                            hop.snapTo(0f)
                        }
                        scope.launch {
                            joy.snapTo(1f)
                            joy.animateTo(0f, tween(1400, delayMillis = 300, easing = LinearEasing))
                        }
                        tapAction()
                    },
                    onLongPress = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        scope.launch {
                            hearts.snapTo(0f)
                            hearts.animateTo(1f, tween(1700, easing = LinearEasing))
                            hearts.snapTo(0f)
                        }
                        scope.launch {
                            joy.snapTo(1f)
                            joy.animateTo(0f, tween(1200, delayMillis = 900, easing = LinearEasing))
                        }
                        petAction()
                    }
                )
            },
        contentAlignment = Alignment.BottomCenter
    ) {
        AnimatedContent(
            targetState = mood,
            transitionSpec = {
                (fadeIn(tween(420, delayMillis = 120)) +
                        scaleIn(spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessLow), initialScale = 0.7f)) togetherWith
                        (fadeOut(tween(200)) + scaleOut(tween(220), targetScale = 0.85f))
            },
            contentAlignment = Alignment.BottomCenter,
            label = "pandaMood"
        ) { current ->
            Spacer(
                Modifier
                    .fillMaxSize()
                    .drawBehind {
                        val s = size.minDimension / PANDA_VIEWPORT
                        val left = (size.width - PANDA_VIEWPORT * s) / 2f
                        val top = size.height - PANDA_VIEWPORT * s
                        translate(left, top) {
                            scale(s, s, Offset.Zero) {
                                drawPanda(
                                    current,
                                    PandaMotion(clock.value, hop.value, joy.value, hearts.value, rim)
                                )
                            }
                        }
                    }
            )
        }
    }
}
