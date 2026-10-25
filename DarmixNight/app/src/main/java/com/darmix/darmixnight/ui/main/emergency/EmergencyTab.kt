package com.darmix.darmixnight.ui.main.emergency

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.darmix.darmixnight.OverlayManager
import com.darmix.darmixnight.ScheduleUtils
import com.darmix.darmixnight.ui.components.DnGlyph
import com.darmix.darmixnight.ui.components.Glyph
import com.darmix.darmixnight.ui.components.rememberSceneClock
import com.darmix.darmixnight.ui.main.SectionHeader
import com.darmix.darmixnight.ui.main.TimeText
import com.darmix.darmixnight.ui.theme.DnTheme
import com.darmix.darmixnight.ui.theme.DnType
import com.darmix.darmixnight.ui.theme.Manrope
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private val SirenRed = Color(0xFFE5484D)
private val SirenRedDeep = Color(0xFFC62F3A)
private val SirenHot = Color(0xFFFF9A9D)
private val SirenDark = Color(0xFF8E1F25)
private val DomeOffLight = Color(0xFF6B7290)
private val DomeOffDark = Color(0xFF2A3049)
private val Steel = Color(0xFF3A4160)
private val TapeGold = Color(0xFFE8C796)
private val TapeInk = Color(0xFF171A27)
private val PlateMuted = Color(0xFF7D869E)
private val DayLetters = listOf("L", "M", "M", "J", "V", "S", "D")
private const val MIN_DURATION = 5
private const val MAX_DURATION = 60
private const val STEP = 5

private enum class CaseState { READY, SEALED, IDLE }

@Composable
internal fun EmergencyTab(
    usageTimestamps: List<Long>,
    durationMinutes: Int,
    now: Int,
    startMinutes: Int,
    endMinutes: Int,
    paused: Boolean,
    editable: Boolean,
    onChangeDuration: (Int) -> Unit
) {
    val colors = DnTheme.colors
    val blocking = !paused && ScheduleUtils.isWithinBlockRange(now, startMinutes, endMinutes)
    val window = ScheduleUtils.isWithinEmergencyUnlockWindow(now)
    val state = when {
        blocking && window -> CaseState.READY
        blocking -> CaseState.SEALED
        else -> CaseState.IDLE
    }
    val opensAt = if (TimeText.between(now, ScheduleUtils.EMERGENCY_WINDOW_START) <= TimeText.between(now, endMinutes)) {
        ScheduleUtils.EMERGENCY_WINDOW_START
    } else {
        endMinutes
    }
    val accentRed = if (colors.isDark) SirenRed else SirenRedDeep

    Column(Modifier.fillMaxWidth()) {
        EmergencyCase(state = state, opensAt = opensAt)
        Spacer(Modifier.height(6.dp))
        val (label, detail) = when (state) {
            CaseState.READY -> "LISTA PARA USAR" to "Si te bloqueas ahora, puedes abrirla."
            CaseState.SEALED -> "SELLADA" to "De noche no hay salida. Se abre a las ${TimeText.full(opensAt)}."
            CaseState.IDLE -> "EN RESERVA" to if (paused) "El servicio está en pausa." else "Solo existe para imprevistos de día."
        }
        val tone = if (state == CaseState.READY) accentRed else colors.accent
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(8.dp)
                    .background(tone, CircleShape)
            )
            Spacer(Modifier.width(8.dp))
            Text(label, style = DnType.overline.copy(letterSpacing = 2.4.sp, fontWeight = FontWeight.Bold), color = tone)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            detail,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textDim,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(30.dp))
        SectionHeader(title = "CÓMO SE ABRE", hint = "")
        Spacer(Modifier.height(14.dp))
        OpenSteps(durationMinutes, accentRed)

        Spacer(Modifier.height(28.dp))
        SectionHeader(title = "TIEMPO DE USO", hint = if (editable) "Toca o desliza" else "Bloqueado de noche")
        Spacer(Modifier.height(12.dp))
        DurationPicker(durationMinutes, editable, accentRed, onChangeDuration)

        Spacer(Modifier.height(28.dp))
        WeekTrail(usageTimestamps, accentRed)
    }
}

private class CaseLook(
    val frameTop: Color,
    val frameBottom: Color,
    val glass: Color,
    val outline: Color,
    val badge: Color,
    val accent: Color
)

@Composable
private fun EmergencyCase(state: CaseState, opensAt: Int) {
    val colors = DnTheme.colors
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val clock = rememberSceneClock()
    val measurer = rememberTextMeasurer()
    val wobble = remember { Animatable(0f) }
    val tape = remember { Animatable(if (state == CaseState.SEALED) 1f else 0f) }
    val power = remember { Animatable(if (state == CaseState.READY) 1f else 0f) }

    LaunchedEffect(state) {
        launch {
            if (state == CaseState.SEALED) {
                tape.snapTo(0f)
                tape.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMediumLow))
            } else {
                tape.animateTo(0f, tween(240))
            }
        }
        power.animateTo(if (state == CaseState.READY) 1f else 0f, tween(700))
    }

    val look = if (colors.isDark) {
        CaseLook(Color(0xFF2A3555), Color(0xFF161D33), Color(0xFF0A0F1F), colors.text, colors.card, colors.accent)
    } else {
        CaseLook(Color(0xFFD9DFEA), Color(0xFFB4BED0), Color(0xFF1A2340), colors.text, colors.card, colors.accent)
    }
    val plateText = if (state == CaseState.SEALED) "SE ABRE A LAS ${TimeText.full(opensAt)}" else "MANTÉN ${OverlayManager.HOLD_DURATION_MS / 1000} s PARA ABRIR"
    val plateStyle = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 1.4.sp, color = TapeInk)
    val tapeStyle = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 1.8.sp, color = TapeInk)
    val currentState by rememberUpdatedState(state)

    Spacer(
        Modifier
            .fillMaxWidth()
            .height(250.dp)
            .semantics { contentDescription = "Caja de desbloqueo de emergencia" }
            .pointerInput(Unit) {
                detectTapGestures {
                    haptics.performHapticFeedback(
                        if (currentState == CaseState.READY) HapticFeedbackType.LongPress else HapticFeedbackType.TextHandleMove
                    )
                    scope.launch {
                        wobble.snapTo(0f)
                        wobble.animateTo(0f, keyframes {
                            durationMillis = 520
                            3.5f at 60
                            -3.5f at 140
                            2.5f at 230
                            -1.5f at 320
                            0f at 520
                        })
                    }
                }
            }
            .drawWithCache {
                val plate = measurer.measure(plateText, plateStyle)
                val tapeLabel = measurer.measure("SELLADO DE NOCHE", tapeStyle)
                onDrawBehind {
                    drawCase(look, clock.value, power.value, tape.value, wobble.value, plate, tapeLabel, state == CaseState.SEALED)
                }
            }
    )
}

private fun DrawScope.drawCase(
    look: CaseLook,
    t: Float,
    power: Float,
    tape: Float,
    wobble: Float,
    plate: TextLayoutResult,
    tapeLabel: TextLayoutResult,
    sealed: Boolean
) {
    val u = density
    val cw = 236f * u
    val ch = 196f * u
    val x0 = (size.width - cw) / 2f
    val y0 = (size.height - ch) / 2f
    val cx = size.width / 2f
    val center = Offset(cx, y0 + ch / 2f)

    if (power > 0.01f) {
        val pulse = 0.8f + 0.2f * sin(t * 4f)
        drawCircle(
            Brush.radialGradient(
                listOf(SirenRed.copy(alpha = 0.32f * power * pulse), Color.Transparent),
                center = center,
                radius = 200f * u
            ),
            radius = 200f * u,
            center = center
        )
    }

    rotate(wobble, center) {
        drawRoundRect(
            Brush.verticalGradient(listOf(look.frameTop, look.frameBottom), startY = y0, endY = y0 + ch),
            Offset(x0, y0),
            Size(cw, ch),
            CornerRadius(26f * u)
        )
        drawRoundRect(look.outline.copy(alpha = 0.12f), Offset(x0, y0), Size(cw, ch), CornerRadius(26f * u), style = Stroke(1f * u))
        drawRoundRect(Color.White.copy(alpha = 0.08f), Offset(x0 + 3f * u, y0 + 3f * u), Size(cw - 6f * u, ch - 6f * u), CornerRadius(23f * u), style = Stroke(1f * u))
        listOf(
            Offset(x0 + 12f * u, y0 + 12f * u),
            Offset(x0 + cw - 12f * u, y0 + 12f * u),
            Offset(x0 + 12f * u, y0 + ch - 12f * u),
            Offset(x0 + cw - 12f * u, y0 + ch - 12f * u)
        ).forEach { s ->
            drawCircle(look.frameBottom, 3.6f * u, s)
            drawCircle(Color.White.copy(alpha = 0.25f), 3.6f * u, s, style = Stroke(0.8f * u))
            drawLine(Color.White.copy(alpha = 0.35f), Offset(s.x - 2f * u, s.y - 2f * u), Offset(s.x + 2f * u, s.y + 2f * u), 1.2f * u)
        }

        val gx = x0 + 18f * u
        val gy = y0 + 18f * u
        val gw = cw - 36f * u
        val gh = ch - 66f * u
        val glass = Path().apply { addRoundRect(RoundRect(gx, gy, gx + gw, gy + gh, CornerRadius(14f * u))) }
        clipPath(glass) {
            drawRect(look.glass, Offset(gx, gy), Size(gw, gh))
            val dcx = cx
            val dcy = gy + gh - 22f * u
            if (power > 0.01f) {
                val spin = t * 200f
                for (k in 0 until 2) {
                    val a = Math.toRadians((spin + k * 180f).toDouble()).toFloat()
                    val spread = 0.24f
                    val reach = 220f * u
                    val origin = Offset(dcx, dcy - 22f * u)
                    val beam = Path().apply {
                        moveTo(origin.x, origin.y)
                        lineTo(origin.x + reach * cos(a - spread), origin.y + reach * sin(a - spread))
                        lineTo(origin.x + reach * cos(a + spread), origin.y + reach * sin(a + spread))
                        close()
                    }
                    drawPath(beam, SirenRed, alpha = 0.22f * power)
                }
                drawCircle(SirenRed.copy(alpha = 0.3f * power), 50f * u, Offset(dcx, dcy - 12f * u))
            }
            drawRoundRect(Steel, Offset(dcx - 44f * u, dcy), Size(88f * u, 14f * u), CornerRadius(5f * u))
            drawRoundRect(Color.White.copy(alpha = 0.12f), Offset(dcx - 44f * u, dcy), Size(88f * u, 4f * u), CornerRadius(2f * u))
            drawRoundRect(Steel, Offset(dcx - 6f * u, dcy - 40f * u), Size(12f * u, 7f * u), CornerRadius(3f * u))
            val dome = Path().apply {
                arcTo(Rect(dcx - 34f * u, dcy - 34f * u, dcx + 34f * u, dcy + 34f * u), 180f, 180f, false)
                close()
            }
            val domeCenter = Offset(dcx - 8f * u, dcy - 20f * u)
            drawPath(dome, Brush.radialGradient(listOf(DomeOffLight, DomeOffDark), center = domeCenter, radius = 46f * u))
            if (power > 0.01f) {
                drawPath(
                    dome,
                    Brush.radialGradient(listOf(SirenHot, SirenRed, SirenDark), center = domeCenter, radius = 46f * u),
                    alpha = power
                )
            }
            val shine = Path().apply {
                moveTo(dcx - 20f * u, dcy - 14f * u)
                quadraticTo(dcx - 18f * u, dcy - 26f * u, dcx - 4f * u, dcy - 28f * u)
            }
            drawPath(shine, Color.White, alpha = 0.25f + 0.45f * power, style = Stroke(3f * u, cap = StrokeCap.Round))

            val sweep = ((t % 5.5f) / 5.5f) * (gw + 160f * u) - 80f * u
            floatArrayOf(0f, 26f, 44f).forEachIndexed { i, dx ->
                val x = gx + sweep + dx * u
                drawLine(
                    Color.White.copy(alpha = 0.06f),
                    Offset(x, gy),
                    Offset(x - 60f * u, gy + gh),
                    strokeWidth = (if (i == 1) 6f else 14f) * u
                )
            }

            if (tape > 0.01f) {
                val tc = Offset(cx, gy + gh / 2f)
                val grow = 1.35f - 0.35f * tape
                rotate(-16f, tc) {
                    scale(grow, grow, tc) {
                        val left = x0 - 20f * u
                        val bandW = cw + 40f * u
                        val bandH = 28f * u
                        val top = tc.y - bandH / 2f
                        drawRect(TapeGold, Offset(left, top), Size(bandW, bandH), alpha = tape)
                        var i = -2
                        while (i < 16) {
                            val sx = left + i * 20f * u
                            val stripe = Path().apply {
                                moveTo(sx, top)
                                lineTo(sx + 14f * u, top)
                                lineTo(sx, top + bandH)
                                lineTo(sx - 14f * u, top + bandH)
                                close()
                            }
                            drawPath(stripe, TapeInk, alpha = 0.9f * tape)
                            i++
                        }
                        drawRoundRect(TapeGold, Offset(cx - 70f * u, tc.y - 10f * u), Size(140f * u, 20f * u), CornerRadius(3f * u), alpha = tape)
                        drawText(
                            tapeLabel,
                            topLeft = Offset(cx - tapeLabel.size.width / 2f, tc.y - tapeLabel.size.height / 2f),
                            alpha = tape
                        )
                    }
                }
            }
        }
        drawPath(glass, Color.White.copy(alpha = 0.18f), style = Stroke(1.5f * u))

        val py = y0 + ch - 38f * u
        val plateColor = if (sealed) PlateMuted else TapeGold
        drawRoundRect(plateColor, Offset(cx - 86f * u, py), Size(172f * u, 24f * u), CornerRadius(6f * u))
        drawCircle(TapeInk.copy(alpha = 0.5f), 2f * u, Offset(cx - 78f * u, py + 12f * u))
        drawCircle(TapeInk.copy(alpha = 0.5f), 2f * u, Offset(cx + 78f * u, py + 12f * u))
        drawText(plate, topLeft = Offset(cx - plate.size.width / 2f, py + 12f * u - plate.size.height / 2f))

        if (tape > 0.01f) {
            val lc = Offset(x0 + cw - 14f * u, y0 + ch - 70f * u)
            val s = 0.6f + 0.4f * tape
            scale(s, s, lc) {
                drawCircle(look.badge, 18f * u, lc, alpha = tape)
                drawCircle(look.accent, 18f * u, lc, alpha = tape, style = Stroke(1.5f * u))
                val g = 0.8f * u
                translate(lc.x - 12f * g, lc.y - 12f * g) {
                    scale(g, g, Offset.Zero) {
                        drawPath(
                            DnGlyph.LOCK.path,
                            look.accent,
                            alpha = tape,
                            style = Stroke(2.2f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OpenSteps(durationMinutes: Int, red: Color) {
    val colors = DnTheme.colors
    val steps = listOf(
        Triple(DnGlyph.HOLD, "Mantén", "${OverlayManager.HOLD_DURATION_MS / 1000} s"),
        Triple(DnGlyph.KEY, "Escribe", "“${OverlayManager.REQUIRED_PHRASE}”"),
        Triple(DnGlyph.HOURGLASS, "Usa", "$durationMinutes min")
    )
    Box(Modifier.fillMaxWidth()) {
        Spacer(
            Modifier
                .fillMaxWidth()
                .height(54.dp)
                .drawBehind {
                    val cell = size.width / 3f
                    drawLine(
                        red.copy(alpha = 0.5f),
                        Offset(cell / 2f, size.height / 2f),
                        Offset(size.width - cell / 2f, size.height / 2f),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 5.dp.toPx()))
                    )
                }
        )
        Row(Modifier.fillMaxWidth()) {
            steps.forEachIndexed { i, (glyph, title, detail) ->
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(54.dp), contentAlignment = Alignment.Center) {
                        Box(
                            Modifier
                                .size(48.dp)
                                .background(colors.card, CircleShape)
                                .border(1.5.dp, red.copy(alpha = 0.55f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Glyph(glyph, red, size = 22.dp, strokeWidth = 2f)
                        }
                        Box(
                            Modifier
                                .align(Alignment.TopEnd)
                                .size(18.dp)
                                .background(red, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "${i + 1}",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, letterSpacing = 0.sp),
                                color = Color.White
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(title, style = MaterialTheme.typography.labelLarge, color = colors.text)
                    Text(
                        detail,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textDim,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DurationPicker(value: Int, enabled: Boolean, red: Color, onChange: (Int) -> Unit) {
    val colors = DnTheme.colors
    val haptics = LocalHapticFeedback.current
    val latest by rememberUpdatedState(value)
    val change: (Int) -> Unit = { next ->
        val clamped = next.coerceIn(MIN_DURATION, MAX_DURATION)
        if (clamped != latest) {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onChange(clamped)
        }
    }
    Column(
        Modifier
            .fillMaxWidth()
            .background(colors.card.copy(alpha = if (colors.isDark) 0.72f else 0.85f), RoundedCornerShape(22.dp))
            .border(1.dp, colors.text.copy(alpha = 0.08f), RoundedCornerShape(22.dp))
            .alpha(if (enabled) 1f else 0.55f)
            .padding(horizontal = 18.dp, vertical = 16.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            RoundStep(DnGlyph.MINUS, enabled && value > MIN_DURATION) { change(value - STEP) }
            Row(
                Modifier.weight(1f),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.Bottom
            ) {
                AnimatedContent(
                    targetState = value,
                    transitionSpec = {
                        val up = targetState > initialState
                        (slideInVertically(tween(260)) { if (up) it / 2 else -it / 2 } + fadeIn(tween(220))) togetherWith
                                (slideOutVertically(tween(260)) { if (up) -it / 2 else it / 2 } + fadeOut(tween(160))) using
                                SizeTransform(clip = false)
                    },
                    label = "duration"
                ) { minutes ->
                    Text(
                        "$minutes",
                        style = DnType.clock.copy(fontSize = 54.sp, lineHeight = 58.sp),
                        color = colors.text
                    )
                }
                Text(
                    "min",
                    style = DnType.clockMedium.copy(fontSize = 16.sp),
                    color = colors.textDim,
                    modifier = Modifier.padding(start = 4.dp, bottom = 10.dp)
                )
            }
            RoundStep(DnGlyph.PLUS, enabled && value < MAX_DURATION) { change(value + STEP) }
        }
        Spacer(Modifier.height(10.dp))
        Ruler(value, enabled, red) { change(it) }
    }
}

@Composable
private fun RoundStep(glyph: DnGlyph, enabled: Boolean, onClick: () -> Unit) {
    val colors = DnTheme.colors
    Box(
        Modifier
            .size(42.dp)
            .background(colors.raised.copy(alpha = if (enabled) 1f else 0.5f), CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Glyph(glyph, colors.text.copy(alpha = if (enabled) 1f else 0.4f), size = 20.dp, strokeWidth = 2.4f)
    }
}

@Composable
private fun Ruler(value: Int, enabled: Boolean, red: Color, onPick: (Int) -> Unit) {
    val colors = DnTheme.colors
    val measurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.sp, color = colors.textDim)
    val count = (MAX_DURATION - MIN_DURATION) / STEP
    val pick: (Float, Float) -> Unit = { x, width ->
        val pad = 8.dp.value
        val fraction = ((x - pad) / (width - pad * 2)).coerceIn(0f, 1f)
        onPick(MIN_DURATION + (fraction * count).roundToInt() * STEP)
    }
    val marker by androidx.compose.animation.core.animateFloatAsState(
        targetValue = (value - MIN_DURATION) / (MAX_DURATION - MIN_DURATION).toFloat(),
        animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMedium),
        label = "rulerMarker"
    )
    Spacer(
        Modifier
            .fillMaxWidth()
            .height(52.dp)
            .pointerInput(enabled) {
                if (enabled) detectTapGestures { pick(it.x / density, size.width / density) }
            }
            .pointerInput(enabled) {
                if (enabled) detectHorizontalDragGestures { change, _ -> pick(change.position.x / density, size.width / density) }
            }
            .drawWithCache {
                val labels = listOf(5, 15, 30, 45, 60).associateWith { measurer.measure("$it", labelStyle) }
                onDrawBehind {
                    val pad = 8.dp.toPx()
                    val x0 = pad
                    val x1 = size.width - pad
                    val baseY = 30.dp.toPx()
                    for (i in 0..count) {
                        val v = MIN_DURATION + i * STEP
                        val x = x0 + (x1 - x0) * i / count
                        val big = v in labels.keys
                        val on = v <= value
                        drawLine(
                            if (on) red else colors.text.copy(alpha = 0.3f),
                            Offset(x, baseY - (if (big) 12.dp.toPx() else 7.dp.toPx())),
                            Offset(x, baseY),
                            strokeWidth = 2.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                        labels[v]?.let { layout ->
                            drawText(layout, topLeft = Offset(x - layout.size.width / 2f, baseY + 6.dp.toPx()))
                        }
                    }
                    val mx = x0 + (x1 - x0) * marker
                    val tip = Path().apply {
                        moveTo(mx - 6.dp.toPx(), baseY - 24.dp.toPx())
                        lineTo(mx + 6.dp.toPx(), baseY - 24.dp.toPx())
                        lineTo(mx, baseY - 15.dp.toPx())
                        close()
                    }
                    drawPath(tip, red)
                }
            }
    )
}

@Composable
private fun WeekTrail(timestamps: List<Long>, red: Color) {
    val colors = DnTheme.colors
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now(zone)
    val days = (6 downTo 0).map { today.minusDays(it.toLong()) }
    val perDay = days.map { day ->
        timestamps.count { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() == day }
    }
    val total = perDay.sum()
    SectionHeader(
        title = "TUS ÚLTIMOS 7 DÍAS",
        hint = when (total) {
            0 -> "0 usos"
            1 -> "1 uso"
            else -> "$total usos"
        }
    )
    Spacer(Modifier.height(14.dp))
    Row(Modifier.fillMaxWidth()) {
        days.forEachIndexed { i, day ->
            val used = perDay[i]
            val isToday = i == days.lastIndex
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                    if (used > 0) {
                        Box(
                            Modifier
                                .size(34.dp)
                                .background(red.copy(alpha = 0.18f), CircleShape)
                        )
                        Box(
                            Modifier
                                .size(24.dp)
                                .background(red, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                if (used == 1) "!" else "$used",
                                style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 0.sp),
                                color = Color.White
                            )
                        }
                    } else {
                        Box(
                            Modifier
                                .size(24.dp)
                                .background(colors.card, CircleShape)
                                .border(1.dp, colors.text.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Glyph(DnGlyph.MOON, colors.accent, size = 12.dp, strokeWidth = 2.2f)
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    DayLetters[day.dayOfWeek.value - 1],
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium
                    ),
                    color = if (isToday) colors.text else colors.textDim
                )
            }
        }
    }
    Spacer(Modifier.height(12.dp))
    Text(
        when {
            total == 0 -> "Ninguna emergencia esta semana. Así se descansa."
            total <= 2 -> "Úsala solo cuando de verdad la necesites."
            else -> "Cada uso le quita minutos a tu descanso."
        },
        style = MaterialTheme.typography.bodySmall,
        color = colors.textDim,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
}
