package com.darmix.darmixnight.ui.block

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.darmix.darmixnight.ScheduleUtils
import com.darmix.darmixnight.ui.components.AppConstellation
import com.darmix.darmixnight.ui.components.ConstellationApp
import com.darmix.darmixnight.ui.components.DnGlyph
import com.darmix.darmixnight.ui.components.DnPrimaryButton
import com.darmix.darmixnight.ui.components.Glyph
import com.darmix.darmixnight.ui.components.SkyBackdrop
import com.darmix.darmixnight.ui.components.rememberSceneClock
import com.darmix.darmixnight.ui.main.TimeText
import com.darmix.darmixnight.ui.panda.PandaMood
import com.darmix.darmixnight.ui.panda.PandaMotion
import com.darmix.darmixnight.ui.panda.drawPanda
import com.darmix.darmixnight.ui.theme.DnTheme
import com.darmix.darmixnight.ui.theme.DnType
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.sin
import kotlin.math.sqrt

enum class BlockStep { HOLD, PHRASE }

private val CardInk = Color(0xFF0A1428)
private val GlassEdge = Color.White.copy(alpha = 0.12f)
private val SirenRed = Color(0xFFE5484D)
private val MoonTop = Color(0xFFFFF4D6)
private val MoonMid = Color(0xFFF0D49C)
private val MoonLow = Color(0xFFC99A55)
private val MoonGlow = Color(0xFFF4E3BC)
private val PandaRim = Color(0xFF4A5680)

private val NightApps = listOf(
    ConstellationApp(DnGlyph.CALL, "Llamadas"),
    ConstellationApp(DnGlyph.MESSAGE, "Mensajes"),
    ConstellationApp(DnGlyph.CONTACT, "Contactos"),
    ConstellationApp(DnGlyph.CAMERA, "Cámara")
)

@Composable
fun BlockScreen(
    emergencyAllowed: Boolean,
    holdDurationMs: Long,
    requiredPhrase: String,
    startMinutes: Int,
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
    val clock = rememberSceneClock()
    val typing = emergencyAllowed && step == BlockStep.PHRASE

    SkyBackdrop(Modifier.fillMaxSize()) {
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
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    ModeBadge(clock)
                    Spacer(Modifier.height(if (typing) 18.dp else 30.dp))
                    CenteredClock(now)
                    Text(
                        "Es hora de dormir",
                        style = DnType.clockMedium.copy(fontSize = 20.sp, lineHeight = 26.sp),
                        color = colors.text,
                        textAlign = TextAlign.Center
                    )
                    AnimatedVisibility(
                        visible = !typing,
                        enter = fadeIn(tween(400)) + expandVertically(tween(400)),
                        exit = fadeOut(tween(200)) + shrinkVertically(tween(320))
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            MoonCradle(clock)
                            NightBar(startMinutes, endMinutes, now)
                        }
                    }
                }

                Column(
                    modifier = Modifier.padding(top = 28.dp),
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
                                    clock = clock,
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
private fun ModeBadge(clock: State<Float>) {
    val colors = DnTheme.colors
    Row(
        Modifier
            .background(Color.White.copy(alpha = 0.07f), CircleShape)
            .border(1.dp, GlassEdge, CircleShape)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(
            Modifier
                .size(14.dp)
                .drawBehind {
                    val pulse = 0.5f + 0.5f * sin(clock.value * 2.4f)
                    drawCircle(colors.accent, size.minDimension / 2f, alpha = 0.15f + 0.2f * pulse)
                    drawCircle(colors.accent, 3.5.dp.toPx())
                }
        )
        Spacer(Modifier.width(8.dp))
        Text("MODO DESCANSO", style = DnType.overline.copy(letterSpacing = 2.2.sp), color = colors.text)
    }
}

@Composable
private fun CenteredClock(now: Int) {
    val colors = DnTheme.colors
    val periodStyle = DnType.clockMedium.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold)
    Row(verticalAlignment = Alignment.Bottom) {
        Text(
            TimeText.period(now),
            style = periodStyle,
            color = Color.Transparent,
            modifier = Modifier
                .padding(end = 6.dp, bottom = 16.dp)
                .alpha(0f)
        )
        Text(
            TimeText.hour(now),
            style = DnType.clock.copy(fontSize = 84.sp, lineHeight = 88.sp, letterSpacing = (-3).sp),
            color = colors.text
        )
        Text(
            TimeText.period(now),
            style = periodStyle,
            color = colors.textDim,
            modifier = Modifier.padding(start = 6.dp, bottom = 16.dp)
        )
    }
}

@Composable
private fun MoonCradle(clock: State<Float>) {
    val crescent = remember { CradleShape() }
    Spacer(
        Modifier
            .fillMaxWidth()
            .height(270.dp)
            .semantics { contentDescription = "Panda durmiendo en la luna" }
            .drawWithCache {
                val u = density
                val cx = size.width / 2f
                val cy = 150f * u
                val shape = crescent.build(cx, cy, 112f * u, 46f * u)
                val moonBrush = Brush.verticalGradient(
                    0f to MoonTop,
                    0.6f to MoonMid,
                    1f to MoonLow,
                    startY = cy - 112f * u,
                    endY = cy + 112f * u
                )
                val glow = Brush.radialGradient(
                    0f to MoonGlow.copy(alpha = 0.42f),
                    0.5f to MoonGlow.copy(alpha = 0.12f),
                    1f to Color.Transparent,
                    center = Offset(cx, cy - 10f * u),
                    radius = 230f * u
                )
                onDrawBehind {
                    drawCradle(clock.value, cx, cy, u, shape, moonBrush, glow)
                }
            }
    )
}

private class CradleShape {
    fun build(cx: Float, cy: Float, r: Float, lift: Float): Path {
        val outer = Path().apply { addOval(Rect(Offset(cx, cy), r)) }
        val inner = Path().apply { addOval(Rect(Offset(cx, cy - lift), r)) }
        return Path().apply { op(outer, inner, PathOperation.Difference) }
    }
}

private fun DrawScope.drawCradle(
    t: Float,
    cx: Float,
    cy: Float,
    u: Float,
    shape: Path,
    moonBrush: Brush,
    glow: Brush
) {
    val r = 112f * u
    val lift = 46f * u
    val hornX = sqrt(r * r - (lift / 2f) * (lift / 2f))
    val hornY = cy - lift / 2f
    val pulse = 0.9f + 0.1f * sin(t * 1.2f)
    drawCircle(glow, 230f * u * pulse, Offset(cx, cy - 10f * u))
    val sway = 4f * sin(t * 0.9f)
    rotate(sway, Offset(cx, 0f)) {
        for (hx in floatArrayOf(cx - hornX, cx + hornX)) {
            drawLine(
                MoonGlow.copy(alpha = 0.35f),
                Offset(hx, hornY - 2f * u),
                Offset(hx, 0f),
                strokeWidth = 1f * u,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(2f * u, 4f * u))
            )
        }
        drawPath(shape, moonBrush)
        floatArrayOf(-70f, 30f, 9f, -30f, 62f, 6f, 40f, 66f, 8f, 78f, 28f, 5f, 0f, 80f, 4f).toList().chunked(3).forEach { (dx, dy, cr) ->
            drawCircle(MoonLow, cr * u, Offset(cx + dx * u, cy + dy * u), alpha = 0.35f)
        }
        val s = 0.92f * u
        val left = cx - 106f * s
        val top = cy + r - lift / 2f - 194f * s + 6f * u
        translate(left, top) {
            scale(s, s, Offset.Zero) {
                drawPanda(PandaMood.SLEEP, PandaMotion(t, 0f, 0f, 0f, PandaRim))
            }
        }
    }
}

@Composable
private fun NightBar(start: Int, end: Int, now: Int) {
    val colors = DnTheme.colors
    val total = TimeText.between(start, end).coerceAtLeast(1)
    val elapsed = TimeText.between(start, now).coerceAtMost(total)
    val target = elapsed / total.toFloat()
    val fraction by animateFloatAsState(target, tween(900), label = "nightBar")
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
        Spacer(
            Modifier
                .fillMaxWidth()
                .height(22.dp)
                .drawBehind {
                    val y = size.height / 2f
                    val x0 = 6.dp.toPx()
                    val x1 = size.width - 6.dp.toPx()
                    val mx = x0 + (x1 - x0) * fraction
                    drawLine(Color.White.copy(alpha = 0.14f), Offset(x0, y), Offset(x1, y), 4.dp.toPx(), StrokeCap.Round)
                    if (mx > x0) {
                        drawLine(
                            Brush.horizontalGradient(listOf(colors.accent.copy(alpha = 0.3f), colors.accent), startX = x0, endX = mx),
                            Offset(x0, y),
                            Offset(mx, y),
                            4.dp.toPx(),
                            StrokeCap.Round
                        )
                    }
                    drawCircle(colors.accent, 11.dp.toPx(), Offset(mx, y), alpha = 0.2f)
                    drawCircle(colors.accent, 6.dp.toPx(), Offset(mx, y))
                }
        )
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(TimeText.full(start), style = MaterialTheme.typography.labelMedium, color = colors.textDim)
            Text(
                "Faltan ${TimeText.span(TimeText.between(now, end))}",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = colors.accent,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            Text(TimeText.full(end), style = MaterialTheme.typography.labelMedium, color = colors.textDim)
        }
    }
}

@Composable
private fun NightOnlyNote() {
    val colors = DnTheme.colors
    Column(
        Modifier
            .fillMaxWidth()
            .widthIn(max = 420.dp)
            .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(26.dp))
            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(26.dp))
            .padding(horizontal = 14.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Buenas noches", style = DnType.clockMedium.copy(fontSize = 19.sp, lineHeight = 24.sp), color = colors.text)
        Spacer(Modifier.height(4.dp))
        Text(
            "De noche no hay salida de emergencia.\nEsto sí sigue funcionando:",
            style = MaterialTheme.typography.bodySmall,
            color = colors.textDim,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        AppConstellation(NightApps)
    }
}

@Composable
private fun HoldToUnlock(
    holdDurationMs: Long,
    weeklyUses: Int?,
    clock: State<Float>,
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
                style = DnType.clock.copy(fontSize = 44.sp, lineHeight = 48.sp),
                color = colors.text
            )
            Text(
                if (remainingSeconds == 1) "SEGUNDO" else "SEGUNDOS",
                style = DnType.overline,
                color = colors.textDim
            )
        }
        Spacer(Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .size(150.dp)
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
                    val ringStroke = 5.dp.toPx()
                    val ringRadius = 46.dp.toPx()
                    val coreRadius = 36.dp.toPx()
                    val middle = Offset(size.width / 2f, size.height / 2f)
                    val shine = Brush.radialGradient(
                        listOf(Color.White.copy(alpha = 0.45f), Color.Transparent),
                        center = Offset(middle.x - coreRadius * 0.3f, middle.y - coreRadius * 0.4f),
                        radius = coreRadius * 1.3f
                    )
                    onDrawBehind {
                        val p = progress.value
                        val breathe = 0.5f + 0.5f * sin(clock.value * 2f)
                        drawCircle(SirenRed, 70.dp.toPx(), alpha = 0.08f + 0.06f * breathe + 0.12f * p)
                        drawCircle(SirenRed, 54.dp.toPx(), alpha = 0.14f + 0.1f * p)
                        drawCircle(Color.White.copy(alpha = 0.12f), ringRadius, style = Stroke(ringStroke))
                        if (p > 0f) {
                            drawArc(
                                color = SirenRed,
                                startAngle = -90f,
                                sweepAngle = 360f * p,
                                useCenter = false,
                                topLeft = Offset(center.x - ringRadius, center.y - ringRadius),
                                size = Size(ringRadius * 2f, ringRadius * 2f),
                                style = Stroke(ringStroke, cap = StrokeCap.Round)
                            )
                        }
                        drawCircle(SirenRed, coreRadius)
                        drawCircle(shine, coreRadius)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Glyph(DnGlyph.SIREN, Color.White, size = 28.dp, strokeWidth = 2f)
        }
        Spacer(Modifier.height(6.dp))
        Text("¿Es una emergencia?", style = MaterialTheme.typography.titleSmall, color = colors.text)
        Spacer(Modifier.height(4.dp))
        Text(
            listOfNotNull(
                if (holding) "Sigue presionando…" else "Mantén presionado $totalSeconds s",
                when (weeklyUses) {
                    null -> null
                    0 -> "sin usos esta semana"
                    1 -> "1 uso esta semana"
                    else -> "$weeklyUses usos esta semana"
                }
            ).joinToString(" · "),
            style = MaterialTheme.typography.bodySmall,
            color = if (holding) colors.text else colors.textDim,
            textAlign = TextAlign.Center
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
            .widthIn(max = 420.dp)
            .background(CardInk.copy(alpha = 0.9f), RoundedCornerShape(26.dp))
            .border(1.dp, GlassEdge, RoundedCornerShape(26.dp))
            .padding(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(38.dp)
                    .background(SirenRed.copy(alpha = 0.16f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Glyph(DnGlyph.LOCK, SirenRed, size = 18.dp, strokeWidth = 2.2f)
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Último paso", style = MaterialTheme.typography.titleMedium, color = colors.text)
                Text("Escribe la frase para abrir", style = MaterialTheme.typography.bodySmall, color = colors.textDim)
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            requiredPhrase,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.4.sp),
            color = colors.accent,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    drawRoundRect(
                        colors.accent.copy(alpha = 0.06f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(12.dp.toPx())
                    )
                    drawRoundRect(
                        colors.accent.copy(alpha = 0.35f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(12.dp.toPx()),
                        style = Stroke(1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 4.dp.toPx())))
                    )
                }
                .padding(vertical = 12.dp)
        )
        Spacer(Modifier.height(12.dp))
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
                        .background(Color.White.copy(alpha = 0.07f), RoundedCornerShape(14.dp))
                        .border(
                            1.dp,
                            if (error) colors.danger else Color.White.copy(alpha = 0.22f),
                            RoundedCornerShape(14.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 14.dp)
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
        Box(
            Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.10f), CircleShape)
                .border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape)
                .clickable {
                    keyboard?.hide()
                    onCancel()
                },
            contentAlignment = Alignment.Center
        ) {
            Text("Cancelar", style = MaterialTheme.typography.labelLarge, color = colors.text.copy(alpha = 0.8f))
        }
    }
}
