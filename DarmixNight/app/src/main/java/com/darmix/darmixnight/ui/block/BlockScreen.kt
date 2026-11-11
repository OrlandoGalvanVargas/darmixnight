package com.darmix.darmixnight.ui.block

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.darmix.darmixnight.R
import com.darmix.darmixnight.ScheduleUtils
import com.darmix.darmixnight.ui.components.EmberButton
import com.darmix.darmixnight.ui.components.QuietLink
import com.darmix.darmixnight.ui.components.SkyBackdrop
import com.darmix.darmixnight.ui.main.MainHeader
import com.darmix.darmixnight.ui.main.TimeText
import com.darmix.darmixnight.ui.main.night.NightMode
import com.darmix.darmixnight.ui.main.night.NightScene
import com.darmix.darmixnight.ui.theme.DnTheme
import com.darmix.darmixnight.ui.theme.DnType
import com.darmix.darmixnight.ui.theme.InstrumentSerif
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.ceil

enum class BlockStep { HOLD, PHRASE }

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
    var now by remember { mutableIntStateOf(ScheduleUtils.currentMinutesOfDay()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = ScheduleUtils.currentMinutesOfDay()
            delay(60_000L - (System.currentTimeMillis() % 60_000L) + 50L)
        }
    }
    val typing = emergencyAllowed && step == BlockStep.PHRASE
    val dim by animateFloatAsState(if (typing) 0.55f else 0f, tween(500), label = "dim")

    SkyBackdrop(Modifier.fillMaxSize()) {
        NightScene(mode = NightMode.BLOCKING, modifier = Modifier.fillMaxSize())
        Spacer(
            Modifier
                .fillMaxSize()
                .drawBehind { drawRect(Color.Black, alpha = dim) }
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            MainHeader(modifier = Modifier.padding(top = 8.dp))
            AnimatedContent(
                targetState = typing,
                transitionSpec = {
                    (fadeIn(tween(480, delayMillis = 150)) +
                            slideInVertically(tween(520, easing = FastOutSlowInEasing)) { it / 18 }) togetherWith
                            fadeOut(tween(220))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                label = "blockStep"
            ) { phrase ->
                if (phrase) {
                    PhraseStep(
                        requiredPhrase = requiredPhrase,
                        onConfirm = onConfirmPhrase,
                        onCancel = { onStepChange(BlockStep.HOLD) }
                    )
                } else {
                    RestStep(
                        now = now,
                        endMinutes = endMinutes,
                        emergencyAllowed = emergencyAllowed,
                        holdDurationMs = holdDurationMs,
                        onComplete = { onStepChange(BlockStep.PHRASE) }
                    )
                }
            }
        }
    }
}

@Composable
private fun RestStep(
    now: Int,
    endMinutes: Int,
    emergencyAllowed: Boolean,
    holdDurationMs: Long,
    onComplete: () -> Unit
) {
    val colors = DnTheme.colors
    val f = DnTheme.scale.factor
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(1.15f))
        val clock = buildAnnotatedString {
            append(TimeText.hour(now))
            withStyle(
                SpanStyle(
                    fontFamily = InstrumentSerif,
                    fontStyle = FontStyle.Italic,
                    fontSize = (40 * f).sp,
                    color = colors.ember,
                    letterSpacing = 0.em
                )
            ) { append(" " + TimeText.period(now).lowercase()) }
        }
        Text(
            clock,
            style = DnType.countdown.copy(fontSize = (124 * f).sp, lineHeight = (124 * f).sp),
            color = colors.text,
            maxLines = 1,
            softWrap = false
        )
        Spacer(Modifier.height((4 * f).dp))
        Text(
            "Es hora de dormir.",
            style = DnType.lead.copy(fontSize = (30 * f).sp, lineHeight = (34 * f).sp),
            color = colors.textSoft
        )
        Spacer(Modifier.height((6 * f).dp))
        val remaining = buildAnnotatedString {
            append("Despiertas en ")
            withStyle(SpanStyle(color = colors.ember)) { append(TimeText.span(TimeText.between(now, endMinutes))) }
        }
        Text(
            remaining,
            style = DnType.note.copy(fontSize = (21 * f).sp, lineHeight = (25 * f).sp),
            color = colors.clay
        )
        Spacer(Modifier.weight(1f))
        if (emergencyAllowed) {
            HoldButton(holdDurationMs = holdDurationMs, onComplete = onComplete)
        } else {
            Spacer(
                Modifier
                    .width(120.dp)
                    .height(1.dp)
                    .drawBehind {
                        drawRect(
                            Brush.horizontalGradient(
                                listOf(colors.ember.copy(alpha = 0f), colors.ember.copy(alpha = 0.5f), colors.ember.copy(alpha = 0f))
                            )
                        )
                    }
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "Esta noche no hay salida de emergencia.",
                style = DnType.noteSmall.copy(fontSize = (16 * f).sp, lineHeight = (20 * f).sp),
                color = colors.textMute,
                textAlign = TextAlign.Center
            )
        }
        Spacer(Modifier.height((26 * f).dp))
    }
}

@Composable
private fun HoldButton(holdDurationMs: Long, onComplete: () -> Unit) {
    val colors = DnTheme.colors
    val f = DnTheme.scale.factor
    val scope = rememberCoroutineScope()
    val progress = remember { Animatable(0f) }
    var holding by remember { mutableStateOf(false) }
    var job by remember { mutableStateOf<Job?>(null) }
    val press by animateFloatAsState(if (holding) 0.94f else 1f, tween(160), label = "holdPress")
    val lit by animateFloatAsState(if (holding) 1f else 0f, tween(220), label = "holdLit")
    val totalSeconds = (holdDurationMs / 1000L).toInt()
    val remaining = ceil((1f - progress.value) * holdDurationMs / 1000f).toInt().coerceAtLeast(0)
    val size = (108 * f).dp

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(size + 44.dp)
                .semantics { contentDescription = "Mantén presionado $totalSeconds segundos para una emergencia" }
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown()
                        holding = true
                        job?.cancel()
                        job = scope.launch {
                            progress.snapTo(0f)
                            progress.animateTo(1f, tween(holdDurationMs.toInt(), easing = LinearEasing))
                            holding = false
                            onComplete()
                        }
                        waitForUpOrCancellation()
                        if (progress.value < 1f) {
                            job?.cancel()
                            holding = false
                            scope.launch { progress.animateTo(0f, tween(380, easing = FastOutSlowInEasing)) }
                        }
                    }
                }
                .drawBehind {
                    val c = center
                    val ring = size.toPx() / 2f + 14.dp.toPx()
                    val glow = Brush.radialGradient(
                        0f to colors.ember.copy(alpha = 0.22f + 0.3f * progress.value),
                        1f to colors.ember.copy(alpha = 0f),
                        center = c,
                        radius = ring * 1.7f
                    )
                    drawCircle(glow, ring * 1.7f, c)
                    drawCircle(colors.bronze, ring, c, style = Stroke(1.5.dp.toPx()))
                    val p = progress.value
                    if (p > 0f) {
                        drawArc(
                            color = colors.ember,
                            startAngle = -90f,
                            sweepAngle = 360f * p,
                            useCenter = false,
                            topLeft = Offset(c.x - ring, c.y - ring),
                            size = Size(ring * 2f, ring * 2f),
                            style = Stroke(3.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier
                    .size(size)
                    .graphicsLayer {
                        scaleX = press
                        scaleY = press
                    }
            ) {
                Image(
                    painter = painterResource(R.drawable.settings_button_off),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = 1f - lit }
                )
                Image(
                    painter = painterResource(R.drawable.settings_button_on),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = lit }
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        AnimatedContent(
            targetState = holding,
            transitionSpec = { fadeIn(tween(240)) togetherWith fadeOut(tween(160)) },
            label = "holdLabel"
        ) { isHolding ->
            if (isHolding) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "$remaining s",
                        style = DnType.note.copy(fontSize = (22 * f).sp, lineHeight = (26 * f).sp),
                        color = colors.ember
                    )
                    Text(
                        "sigue presionando",
                        style = DnType.noteSmall.copy(fontSize = (16 * f).sp, lineHeight = (20 * f).sp),
                        color = colors.textMute
                    )
                }
            } else {
                Text(
                    "Mantén presionado $totalSeconds s si es una emergencia",
                    style = DnType.noteSmall.copy(fontSize = (16 * f).sp, lineHeight = (20 * f).sp),
                    color = colors.textMute,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun PhraseStep(requiredPhrase: String, onConfirm: () -> Unit, onCancel: () -> Unit) {
    val colors = DnTheme.colors
    val f = DnTheme.scale.factor
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
        delay(300)
        focus.requestFocus()
        keyboard?.show()
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(1f))
        Text(
            "Un último paso.",
            style = TextStyle(fontFamily = InstrumentSerif, fontSize = (36 * f).sp, lineHeight = (40 * f).sp),
            color = colors.text
        )
        Spacer(Modifier.height((8 * f).dp))
        Text(
            "Escribe la frase para abrir:",
            style = DnType.note.copy(fontSize = (19 * f).sp, lineHeight = (23 * f).sp),
            color = colors.textSoft
        )
        Spacer(Modifier.height((4 * f).dp))
        Text(
            requiredPhrase,
            style = DnType.note.copy(fontSize = (26 * f).sp, lineHeight = (30 * f).sp),
            color = colors.ember
        )
        Spacer(Modifier.height((26 * f).dp))
        BasicTextField(
            value = typed,
            onValueChange = {
                typed = it
                error = false
            },
            singleLine = true,
            textStyle = TextStyle(fontFamily = InstrumentSerif, fontSize = (24 * f).sp, color = colors.text, textAlign = TextAlign.Center),
            cursorBrush = SolidColor(colors.ember),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { submit() }),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focus)
                .drawBehind {
                    val y = size.height
                    drawLine(
                        if (error) colors.danger else colors.clay.copy(alpha = 0.5f),
                        Offset(0f, y),
                        Offset(size.width, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }
                .padding(vertical = 10.dp)
        )
        Box(Modifier.height(30.dp), contentAlignment = Alignment.Center) {
            if (error) {
                Text(
                    "No coincide, inténtalo de nuevo.",
                    style = DnType.noteSmall.copy(fontSize = (15 * f).sp, lineHeight = (19 * f).sp),
                    color = colors.danger
                )
            }
        }
        Spacer(Modifier.height((10 * f).dp))
        EmberButton("Desbloquear", onClick = { submit() })
        Spacer(Modifier.height(4.dp))
        QuietLink(
            "Cancelar",
            onClick = {
                keyboard?.hide()
                onCancel()
            }
        )
        Spacer(Modifier.height((12 * f).dp))
    }
}
