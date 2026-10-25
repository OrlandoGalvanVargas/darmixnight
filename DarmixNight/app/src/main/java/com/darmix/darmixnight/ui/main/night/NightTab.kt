package com.darmix.darmixnight.ui.main.night

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.darmix.darmixnight.ScheduleUtils
import com.darmix.darmixnight.ui.main.ScheduleEdge
import com.darmix.darmixnight.ui.main.TimeText
import com.darmix.darmixnight.ui.components.DnGlyph
import com.darmix.darmixnight.ui.components.Glyph
import com.darmix.darmixnight.ui.components.rememberSceneClock
import com.darmix.darmixnight.ui.panda.PandaCharacter
import com.darmix.darmixnight.ui.panda.PandaMood
import com.darmix.darmixnight.ui.theme.DnTheme
import com.darmix.darmixnight.ui.theme.DnType
import kotlinx.coroutines.delay
import java.time.LocalTime
import kotlin.math.floor
import kotlin.math.sin

private const val DAY_SECONDS = 86_400
private val RitualLabels = listOf("Tarde", "Preparar", "Bostezo", "Ya casi", "A dormir")

private class NightMoment(
    val mood: PandaMood,
    val blocking: Boolean,
    val secondsLeft: Int,
    val ritual: Float
)

private fun moduloDay(value: Int): Int = ((value % DAY_SECONDS) + DAY_SECONDS) % DAY_SECONDS

private fun resolveMoment(secondOfDay: Int, start: Int, end: Int, paused: Boolean): NightMoment {
    val nowMinutes = secondOfDay / 60
    val inWindow = ScheduleUtils.isWithinBlockRange(nowMinutes, start, end)
    val untilStart = moduloDay(start * 60 - secondOfDay)
    val untilEnd = moduloDay(end * 60 - secondOfDay)
    val sinceEnd = TimeText.between(end, nowMinutes)
    val u = untilStart / 60f
    return when {
        paused -> NightMoment(PandaMood.PAUSED, false, untilStart, -1f)
        inWindow -> NightMoment(PandaMood.SLEEP, true, untilEnd, 4f)
        untilStart <= 60 -> NightMoment(PandaMood.SLEEP, false, untilStart, 4f)
        sinceEnd < 180 && untilStart > 3 * 3600 -> NightMoment(PandaMood.MORNING, false, untilStart, 5f)
        u > 180f -> NightMoment(PandaMood.SNACK, false, untilStart, 0f)
        u > 60f -> NightMoment(PandaMood.COCOA, false, untilStart, 1f + (180f - u) / 120f)
        u > 30f -> NightMoment(PandaMood.YAWN, false, untilStart, 2f + (60f - u) / 30f)
        else -> NightMoment(PandaMood.MOON, false, untilStart, (3f + (30f - u) / 29f).coerceAtMost(3.99f))
    }
}

private fun pandaLines(mood: PandaMood, blocking: Boolean, start: Int, end: Int): List<String> {
    val sleep = TimeText.span(TimeText.between(start, end))
    return when (mood) {
        PandaMood.SNACK -> listOf(
            "¡Aún hay tiempo! Disfruta tu tarde.",
            "¿Bambú? Está bien crujiente.",
            "Hoy a las ${TimeText.full(start)} nos vamos a dormir."
        )
        PandaMood.COCOA -> listOf(
            "Voy por mi leche tibia. Tú ve cerrando pendientes.",
            "Buen momento para bajarle al brillo.",
            "Mmm… calientita."
        )
        PandaMood.YAWN -> listOf(
            "¡Aaah! Ya me está dando sueño…",
            "¿Tú también bostezaste? Es contagioso.",
            "Prepara tu cama, ya falta poco."
        )
        PandaMood.MOON -> listOf(
            "Ya casi… deja todo listo para dormir.",
            "Conecta el cargador lejos de la cama.",
            "La luna ya salió. Shh…"
        )
        PandaMood.SLEEP -> if (blocking) listOf(
            "Zzz… nos vemos a las ${TimeText.full(end)}.",
            "Shh… estoy soñando con bambú."
        ) else listOf(
            "Buenas noches… Zzz",
            "Shh, ya me estoy durmiendo."
        )
        PandaMood.MORNING -> listOf(
            "¡Buenos días! Tu noche fue de $sleep.",
            "¡Qué buena noche! A darle con todo.",
            "Esta noche repetimos a las ${TimeText.full(start)}."
        )
        PandaMood.PAUSED -> listOf(
            "Hoy no vigilo. Vuelvo cuando me actives.",
            "Tómate la noche con calma."
        )
    }
}

private fun bubbleOnLeft(mood: PandaMood): Boolean = when (mood) {
    PandaMood.SNACK, PandaMood.COCOA, PandaMood.MORNING -> true
    else -> false
}

@Composable
internal fun NightTab(
    startMinutes: Int,
    endMinutes: Int,
    paused: Boolean,
    editable: Boolean,
    onPick: (ScheduleEdge) -> Unit
) {
    var secondOfDay by remember { mutableIntStateOf(LocalTime.now().toSecondOfDay()) }
    LaunchedEffect(Unit) {
        while (true) {
            secondOfDay = LocalTime.now().toSecondOfDay()
            delay(1_000L - System.currentTimeMillis() % 1_000L + 5L)
        }
    }
    val moment = resolveMoment(secondOfDay, startMinutes, endMinutes, paused)
    val clock = rememberSceneClock()

    Column(Modifier.fillMaxWidth()) {
        Countdown(moment, startMinutes, endMinutes, paused)
        Spacer(Modifier.height(6.dp))
        PandaScene(moment, startMinutes, endMinutes, clock)
        Spacer(Modifier.height(18.dp))
        RitualStrip(moment.ritual, clock)
        Spacer(Modifier.height(28.dp))
        RestTicket(startMinutes, endMinutes, editable, onPick)
    }
}

@Composable
private fun Countdown(moment: NightMoment, start: Int, end: Int, paused: Boolean) {
    val colors = DnTheme.colors
    val overline = when {
        paused -> "SERVICIO EN PAUSA"
        moment.blocking -> "TU NOCHE TERMINA EN"
        else -> "TU NOCHE EMPIEZA EN"
    }
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(overline, style = DnType.overline.copy(letterSpacing = 3.sp), color = colors.accent)
        Spacer(Modifier.height(6.dp))
        if (paused) {
            Text(
                "En pausa",
                style = DnType.clock.copy(fontSize = 58.sp, lineHeight = 74.sp),
                color = colors.text
            )
        } else {
            val s = moment.secondsLeft
            val parts = when {
                s >= 3600 && (s / 60) % 60 == 0 -> listOf(s / 3600 to "h")
                s >= 3600 -> listOf(s / 3600 to "h", (s / 60) % 60 to "min")
                s >= 60 -> listOf(s / 60 to "min")
                else -> listOf(s to "s")
            }
            Row(verticalAlignment = Alignment.Bottom) {
                parts.forEachIndexed { index, (value, unit) ->
                    if (index > 0) Spacer(Modifier.width(14.dp))
                    RollingNumber(value)
                    Text(
                        unit,
                        style = DnType.clockMedium.copy(fontSize = 19.sp, fontWeight = FontWeight.SemiBold),
                        color = colors.textDim,
                        modifier = Modifier.padding(start = 3.dp, bottom = 14.dp)
                    )
                }
            }
        }
        Text(
            when {
                paused -> "El bloqueo no se aplicará hasta que lo reactives"
                moment.blocking -> "Despiertas a las ${TimeText.full(end)}"
                else -> "${TimeText.full(start)} → ${TimeText.full(end)} · ${TimeText.span(TimeText.between(start, end))}"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textDim,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun RollingNumber(value: Int) {
    val colors = DnTheme.colors
    AnimatedContent(
        targetState = value,
        transitionSpec = {
            val down = targetState < initialState
            (slideInVertically(tween(360)) { if (down) -it / 2 else it / 2 } + fadeIn(tween(300))) togetherWith
                    (slideOutVertically(tween(360)) { if (down) it / 2 else -it / 2 } + fadeOut(tween(200))) using
                    SizeTransform(clip = false)
        },
        label = "rollingNumber"
    ) { number ->
        Text(
            number.toString(),
            style = DnType.clock.copy(fontSize = 78.sp, lineHeight = 80.sp, letterSpacing = (-3).sp),
            color = colors.text
        )
    }
}

@Composable
private fun PandaScene(moment: NightMoment, start: Int, end: Int, clock: State<Float>) {
    val colors = DnTheme.colors
    val lines = pandaLines(moment.mood, moment.blocking, start, end)
    var lineIndex by remember(moment.mood) { mutableIntStateOf(0) }
    var petCount by remember { mutableIntStateOf(0) }
    var petting by remember { mutableStateOf(false) }
    LaunchedEffect(petCount) {
        if (petCount > 0) {
            petting = true
            delay(2_600L)
            petting = false
        }
    }
    val message = if (petting) {
        if (moment.mood == PandaMood.SLEEP) "Mmm… gracias. Zzz" else "¡Jeje, me haces cosquillas!"
    } else {
        lines[lineIndex % lines.size]
    }
    val leftBubble = bubbleOnLeft(moment.mood)

    Box(
        Modifier
            .fillMaxWidth()
            .height(272.dp)
    ) {
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .size(width = 320.dp, height = 260.dp)
                .drawBehind {
                    val glowCenter = Offset(size.width / 2f, size.height * 0.55f)
                    drawCircle(
                        Brush.radialGradient(
                            listOf(colors.glow.copy(alpha = if (colors.isDark) 0.22f else 0.45f), Color.Transparent),
                            center = glowCenter,
                            radius = size.width * 0.5f
                        ),
                        radius = size.width * 0.5f,
                        center = glowCenter
                    )
                    val shadowWidth = if (moment.mood == PandaMood.SLEEP) size.width * 0.72f else size.width * 0.58f
                    val baseY = size.height - 6.dp.toPx()
                    drawOval(
                        colors.text.copy(alpha = 0.08f),
                        Offset((size.width - shadowWidth) / 2f, baseY - 12.dp.toPx()),
                        Size(shadowWidth, 24.dp.toPx())
                    )
                    drawOval(
                        colors.text.copy(alpha = 0.08f),
                        Offset((size.width - shadowWidth * 0.6f) / 2f, baseY - 7.dp.toPx()),
                        Size(shadowWidth * 0.6f, 14.dp.toPx())
                    )
                }
        )
        PandaCharacter(
            mood = moment.mood,
            clock = clock,
            onTap = { lineIndex++ },
            onPet = { petCount++ },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 4.dp)
                .size(230.dp)
        )
        SpeechBubble(
            text = message,
            tailOnRight = leftBubble,
            modifier = Modifier
                .align(if (leftBubble) Alignment.TopStart else Alignment.TopEnd)
                .widthIn(max = 168.dp)
        )
    }
}

private class BubbleShape(private val tailOnRight: Boolean, private val tail: Float, private val radius: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val bodyHeight = size.height - tail
        val path = Path().apply {
            addRoundRect(RoundRect(0f, 0f, size.width, bodyHeight, CornerRadius(radius)))
            val baseX = if (tailOnRight) size.width - radius - tail * 2.2f else radius + tail * 0.2f
            moveTo(baseX, bodyHeight - 1f)
            lineTo(baseX + tail * 2f, bodyHeight - 1f)
            if (tailOnRight) lineTo(baseX + tail * 2.4f, size.height) else lineTo(baseX - tail * 0.4f, size.height)
            close()
        }
        return Outline.Generic(path)
    }
}

@Composable
private fun SpeechBubble(text: String, tailOnRight: Boolean, modifier: Modifier = Modifier) {
    val colors = DnTheme.colors
    val density = androidx.compose.ui.platform.LocalDensity.current
    val shape = remember(tailOnRight, density) {
        with(density) { BubbleShape(tailOnRight, 11.dp.toPx(), 16.dp.toPx()) }
    }
    AnimatedContent(
        targetState = text,
        transitionSpec = {
            (fadeIn(tween(260, delayMillis = 80)) + scaleIn(spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow), initialScale = 0.85f)) togetherWith
                    (fadeOut(tween(140)) + scaleOut(tween(160), targetScale = 0.92f)) using
                    SizeTransform(clip = false)
        },
        contentAlignment = if (tailOnRight) Alignment.BottomEnd else Alignment.BottomStart,
        modifier = modifier,
        label = "bubble"
    ) { line ->
        Text(
            line,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, lineHeight = 19.sp),
            color = colors.text,
            modifier = Modifier
                .clip(shape)
                .background(colors.card.copy(alpha = if (colors.isDark) 0.9f else 0.95f))
                .padding(start = 14.dp, end = 14.dp, top = 11.dp, bottom = 11.dp + 11.dp)
        )
    }
}

@Composable
private fun RitualStrip(position: Float, clock: State<Float>) {
    val colors = DnTheme.colors
    val animated by animateFloatAsState(
        targetValue = position,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessVeryLow),
        label = "ritual"
    )
    val dim = position < 0f
    val current = if (dim) -1 else floor(position).toInt().coerceAtLeast(0)
    Column(Modifier.fillMaxWidth()) {
        Text("RITUAL DE SUEÑO", style = DnType.overline, color = colors.textDim)
        Spacer(Modifier.height(12.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(28.dp)
                .drawBehind {
                    val cell = size.width / RitualLabels.size
                    val y = size.height / 2f
                    val x0 = cell / 2f
                    val x1 = size.width - cell / 2f
                    val step = (x1 - x0) / (RitualLabels.size - 1)
                    drawLine(
                        colors.text.copy(alpha = 0.18f),
                        Offset(x0, y),
                        Offset(x1, y),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 6.dp.toPx()))
                    )
                    if (!dim) {
                        drawLine(
                            colors.accent,
                            Offset(x0, y),
                            Offset(x0 + step * animated.coerceIn(0f, 4f), y),
                            strokeWidth = 2.5.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                    val pulse = 0.5f + 0.5f * sin(clock.value * 2.4f)
                    for (i in RitualLabels.indices) {
                        val c = Offset(x0 + step * i, y)
                        when {
                            dim -> drawCircle(colors.text.copy(alpha = 0.25f), 4.dp.toPx(), c, style = Stroke(1.6.dp.toPx()))
                            i < current -> drawCircle(colors.accent, 5.dp.toPx(), c)
                            i == current -> {
                                drawCircle(colors.accent.copy(alpha = 0.12f + 0.12f * pulse), (12f + 4f * pulse).dp.toPx(), c)
                                drawCircle(colors.accent, 8.dp.toPx(), c)
                                drawCircle(colors.onAccent, 3.dp.toPx(), c)
                            }
                            else -> {
                                drawCircle(colors.bg, 5.dp.toPx(), c)
                                drawCircle(colors.text.copy(alpha = 0.35f), 5.dp.toPx(), c, style = Stroke(1.6.dp.toPx()))
                            }
                        }
                    }
                }
        )
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth()) {
            RitualLabels.forEachIndexed { i, label ->
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = if (i == current) FontWeight.Bold else FontWeight.Medium
                    ),
                    color = if (i == current) colors.accent else colors.textDim,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

private class TicketShape(private val corner: Float, private val notch: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val base = Path().apply { addRoundRect(RoundRect(0f, 0f, size.width, size.height, CornerRadius(corner))) }
        val bites = Path().apply {
            addOval(Rect(Offset(size.width / 2f, 0f), notch))
            addOval(Rect(Offset(size.width / 2f, size.height), notch))
        }
        return Outline.Generic(Path().apply { op(base, bites, PathOperation.Difference) })
    }
}

@Composable
private fun RestTicket(start: Int, end: Int, editable: Boolean, onPick: (ScheduleEdge) -> Unit) {
    val colors = DnTheme.colors
    val density = androidx.compose.ui.platform.LocalDensity.current
    val shape = remember(density) { with(density) { TicketShape(22.dp.toPx(), 12.dp.toPx()) } }
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("TU PASE DE DESCANSO", style = DnType.overline, color = colors.textDim, modifier = Modifier.weight(1f))
            Text(
                if (editable) "Toca para cambiar" else "Se libera a las ${TimeText.full(end)}",
                style = MaterialTheme.typography.labelMedium,
                color = colors.accent
            )
        }
        Spacer(Modifier.height(12.dp))
        Box(Modifier.fillMaxWidth()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(112.dp)
                    .clip(shape)
                    .background(colors.card.copy(alpha = if (colors.isDark) 0.72f else 0.85f))
                    .border(1.dp, colors.text.copy(alpha = 0.10f), shape)
            ) {
                TicketHalf(
                    label = "ME DUERMO",
                    minutes = start,
                    glyph = DnGlyph.MOON,
                    tint = colors.accent,
                    editable = editable,
                    onClick = { onPick(ScheduleEdge.START) },
                    modifier = Modifier.weight(1f)
                )
                Box(
                    Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .padding(vertical = 18.dp)
                        .drawBehind {
                            drawLine(
                                colors.text.copy(alpha = 0.25f),
                                Offset(0f, 0f),
                                Offset(0f, size.height),
                                strokeWidth = 1.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 5.dp.toPx()))
                            )
                        }
                )
                TicketHalf(
                    label = "DESPIERTO",
                    minutes = end,
                    glyph = DnGlyph.SUN,
                    tint = if (colors.isDark) colors.warn else colors.accentAlt,
                    editable = editable,
                    onClick = { onPick(ScheduleEdge.END) },
                    modifier = Modifier.weight(1f)
                )
            }
            this@Column.AnimatedVisibility(
                visible = !editable,
                enter = scaleIn(spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMedium), initialScale = 1.8f) + fadeIn(tween(120)),
                exit = fadeOut(tween(200)),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 10.dp)
            ) {
                Text(
                    "EN CURSO",
                    style = DnType.overline.copy(fontSize = 12.sp, letterSpacing = 3.sp, fontWeight = FontWeight.ExtraBold),
                    color = colors.accent,
                    modifier = Modifier
                        .rotate(-9f)
                        .background(colors.card, RoundedCornerShape(7.dp))
                        .border(2.dp, colors.accent, RoundedCornerShape(7.dp))
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                )
            }
        }
    }
}

@Composable
private fun TicketHalf(
    label: String,
    minutes: Int,
    glyph: DnGlyph,
    tint: Color,
    editable: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = DnTheme.colors
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clickable(enabled = editable, onClick = onClick)
            .padding(start = 18.dp, end = 14.dp, top = 16.dp, bottom = 14.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Glyph(glyph, tint, size = 17.dp, strokeWidth = 2f)
            Spacer(Modifier.width(7.dp))
            Text(label, style = DnType.overline.copy(fontSize = 10.sp), color = colors.textDim, modifier = Modifier.weight(1f))
            Glyph(if (editable) DnGlyph.PENCIL else DnGlyph.LOCK, colors.textDim, size = 15.dp, strokeWidth = 2f)
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                TimeText.hour(minutes),
                style = DnType.clock.copy(fontSize = 38.sp, lineHeight = 42.sp, letterSpacing = (-1.5).sp),
                color = colors.text
            )
            Spacer(Modifier.width(5.dp))
            Text(
                TimeText.period(minutes),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = tint,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }
    }
}
