package com.darmix.darmixnight.ui.main.night

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.darmix.darmixnight.ui.components.DnGlyph
import com.darmix.darmixnight.ui.components.Glyph
import com.darmix.darmixnight.ui.main.ScheduleEdge
import com.darmix.darmixnight.ui.main.TimeText
import com.darmix.darmixnight.ui.theme.DnTheme
import com.darmix.darmixnight.ui.theme.DnType
import com.darmix.darmixnight.ui.theme.InstrumentSerif
import kotlinx.coroutines.delay
import java.time.LocalTime

private const val DAY_SECONDS = 86_400
private val PencilPath: Path = PathParser()
    .parsePathString("M6 17.5 L6.5 14.5 L15 6 L18 9 L9.5 17.5 Z M13.25 7.75 L16.25 10.75 M4.5 20.5 H13")
    .toPath()

private fun moduloDay(value: Int): Int = ((value % DAY_SECONDS) + DAY_SECONDS) % DAY_SECONDS

@Composable
internal fun NightTab(
    startMinutes: Int,
    endMinutes: Int,
    paused: Boolean,
    editable: Boolean,
    onPick: (ScheduleEdge) -> Unit,
    modifier: Modifier = Modifier
) {
    var secondOfDay by remember { mutableIntStateOf(LocalTime.now().toSecondOfDay()) }
    LaunchedEffect(Unit) {
        while (true) {
            secondOfDay = LocalTime.now().toSecondOfDay()
            delay(1_000L - System.currentTimeMillis() % 1_000L + 5L)
        }
    }
    val mode = nightModeOf(secondOfDay / 60, startMinutes, endMinutes, paused)
    val secondsLeft = if (mode == NightMode.BLOCKING) {
        moduloDay(endMinutes * 60 - secondOfDay)
    } else {
        moduloDay(startMinutes * 60 - secondOfDay)
    }
    val f = DnTheme.scale.factor
    val canEdit = editable && mode != NightMode.BLOCKING

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(1f))
        Headline(mode = mode, secondsLeft = secondsLeft, f = f)
        Crest(mode = mode, f = f)
        Spacer(Modifier.height((18 * f).dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            TimeCard(
                label = "Hora de dormir",
                minutes = startMinutes,
                glyph = DnGlyph.MOON,
                tint = DnTheme.colors.ember,
                outerLeft = true,
                editable = canEdit,
                f = f,
                onClick = { onPick(ScheduleEdge.START) },
                modifier = Modifier.weight(1f)
            )
            TimeCard(
                label = "Hora de despertar",
                minutes = endMinutes,
                glyph = DnGlyph.SUN,
                tint = DnTheme.colors.amber,
                outerLeft = false,
                editable = canEdit,
                f = f,
                onClick = { onPick(ScheduleEdge.END) },
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height((22 * f).dp))
        FootLine(
            text = if (mode == NightMode.PAUSED) {
                "reactívalo desde Ajustes"
            } else {
                "${TimeText.span(TimeText.between(startMinutes, endMinutes))} de sueño"
            },
            f = f
        )
        Spacer(Modifier.height((22 * f).dp))
    }
}

@Composable
private fun Headline(mode: NightMode, secondsLeft: Int, f: Float) {
    val colors = DnTheme.colors
    AnimatedContent(
        targetState = mode,
        transitionSpec = {
            (fadeIn(tween(700, delayMillis = 200)) + slideInVertically(tween(800, easing = FastOutSlowInEasing)) { it / 12 }) togetherWith
                    fadeOut(tween(350))
        },
        contentAlignment = Alignment.Center,
        label = "headline"
    ) { current ->
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                when (current) {
                    NightMode.BEFORE -> "Bloqueo en:"
                    NightMode.BLOCKING -> "Despiertas en:"
                    NightMode.PAUSED -> "El bloqueo está"
                },
                style = DnType.lead.copy(fontSize = (27 * f).sp, lineHeight = (32 * f).sp),
                color = colors.textSoft
            )
            Spacer(Modifier.height((6 * f).dp))
            if (current == NightMode.PAUSED) {
                Text(
                    "en pausa",
                    style = DnType.countdown.copy(
                        fontStyle = FontStyle.Italic,
                        fontSize = (92 * f).sp,
                        lineHeight = (96 * f).sp,
                        letterSpacing = (-0.02).em
                    ),
                    color = colors.textSoft,
                    maxLines = 1,
                    softWrap = false
                )
            } else {
                Countdown(secondsLeft = secondsLeft, f = f)
            }
        }
    }
}

@Composable
private fun Countdown(secondsLeft: Int, f: Float) {
    val colors = DnTheme.colors
    val totalMinutes = ((secondsLeft + 59) / 60).coerceAtLeast(1)
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    val unit = SpanStyle(
        fontFamily = InstrumentSerif,
        fontStyle = FontStyle.Italic,
        fontSize = (50 * f).sp,
        color = colors.ember,
        letterSpacing = 0.em
    )
    val text = buildAnnotatedString {
        if (hours > 0) {
            append(hours.toString())
            withStyle(unit) { append(" h ") }
            append(minutes.toString())
        } else {
            append(minutes.toString())
        }
        withStyle(unit) { append(" min") }
    }
    AnimatedContent(
        targetState = text,
        transitionSpec = {
            (fadeIn(tween(420)) + slideInVertically(tween(480, easing = FastOutSlowInEasing)) { -it / 6 }) togetherWith
                    (fadeOut(tween(260)) + slideOutVertically(tween(420)) { it / 8 })
        },
        contentAlignment = Alignment.Center,
        label = "countdown"
    ) { value: AnnotatedString ->
        Text(
            value,
            style = DnType.countdown.copy(fontSize = (100 * f).sp, lineHeight = (100 * f).sp),
            color = colors.text,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.semantics {
                contentDescription = if (hours > 0) "$hours horas y $minutes minutos" else "$minutes minutos"
            }
        )
    }
}

@Composable
private fun Crest(mode: NightMode, f: Float) {
    val colors = DnTheme.colors
    val strength by animateFloatAsState(if (mode == NightMode.PAUSED) 0.35f else 1f, tween(1200), label = "crest")
    Spacer(
        Modifier
            .fillMaxWidth(0.8f)
            .height((30 * f).dp)
            .graphicsLayer { alpha = strength }
            .drawWithCache {
                val w = size.width
                val h = size.height
                val arc = Path().apply {
                    moveTo(0f, h * 0.92f)
                    quadraticTo(w / 2f, h * 0.05f, w, h * 0.92f)
                }
                val line = Brush.horizontalGradient(
                    0f to colors.ember.copy(alpha = 0f),
                    0.3f to colors.ember.copy(alpha = 0.55f),
                    0.5f to colors.flame,
                    0.7f to colors.ember.copy(alpha = 0.55f),
                    1f to colors.ember.copy(alpha = 0f)
                )
                val haze = Brush.radialGradient(
                    0f to colors.ember.copy(alpha = 0.28f),
                    1f to colors.ember.copy(alpha = 0f),
                    center = Offset.Zero,
                    radius = 1f
                )
                val glowStroke = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round)
                val thinStroke = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round)
                onDrawBehind {
                    translate(w / 2f, h * 0.5f) {
                        scale(w * 0.38f, h * 0.9f, Offset.Zero) {
                            drawCircle(haze, 1f, Offset.Zero)
                        }
                    }
                    drawPath(arc, line, alpha = 0.22f, style = glowStroke)
                    drawPath(arc, line, style = thinStroke)
                }
            }
    )
}

@Composable
private fun TimeCard(
    label: String,
    minutes: Int,
    glyph: DnGlyph,
    tint: Color,
    outerLeft: Boolean,
    editable: Boolean,
    f: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = DnTheme.colors
    val shape = RoundedCornerShape(50)
    val pencil by animateFloatAsState(if (editable) 1f else 0f, tween(500), label = "pencil")
    val iconSize = (26 * f).dp
    val time = buildAnnotatedString {
        append(TimeText.hour(minutes))
        withStyle(
            SpanStyle(
                fontFamily = InstrumentSerif,
                fontSize = (19 * f).sp,
                color = colors.ember,
                letterSpacing = 0.02.em
            )
        ) { append(" " + TimeText.period(minutes)) }
    }

    Box(modifier) {
        Column(
            modifier = Modifier
                .padding(top = iconSize / 2)
                .fillMaxWidth()
                .clip(shape)
                .clickable(enabled = editable, onClick = onClick)
                .semantics(mergeDescendants = true) {
                    role = Role.Button
                    contentDescription = "$label, ${TimeText.full(minutes)}" + if (editable) ", toca para cambiar" else ""
                }
                .drawWithCache {
                    val stroke = 1.dp.toPx()
                    val radius = CornerRadius(size.height / 2f)
                    val inset = stroke / 2f
                    val gap = iconSize.toPx() * 0.9f
                    val fill = Brush.verticalGradient(
                        0f to colors.ink.copy(alpha = 0.55f),
                        1f to Color(0xFF1A0C07).copy(alpha = 0.75f)
                    )
                    val edge = Brush.horizontalGradient(
                        if (outerLeft) 0f to colors.ember.copy(alpha = 0.6f) else 0f to colors.ember.copy(alpha = 0.16f),
                        if (outerLeft) 1f to colors.ember.copy(alpha = 0.16f) else 1f to colors.ember.copy(alpha = 0.6f)
                    )
                    onDrawBehind {
                        drawRoundRect(fill, cornerRadius = radius)
                        clipRect(
                            left = size.width / 2f - gap / 2f,
                            top = -stroke,
                            right = size.width / 2f + gap / 2f,
                            bottom = stroke * 3f,
                            clipOp = ClipOp.Difference
                        ) {
                            drawRoundRect(
                                edge,
                                topLeft = Offset(inset, inset),
                                size = Size(size.width - stroke, size.height - stroke),
                                cornerRadius = CornerRadius(radius.x - inset),
                                style = Stroke(stroke)
                            )
                        }
                    }
                }
                .padding(top = (iconSize / 2) + (8 * f).dp, bottom = (10 * f).dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                label,
                style = TextStyle(fontFamily = InstrumentSerif, fontSize = (17 * f).sp, lineHeight = (20 * f).sp),
                color = colors.textSoft,
                maxLines = 1
            )
            Spacer(Modifier.height((2 * f).dp))
            Text(
                time,
                style = DnType.timeLarge.copy(fontSize = (40 * f).sp, lineHeight = (44 * f).sp, letterSpacing = (-0.01).em),
                color = colors.text,
                maxLines = 1,
                softWrap = false
            )
            Spacer(Modifier.height((4 * f).dp))
            Spacer(
                Modifier
                    .size((20 * f).dp)
                    .graphicsLayer { alpha = pencil }
                    .drawWithCache {
                        val k = size.minDimension / 24f
                        val stroke = Stroke(width = 1.7f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                        onDrawBehind {
                            scale(k, k, Offset.Zero) {
                                drawPath(PencilPath, colors.ember, style = stroke)
                            }
                        }
                    }
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .size(iconSize),
            contentAlignment = Alignment.Center
        ) {
            Glyph(glyph, tint, size = iconSize, strokeWidth = 1.9f)
        }
    }
}

@Composable
private fun FootLine(text: String, f: Float) {
    val colors = DnTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Hairline(fadeToRight = false, modifier = Modifier.weight(1f))
        AnimatedContent(
            targetState = text,
            transitionSpec = { fadeIn(tween(500, delayMillis = 150)) togetherWith fadeOut(tween(250)) },
            label = "foot"
        ) { value ->
            Text(
                value,
                style = DnType.note.copy(fontSize = (20 * f).sp, lineHeight = (24 * f).sp),
                color = colors.clay,
                maxLines = 1,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
        Hairline(fadeToRight = true, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun Hairline(fadeToRight: Boolean, modifier: Modifier = Modifier) {
    val colors = DnTheme.colors
    Spacer(
        modifier
            .height(1.dp)
            .drawWithCache {
                val brush = Brush.horizontalGradient(
                    if (fadeToRight) 0f to colors.ember.copy(alpha = 0.45f) else 0f to colors.ember.copy(alpha = 0f),
                    if (fadeToRight) 1f to colors.ember.copy(alpha = 0f) else 1f to colors.ember.copy(alpha = 0.45f)
                )
                onDrawBehind { drawRect(brush) }
            }
    )
}
