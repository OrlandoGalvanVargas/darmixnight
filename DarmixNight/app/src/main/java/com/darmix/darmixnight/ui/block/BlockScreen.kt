package com.darmix.darmixnight.ui.block

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.darmix.darmixnight.ScheduleUtils
import com.darmix.darmixnight.ui.components.Crescent
import com.darmix.darmixnight.ui.components.DnGlyph
import com.darmix.darmixnight.ui.components.DnPrimaryButton
import com.darmix.darmixnight.ui.components.DnSecondaryButton
import com.darmix.darmixnight.ui.components.FarRidge
import com.darmix.darmixnight.ui.components.Glyph
import com.darmix.darmixnight.ui.components.MidRidge
import com.darmix.darmixnight.ui.components.NearRidge
import com.darmix.darmixnight.ui.components.buildStars
import com.darmix.darmixnight.ui.components.ridgePath
import com.darmix.darmixnight.ui.main.TimeText
import com.darmix.darmixnight.ui.theme.DnTheme
import com.darmix.darmixnight.ui.theme.DnType
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.sin

enum class BlockStep { HOLD, PHRASE }

private val SkyTop = Color(0xFF02050C)
private val SkyMid = Color(0xFF071226)
private val SkyLow = Color(0xFF0C2142)
private val HillFar = Color(0xFF0C1A31)
private val HillMid = Color(0xFF081326)
private val HillNear = Color(0xFF040A16)
private val MoonLight = Color(0xFFF7E6B5)
private val Glass = Color.White.copy(alpha = 0.06f)
private val GlassEdge = Color.White.copy(alpha = 0.12f)

@Composable
fun BlockScreen(
    emergencyAllowed: Boolean,
    holdDurationMs: Long,
    requiredPhrase: String,
    endMinutes: Int,
    loadWeeklyUses: suspend () -> Int,
    step: BlockStep,
    onStepChange: (BlockStep) -> Unit,
    onConfirmPhrase: () -> Unit
) {
    val colors = DnTheme.colors
    var now by remember { mutableIntStateOf(ScheduleUtils.currentMinutesOfDay()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = ScheduleUtils.currentMinutesOfDay()
            delay(60_000L - (System.currentTimeMillis() % 60_000L) + 50L)
        }
    }
    val weeklyUses by produceState<Int?>(null) { value = loadWeeklyUses() }

    Box(Modifier.fillMaxSize()) {
        NightSky(Modifier.fillMaxSize())

        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .heightIn(min = maxHeight)
                    .padding(horizontal = 28.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    ModeBadge()
                    Spacer(Modifier.height(if (step == BlockStep.PHRASE) 28.dp else 64.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            TimeText.hour(now),
                            style = DnType.clock.copy(fontSize = 80.sp, lineHeight = 84.sp, letterSpacing = (-3).sp),
                            color = colors.text
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            TimeText.period(now),
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.textDim,
                            modifier = Modifier.padding(bottom = 14.dp)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Es hora de dormir",
                        style = MaterialTheme.typography.headlineMedium,
                        color = colors.text,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Tu descanso termina a las ${TimeText.full(endMinutes)}. Faltan ${TimeText.span(TimeText.between(now, endMinutes))}.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textDim,
                        textAlign = TextAlign.Center
                    )
                }

                Column(
                    modifier = Modifier.padding(top = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (!emergencyAllowed) {
                        NightOnlyNote()
                    } else {
                        AnimatedContent(
                            targetState = step,
                            transitionSpec = {
                                (fadeIn(tween(320)) + slideInVertically(tween(380)) { it / 6 }) togetherWith
                                        (fadeOut(tween(200)) + slideOutVertically(tween(260)) { it / 8 })
                            },
                            label = "blockStep"
                        ) { current ->
                            when (current) {
                                BlockStep.HOLD -> HoldToUnlock(
                                    holdDurationMs = holdDurationMs,
                                    weeklyUses = weeklyUses,
                                    onComplete = { onStepChange(BlockStep.PHRASE) }
                                )
                                BlockStep.PHRASE -> PhraseCard(
                                    requiredPhrase = requiredPhrase,
                                    onConfirm = onConfirmPhrase,
                                    onCancel = { onStepChange(BlockStep.HOLD) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NightSky(modifier: Modifier) {
    val stars = remember { buildStars(count = 110, seed = 33, maxY = 0.62f) }
    val moon = remember { Crescent.unitPath() }
    val time = rememberInfiniteTransition(label = "blockSky").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(28_000, easing = LinearEasing), RepeatMode.Restart),
        label = "blockSkyTime"
    )
    Box(
        modifier.drawWithCache {
            val w = size.width
            val h = size.height
            val hillH = h * 0.42f
            val far = ridgePath(FarRidge, w, hillH)
            val mid = ridgePath(MidRidge, w, hillH)
            val near = ridgePath(NearRidge, w, hillH)
            val sky = Brush.verticalGradient(0f to SkyTop, 0.55f to SkyMid, 1f to SkyLow)
            val moonCenter = Offset(w * 0.84f, h * 0.11f)
            val moonR = 26.dp.toPx()
            val halo = Brush.radialGradient(
                0f to MoonLight.copy(alpha = 0.22f),
                0.4f to MoonLight.copy(alpha = 0.07f),
                1f to Color.Transparent,
                center = moonCenter,
                radius = moonR * 6f
            )
            val trail = 110.dp.toPx()
            onDrawBehind {
                val t = time.value
                drawRect(sky)
                stars.forEach { s ->
                    val tw = 0.5f + 0.5f * sin(t * 2f * PI.toFloat() * 3f + s.phase)
                    drawCircle(
                        color = if (s.warm) MoonLight else Color.White,
                        radius = s.radiusDp * density,
                        center = Offset(s.x * w, s.y * h),
                        alpha = 0.25f + 0.6f * tw
                    )
                }
                val shoot = (t * 2f % 1f) / 0.08f
                if (shoot < 1f) {
                    val head = Offset(w * (0.18f + 0.35f * shoot), h * (0.08f + 0.1f * shoot))
                    val tail = Offset(head.x - trail * 0.94f, head.y - trail * 0.3f)
                    val fade = sin(shoot * PI.toFloat())
                    drawLine(
                        brush = Brush.linearGradient(listOf(Color.White.copy(alpha = 0.8f * fade), Color.Transparent), head, tail),
                        start = head,
                        end = tail,
                        strokeWidth = 1.4.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
                drawCircle(halo, moonR * 6f, moonCenter)
                translate(moonCenter.x, moonCenter.y) {
                    scale(moonR, moonR, pivot = Offset.Zero) { drawPath(moon, MoonLight) }
                }
                translate(0f, h - hillH) {
                    drawPath(far, HillFar)
                    drawPath(mid, HillMid)
                    drawPath(near, HillNear)
                }
                drawRect(
                    Brush.verticalGradient(0.6f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.5f)),
                    size = Size(w, h)
                )
            }
        }
    )
}

@Composable
private fun ModeBadge() {
    val colors = DnTheme.colors
    val pulse = rememberInfiniteTransition(label = "modePulse").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Restart),
        label = "modePulseValue"
    )
    Row(
        modifier = Modifier
            .background(Glass, CircleShape)
            .border(1.dp, GlassEdge, CircleShape)
            .padding(start = 6.dp, end = 14.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(24.dp)
                .drawBehind {
                    val p = pulse.value
                    drawCircle(colors.accent, size.minDimension / 2f + 7.dp.toPx() * p, alpha = 0.4f * (1f - p))
                }
                .background(colors.accent, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Glyph(DnGlyph.MOON, colors.onAccent, size = 14.dp, strokeWidth = 2.2f)
        }
        Spacer(Modifier.width(9.dp))
        Text("MODO DESCANSO", style = DnType.overline, color = colors.text.copy(alpha = 0.85f))
    }
}

@Composable
private fun NightOnlyNote() {
    val colors = DnTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 380.dp)
            .background(Glass, RoundedCornerShape(26.dp))
            .border(1.dp, GlassEdge, RoundedCornerShape(26.dp))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Glyph(DnGlyph.LOCK, colors.textDim, size = 22.dp)
        Spacer(Modifier.height(10.dp))
        Text(
            "De noche no hay desbloqueo de emergencia",
            style = MaterialTheme.typography.titleSmall,
            color = colors.text,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Llamadas y mensajes siguen funcionando. Deja el teléfono, respira hondo y vuelve a dormir.",
            style = MaterialTheme.typography.bodySmall,
            color = colors.textDim,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun HoldToUnlock(
    holdDurationMs: Long,
    weeklyUses: Int?,
    onComplete: () -> Unit
) {
    val colors = DnTheme.colors
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val progress = remember { Animatable(0f) }
    var holding by remember { mutableStateOf(false) }
    var job by remember { mutableStateOf<Job?>(null) }
    val press by animateFloatAsState(if (holding) 0.93f else 1f, label = "holdPress")

    val remainingSeconds = ceil((1f - progress.value) * holdDurationMs / 1000f).toInt().coerceAtLeast(0)
    val totalSeconds = (holdDurationMs / 1000L).toInt()

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val counterAlpha by animateFloatAsState(if (holding) 1f else 0f, tween(220), label = "counterAlpha")
        Column(
            modifier = Modifier.graphicsLayer {
                alpha = counterAlpha
                translationY = (1f - counterAlpha) * 10.dp.toPx()
            },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "$remainingSeconds",
                style = DnType.clock.copy(fontSize = 56.sp, lineHeight = 60.sp),
                color = colors.accent
            )
            Text(
                if (remainingSeconds == 1) "segundo" else "segundos",
                style = DnType.overline,
                color = colors.accent.copy(alpha = 0.8f)
            )
        }
        Spacer(Modifier.height(14.dp))
        Text("¿Es una emergencia?", style = MaterialTheme.typography.titleSmall, color = colors.text)
        Spacer(Modifier.height(18.dp))
        Box(
            modifier = Modifier
                .size(112.dp)
                .graphicsLayer { scaleX = press; scaleY = press }
                .semantics { contentDescription = "Mantén presionado $totalSeconds segundos para una emergencia" }
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown()
                        holding = true
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        job?.cancel()
                        job = scope.launch {
                            progress.snapTo(0f)
                            progress.animateTo(1f, tween(holdDurationMs.toInt(), easing = LinearEasing))
                            holding = false
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onComplete()
                        }
                        waitForUpOrCancellation()
                        if (progress.value < 1f) {
                            job?.cancel()
                            holding = false
                            scope.launch { progress.animateTo(0f, tween(350, easing = FastOutSlowInEasing)) }
                        }
                    }
                }
                .drawWithCache {
                    val stroke = 5.dp.toPx()
                    val inset = stroke / 2f + 2.dp.toPx()
                    val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
                    onDrawBehind {
                        drawCircle(Color.White.copy(alpha = 0.05f), size.minDimension / 2f - inset - 6.dp.toPx())
                        drawArc(
                            color = Color.White.copy(alpha = 0.12f),
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = Offset(inset, inset),
                            size = arcSize,
                            style = Stroke(stroke)
                        )
                        val p = progress.value
                        if (p > 0f) {
                            drawCircle(
                                Brush.radialGradient(
                                    listOf(colors.accent.copy(alpha = 0.28f * p), Color.Transparent),
                                    center = center,
                                    radius = size.minDimension * 0.75f
                                ),
                                size.minDimension * 0.75f
                            )
                            drawArc(
                                color = colors.accent,
                                startAngle = -90f,
                                sweepAngle = 360f * p,
                                useCenter = false,
                                topLeft = Offset(inset, inset),
                                size = arcSize,
                                style = Stroke(stroke, cap = StrokeCap.Round)
                            )
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Glyph(
                DnGlyph.HOLD,
                if (holding) colors.accent else colors.text.copy(alpha = 0.85f),
                size = 34.dp,
                strokeWidth = 1.6f
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            if (holding) "Sigue presionando…" else "Mantén presionado $totalSeconds s",
            style = MaterialTheme.typography.labelLarge,
            color = if (holding) colors.accent else colors.text.copy(alpha = 0.85f)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            when (weeklyUses) {
                null -> " "
                0 -> "Sin usos esta semana"
                1 -> "Usado 1 vez esta semana"
                else -> "Usado $weeklyUses veces esta semana"
            },
            style = MaterialTheme.typography.bodySmall,
            color = colors.textDim
        )
    }
}

@Composable
private fun PhraseCard(
    requiredPhrase: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    val colors = DnTheme.colors
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    var typed by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    fun submit() {
        if (typed.trim().lowercase() == requiredPhrase) {
            keyboard?.hide()
            onConfirm()
        } else {
            error = true
        }
    }

    LaunchedEffect(Unit) {
        delay(250)
        focus.requestFocus()
        keyboard?.show()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 400.dp)
            .background(Color(0xFF0A1428).copy(alpha = 0.85f), RoundedCornerShape(28.dp))
            .border(1.dp, GlassEdge, RoundedCornerShape(28.dp))
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(36.dp)
                    .background(colors.accent.copy(alpha = 0.14f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Glyph(DnGlyph.KEY, colors.accent, size = 20.dp)
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Último paso", style = MaterialTheme.typography.titleMedium, color = colors.text)
                Text("Escribe exactamente la frase", style = MaterialTheme.typography.bodySmall, color = colors.textDim)
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "“$requiredPhrase”",
            style = MaterialTheme.typography.titleMedium,
            color = colors.accent,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.accent.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                .padding(vertical = 12.dp)
        )
        Spacer(Modifier.height(14.dp))
        BasicTextField(
            value = typed,
            onValueChange = {
                typed = it
                error = false
            },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.text),
            cursorBrush = SolidColor(colors.accent),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { submit() }),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focus),
            decorationBox = { inner ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(colors.raised, RoundedCornerShape(16.dp))
                        .border(
                            1.dp,
                            if (error) colors.danger else Color.White.copy(alpha = 0.10f),
                            RoundedCornerShape(16.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 15.dp)
                ) {
                    if (typed.isEmpty()) {
                        Text("Escribe aquí", style = MaterialTheme.typography.bodyLarge, color = colors.textDim)
                    }
                    inner()
                }
            }
        )
        if (error) {
            Spacer(Modifier.height(8.dp))
            Text("No coincide, inténtalo de nuevo.", style = MaterialTheme.typography.bodySmall, color = colors.danger)
        }
        Spacer(Modifier.height(16.dp))
        DnPrimaryButton(label = "Confirmar desbloqueo", onClick = { submit() }, glyph = DnGlyph.CHECK)
        Spacer(Modifier.height(10.dp))
        DnSecondaryButton(
            label = "Cancelar",
            onClick = {
                keyboard?.hide()
                onCancel()
            },
            tint = colors.textDim
        )
    }
}
