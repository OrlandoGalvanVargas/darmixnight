package com.darmix.darmixnight.ui.main.emergency

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.darmix.darmixnight.BlockPrefs
import com.darmix.darmixnight.R
import com.darmix.darmixnight.ui.components.rememberSoundBank
import com.darmix.darmixnight.ui.main.night.NightMode
import com.darmix.darmixnight.ui.theme.DnTheme
import com.darmix.darmixnight.ui.theme.DnType
import com.darmix.darmixnight.ui.theme.InstrumentSerif
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

private const val SCALE_MAX = 30
private const val SWEEP_START = -120f
private const val DEGREES_PER_MINUTE = 8f

private fun angleFor(minutes: Int): Float = SWEEP_START + minutes * DEGREES_PER_MINUTE

private fun minutesFor(angle: Float): Int =
    ((angle - SWEEP_START) / DEGREES_PER_MINUTE).roundToInt()
        .coerceIn(BlockPrefs.EMERGENCY_MIN_MINUTES, BlockPrefs.EMERGENCY_MAX_MINUTES)

@Composable
internal fun EmergencyTab(
    durationMinutes: Int,
    mode: NightMode,
    tickSound: Boolean,
    tickHaptics: Boolean,
    onChangeDuration: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val f = DnTheme.scale.factor
    val editable = mode != NightMode.BLOCKING
    var shown by remember(durationMinutes) { mutableIntStateOf(durationMinutes) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(1f))
        Heading(mode = mode, f = f)
        Spacer(Modifier.height((4 * f).dp))
        Minutes(value = shown, f = f)
        MinuteDial(
            value = shown,
            enabled = editable,
            tickSound = tickSound,
            tickHaptics = tickHaptics,
            f = f,
            onPreview = { shown = it },
            onCommit = {
                shown = it
                if (it != durationMinutes) onChangeDuration(it)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height((248 * f).dp)
        )
        Spacer(Modifier.height((18 * f).dp))
    }
}

@Composable
private fun Heading(mode: NightMode, f: Float) {
    val colors = DnTheme.colors
    val title = buildAnnotatedString {
        append("Un pequeño ")
        withStyle(SpanStyle(color = colors.ember)) { append("respiro,") }
    }
    Text(
        title,
        style = DnType.lead.copy(fontSize = (31 * f).sp, lineHeight = (36 * f).sp),
        color = colors.textSoft
    )
    Spacer(Modifier.height((2 * f).dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Hairline(toRight = false, modifier = Modifier.weight(1f))
        AnimatedContent(
            targetState = mode,
            transitionSpec = { fadeIn(tween(600, delayMillis = 150)) togetherWith fadeOut(tween(250)) },
            label = "subtitle"
        ) { current ->
            Text(
                when (current) {
                    NightMode.BEFORE -> "solo por un momento."
                    NightMode.BLOCKING -> "fijo hasta que amanezca."
                    NightMode.PAUSED -> "el bloqueo está en pausa."
                },
                style = DnType.note.copy(fontSize = (20 * f).sp, lineHeight = (24 * f).sp),
                color = colors.text,
                maxLines = 1,
                modifier = Modifier.padding(horizontal = 14.dp)
            )
        }
        Hairline(toRight = true, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun Minutes(value: Int, f: Float) {
    val colors = DnTheme.colors
    val text = buildAnnotatedString {
        append(value.toString())
        withStyle(
            SpanStyle(
                fontFamily = InstrumentSerif,
                fontStyle = FontStyle.Italic,
                fontSize = (48 * f).sp,
                color = colors.ember,
                letterSpacing = 0.em
            )
        ) { append(" min") }
    }
    Text(
        text,
        style = DnType.countdown.copy(fontSize = (100 * f).sp, lineHeight = (100 * f).sp),
        color = colors.text,
        maxLines = 1,
        softWrap = false,
        modifier = Modifier.semantics { contentDescription = "$value minutos de emergencia" }
    )
}

@Composable
private fun Hairline(toRight: Boolean, modifier: Modifier = Modifier) {
    val colors = DnTheme.colors
    Spacer(
        modifier
            .height(1.dp)
            .drawWithCache {
                val brush = Brush.horizontalGradient(
                    if (toRight) 0f to colors.ember.copy(alpha = 0.45f) else 0f to colors.ember.copy(alpha = 0f),
                    if (toRight) 1f to colors.ember.copy(alpha = 0f) else 1f to colors.ember.copy(alpha = 0.45f)
                )
                onDrawBehind { drawRect(brush) }
            }
    )
}

@Composable
private fun MinuteDial(
    value: Int,
    enabled: Boolean,
    tickSound: Boolean,
    tickHaptics: Boolean,
    f: Float,
    onPreview: (Int) -> Unit,
    onCommit: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = DnTheme.colors
    val haptics = LocalHapticFeedback.current
    val tick = rememberDialTick()
    val withSound by rememberUpdatedState(tickSound)
    val withHaptics by rememberUpdatedState(tickHaptics)
    val feedback: () -> Unit = {
        if (withHaptics) haptics.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
        if (withSound) tick()
    }
    val scope = rememberCoroutineScope()
    val measurer = rememberTextMeasurer()
    val rotation = remember { Animatable(angleFor(value)) }
    var dragging by remember { mutableStateOf(false) }
    val latest by rememberUpdatedState(value)
    val dim by animateFloatAsState(if (enabled) 1f else 0.55f, tween(600), label = "dialDim")
    val labelStyle = TextStyle(fontFamily = InstrumentSerif, fontSize = (19 * f).sp)
    val minAngle = angleFor(BlockPrefs.EMERGENCY_MIN_MINUTES)
    val maxAngle = angleFor(BlockPrefs.EMERGENCY_MAX_MINUTES)

    LaunchedEffect(value) {
        if (!dragging) rotation.animateTo(angleFor(value), spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow))
    }

    BoxWithConstraints(modifier) {
        val labelGap = (30 * f).dp
        val radius = minOf((maxWidth - labelGap * 2) / 1.75f, (maxHeight - labelGap) / 1.55f)
        val centerY = radius + labelGap * 0.75f
        val tickOuter = radius - (15 * f).dp
        val knobRadius = tickOuter - (22 * f).dp

        val settle: (Int) -> Unit = { minutes ->
            scope.launch { rotation.animateTo(angleFor(minutes), spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium)) }
            onCommit(minutes)
        }

        Box(
            Modifier
                .fillMaxSize()
                .semantics {
                    contentDescription = "Duración del desbloqueo de emergencia"
                    stateDescription = "$value minutos"
                    progressBarRangeInfo = ProgressBarRangeInfo(
                        value.toFloat(),
                        BlockPrefs.EMERGENCY_MIN_MINUTES.toFloat()..BlockPrefs.EMERGENCY_MAX_MINUTES.toFloat(),
                        steps = BlockPrefs.EMERGENCY_MAX_MINUTES - BlockPrefs.EMERGENCY_MIN_MINUTES - 1
                    )
                    if (enabled) {
                        setProgress { target ->
                            val minutes = target.roundToInt().coerceIn(BlockPrefs.EMERGENCY_MIN_MINUTES, BlockPrefs.EMERGENCY_MAX_MINUTES)
                            settle(minutes)
                            true
                        }
                    }
                }
                .pointerInput(enabled, centerY) {
                    if (!enabled) return@pointerInput
                    val center = Offset(size.width / 2f, centerY.toPx())
                    var last = 0f
                    var angle = 0f
                    var shownMinutes = latest
                    detectDragGestures(
                        onDragStart = { start ->
                            dragging = true
                            shownMinutes = latest
                            angle = rotation.value
                            last = Math.toDegrees(atan2((start.x - center.x).toDouble(), (center.y - start.y).toDouble())).toFloat()
                        },
                        onDragEnd = {
                            dragging = false
                            settle(shownMinutes)
                        },
                        onDragCancel = {
                            dragging = false
                            settle(shownMinutes)
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val now = Math.toDegrees(atan2((change.position.x - center.x).toDouble(), (center.y - change.position.y).toDouble())).toFloat()
                            var delta = now - last
                            if (delta > 180f) delta -= 360f
                            if (delta < -180f) delta += 360f
                            last = now
                            angle = (angle + delta).coerceIn(minAngle, maxAngle)
                            val next = angle
                            scope.launch { rotation.snapTo(next) }
                            val minutes = minutesFor(next)
                            if (minutes != shownMinutes) {
                                shownMinutes = minutes
                                feedback()
                                onPreview(minutes)
                            }
                        }
                    )
                }
                .pointerInput(enabled, centerY, knobRadius) {
                    if (!enabled) return@pointerInput
                    val center = Offset(size.width / 2f, centerY.toPx())
                    detectTapGestures { tap ->
                        val distance = (tap - center).getDistance()
                        if (distance < knobRadius.toPx()) return@detectTapGestures
                        val angle = Math.toDegrees(atan2((tap.x - center.x).toDouble(), (center.y - tap.y).toDouble())).toFloat()
                        if (angle < SWEEP_START - DEGREES_PER_MINUTE || angle > -SWEEP_START + DEGREES_PER_MINUTE) return@detectTapGestures
                        val minutes = minutesFor(angle.coerceIn(minAngle, maxAngle))
                        feedback()
                        onPreview(minutes)
                        settle(minutes)
                    }
                }
                .graphicsLayer { alpha = dim }
                .drawWithCache {
                    val center = Offset(size.width / 2f, centerY.toPx())
                    val outer = tickOuter.toPx()
                    val major = (14 * f).dp.toPx()
                    val minor = (7 * f).dp.toPx()
                    val labelRadius = radius.toPx()
                    val labels = (0..SCALE_MAX step 5).associateWith { measurer.measure(it.toString(), labelStyle) }
                    val shadow = Brush.radialGradient(
                        0f to Color.Black.copy(alpha = 0.6f),
                        0.75f to Color.Black.copy(alpha = 0.35f),
                        1f to Color.Black.copy(alpha = 0f),
                        center = Offset.Zero,
                        radius = 1f
                    )
                    val knobR = knobRadius.toPx()
                    onDrawBehind {
                        translate(center.x, center.y + knobR * 0.08f) {
                            scale(knobR * 1.22f, knobR * 1.22f, Offset.Zero) {
                                drawCircle(shadow, 1f, Offset.Zero)
                            }
                        }
                        val current = minutesFor(rotation.value)
                        for (m in 0..SCALE_MAX) {
                            val a = (angleFor(m) * PI / 180.0).toFloat()
                            val dir = Offset(sin(a), -cos(a))
                            val isMajor = m % 5 == 0
                            val len = if (isMajor) major else minor
                            val on = m in 1..current
                            val start = center + dir * (outer - len)
                            val end = center + dir * outer
                            drawLine(
                                color = if (on) colors.ember else colors.text.copy(alpha = if (isMajor) 0.55f else 0.3f),
                                start = start,
                                end = end,
                                strokeWidth = (if (isMajor) 2.2f else 1.3f) * density,
                                cap = StrokeCap.Round
                            )
                        }
                        labels.forEach { (m, layout) ->
                            val a = (angleFor(m) * PI / 180.0).toFloat()
                            val p = center + Offset(sin(a), -cos(a)) * labelRadius
                            val near = m != 0 && current in (m - 2)..(m + 2)
                            drawText(
                                layout,
                                color = if (near) colors.ember else colors.textSoft,
                                topLeft = Offset(p.x - layout.size.width / 2f, p.y - layout.size.height / 2f)
                            )
                        }
                    }
                }
        ) {
            Image(
                painter = painterResource(R.drawable.emergency_knob),
                contentDescription = null,
                modifier = Modifier
                    .offset { IntOffset(0, (centerY - knobRadius).roundToPx()) }
                    .align(Alignment.TopCenter)
                    .size(knobRadius * 2)
                    .graphicsLayer { rotationZ = rotation.value }
            )
        }
    }
}

@Composable
private fun rememberDialTick(): () -> Unit {
    val bank = rememberSoundBank(R.raw.dial_tick)
    return remember(bank) {
        { bank.play(R.raw.dial_tick, volume = 0.55f, rate = 0.94f + Random.nextFloat() * 0.12f) }
    }
}
