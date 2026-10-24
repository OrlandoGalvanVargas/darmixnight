package com.darmix.darmixnight.ui.main

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.darmix.darmixnight.OverlayManager
import com.darmix.darmixnight.ScheduleUtils
import com.darmix.darmixnight.ui.components.Crescent
import com.darmix.darmixnight.ui.components.DnGlyph
import com.darmix.darmixnight.ui.components.Glyph
import com.darmix.darmixnight.ui.theme.DnTheme
import com.darmix.darmixnight.ui.theme.DnType

private val DurationOptions = (5..60 step 5).toList()

@Composable
internal fun ServiceCard(
    paused: Boolean,
    canToggle: Boolean,
    resumesAt: Int,
    onToggle: () -> Unit
) {
    val colors = DnTheme.colors
    val active = !paused
    val interactive = paused || canToggle
    val tone by animateColorAsState(if (active) colors.success else colors.textDim, label = "serviceTone")

    Surface(
        onClick = onToggle,
        enabled = interactive,
        shape = RoundedCornerShape(24.dp),
        color = colors.card.copy(alpha = if (colors.isDark) 0.82f else 0.94f),
        border = BorderStroke(1.dp, if (paused) colors.warn.copy(alpha = 0.4f) else colors.outline),
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                role = Role.Switch
                stateDescription = if (active) "Servicio activo" else "Servicio en pausa"
            }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(44.dp)
                    .background(tone.copy(alpha = 0.14f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Glyph(DnGlyph.POWER, tone, size = 22.dp, strokeWidth = 2f)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (active) "Servicio activo" else "Servicio en pausa",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.text
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    when {
                        !canToggle && active -> "No se puede pausar durante el descanso. Libre a las ${TimeText.full(resumesAt)}."
                        active -> "Protegiendo tus noches"
                        else -> "Toca para reactivar el bloqueo"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (paused) colors.warn else colors.textDim
                )
            }
            Spacer(Modifier.width(12.dp))
            PowerSwitch(on = active, enabled = interactive)
        }
    }
}

@Composable
private fun PowerSwitch(on: Boolean, enabled: Boolean) {
    val colors = DnTheme.colors
    val knob by animateDpAsState(
        if (on) 24.dp else 0.dp,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "knob"
    )
    val track by animateColorAsState(if (on) colors.success else colors.textDim.copy(alpha = 0.3f), label = "track")
    Box(
        Modifier
            .alpha(if (enabled) 1f else 0.5f)
            .width(54.dp)
            .height(30.dp)
            .background(track, CircleShape)
            .padding(3.dp)
    ) {
        Box(
            Modifier
                .offset(x = knob)
                .size(24.dp)
                .background(Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Glyph(if (on) DnGlyph.CHECK else DnGlyph.MINUS, track, size = 14.dp, strokeWidth = 2.6f)
        }
    }
}

@Composable
internal fun PauseDialog(startMinutes: Int, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val colors = DnTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.card,
        titleContentColor = colors.text,
        textContentColor = colors.textDim,
        icon = { Glyph(DnGlyph.POWER, colors.warn, size = 28.dp, strokeWidth = 2f) },
        title = { Text("¿Pausar DarmixNight?", style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center) },
        text = {
            Text(
                "Mientras esté en pausa el teléfono no se bloqueará, ni siquiera a las ${TimeText.full(startMinutes)}. " +
                        "Puedes reactivarlo cuando quieras desde aquí.",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Pausar", style = MaterialTheme.typography.labelLarge, color = colors.warn)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Mantener activo", style = MaterialTheme.typography.labelLarge, color = colors.accent)
            }
        }
    )
}

@Composable
internal fun EmergencyCard(
    weeklyUses: Int,
    durationMinutes: Int,
    now: Int,
    openNow: Boolean,
    editable: Boolean,
    onChangeDuration: (Int) -> Unit
) {
    val colors = DnTheme.colors
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = colors.card.copy(alpha = if (colors.isDark) 0.82f else 0.94f),
        border = BorderStroke(1.dp, colors.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(vertical = 20.dp)) {
            Column(Modifier.padding(horizontal = 20.dp)) {
                Text("Desbloqueo de emergencia", style = MaterialTheme.typography.titleMedium, color = colors.text)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Para imprevistos de día. De noche no existe, así nada interrumpe tu descanso.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textDim
                )
                Spacer(Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    FlowTile(DnGlyph.HOLD, "Mantén ${OverlayManager.HOLD_DURATION_MS / 1000} s", "presionado", Modifier.weight(1f))
                    FlowArrow()
                    FlowTile(DnGlyph.KEY, "Escribe la", "frase", Modifier.weight(1f))
                    FlowArrow()
                    FlowTile(DnGlyph.HOURGLASS, "$durationMinutes min", "de uso", Modifier.weight(1f), highlight = true)
                }

                Spacer(Modifier.height(20.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("DISPONIBILIDAD", style = DnType.overline, color = colors.textDim, modifier = Modifier.weight(1f))
                    Text(
                        if (openNow) "Ahora sí" else "Ahora no",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (openNow) colors.success else colors.textDim
                    )
                }
                Spacer(Modifier.height(10.dp))
                AvailabilityBand(now = now)
            }

            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("DURACIÓN", style = DnType.overline, color = colors.textDim, modifier = Modifier.weight(1f))
                if (!editable) Glyph(DnGlyph.LOCK, colors.textDim, size = 14.dp, strokeWidth = 2f)
            }
            Spacer(Modifier.height(10.dp))
            DurationChips(selected = durationMinutes, enabled = editable, onSelect = onChangeDuration)

            Spacer(Modifier.height(20.dp))
            Column(Modifier.padding(horizontal = 20.dp)) {
                WeekMeter(uses = weeklyUses)
            }
        }
    }
}

@Composable
private fun FlowTile(glyph: DnGlyph, title: String, subtitle: String, modifier: Modifier, highlight: Boolean = false) {
    val colors = DnTheme.colors
    val tone = if (highlight) colors.accent else colors.accentAlt
    Column(
        modifier = modifier
            .background(
                if (highlight) colors.accent.copy(alpha = 0.12f) else colors.raised,
                RoundedCornerShape(18.dp)
            )
            .padding(vertical = 12.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Glyph(glyph, tone, size = 22.dp, strokeWidth = 1.9f)
        Spacer(Modifier.height(6.dp))
        Text(title, style = MaterialTheme.typography.labelLarge, color = colors.text, textAlign = TextAlign.Center)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = colors.textDim, textAlign = TextAlign.Center)
    }
}

@Composable
private fun FlowArrow() {
    Glyph(
        DnGlyph.CHEVRON,
        DnTheme.colors.textDim,
        size = 16.dp,
        strokeWidth = 2f,
        modifier = Modifier
            .padding(horizontal = 2.dp)
            .graphicsLayer { rotationZ = -90f }
    )
}

@Composable
private fun AvailabilityBand(now: Int) {
    val colors = DnTheme.colors
    val open = ScheduleUtils.EMERGENCY_WINDOW_START
    val close = ScheduleUtils.EMERGENCY_WINDOW_END
    val moon = remember { Crescent.unitPath() }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val w = maxWidth
        Column {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(26.dp)
                    .drawWithCache {
                        val trackTop = 6.dp.toPx()
                        val trackH = 14.dp.toPx()
                        val radius = CornerRadius(trackH / 2f)
                        val x0 = size.width * open / 1440f
                        val x1 = size.width * close / 1440f
                        val openBrush = Brush.horizontalGradient(
                            listOf(colors.success.copy(alpha = 0.55f), colors.success.copy(alpha = 0.85f), colors.success.copy(alpha = 0.55f)),
                            startX = x0,
                            endX = x1
                        )
                        onDrawBehind {
                            drawRoundRect(colors.raised, Offset(0f, trackTop), Size(size.width, trackH), radius)
                            drawRoundRect(openBrush, Offset(x0, trackTop), Size(x1 - x0, trackH), radius)
                            val r = 5.dp.toPx()
                            val nightCenterLeft = Offset(x0 / 2f, trackTop + trackH / 2f)
                            val nightCenterRight = Offset((x1 + size.width) / 2f, trackTop + trackH / 2f)
                            listOf(nightCenterLeft, nightCenterRight).forEach { c ->
                                translate(c.x, c.y) { scale(r, r, pivot = Offset.Zero) { drawPath(moon, colors.textDim) } }
                            }
                            val nx = size.width * now / 1440f
                            drawLine(colors.text, Offset(nx, 0f), Offset(nx, size.height), 3.dp.toPx(), StrokeCap.Round)
                            drawCircle(colors.text, 4.dp.toPx(), Offset(nx, 0f))
                        }
                    }
            )
            Spacer(Modifier.height(4.dp))
            Box(Modifier.fillMaxWidth().height(14.dp)) {
                BandLabel("12 AM", 0f, w, Alignment.Start)
                BandLabel(TimeText.full(open).replace(":00", ""), open / 1440f, w)
                BandLabel(TimeText.full(close).replace(":00", ""), close / 1440f, w)
            }
        }
    }
}

@Composable
private fun BandLabel(text: String, fraction: Float, width: androidx.compose.ui.unit.Dp, align: Alignment.Horizontal = Alignment.CenterHorizontally) {
    val colors = DnTheme.colors
    val shift = if (align == Alignment.Start) 0.dp else 18.dp
    Text(
        text,
        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.sp),
        color = colors.textDim,
        modifier = Modifier.offset(x = width * fraction - shift)
    )
}

@Composable
private fun DurationChips(selected: Int, enabled: Boolean, onSelect: (Int) -> Unit) {
    val colors = DnTheme.colors
    val startIndex = (DurationOptions.indexOf(selected) - 2).coerceAtLeast(0)
    val state = rememberLazyListState(initialFirstVisibleItemIndex = startIndex)
    LazyRow(
        state = state,
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.alpha(if (enabled) 1f else 0.55f)
    ) {
        items(DurationOptions) { value ->
            val isSelected = value == selected
            val bg by animateColorAsState(if (isSelected) colors.accent else colors.raised, label = "chipBg")
            val fg by animateColorAsState(if (isSelected) colors.onAccent else colors.text, label = "chipFg")
            Surface(
                onClick = { onSelect(value) },
                enabled = enabled,
                shape = CircleShape,
                color = bg,
                modifier = Modifier
                    .height(40.dp)
                    .semantics { contentDescription = "$value minutos" }
            ) {
                Box(Modifier.padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
                    Text("$value min", style = MaterialTheme.typography.labelLarge, color = fg)
                }
            }
        }
    }
}

@Composable
private fun WeekMeter(uses: Int) {
    val colors = DnTheme.colors
    val tone = when {
        uses == 0 -> colors.success
        uses <= 2 -> colors.warn
        else -> colors.danger
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Últimos 7 días", style = MaterialTheme.typography.titleSmall, color = colors.text, modifier = Modifier.weight(1f))
        Text(
            when (uses) {
                0 -> "Sin usos"
                1 -> "1 uso"
                else -> "$uses usos"
            },
            style = MaterialTheme.typography.labelLarge,
            color = tone
        )
    }
    Spacer(Modifier.height(10.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(7) { i ->
            Box(
                Modifier
                    .weight(1f)
                    .height(8.dp)
                    .background(if (i < uses) tone else colors.raised, CircleShape)
            )
        }
    }
    Spacer(Modifier.height(8.dp))
    Text(
        when {
            uses == 0 -> "Ninguna emergencia esta semana. Así se descansa."
            uses <= 2 -> "Úsalo solo cuando de verdad lo necesites."
            else -> "Cada uso le quita minutos a tu descanso."
        },
        style = MaterialTheme.typography.bodySmall,
        color = colors.textDim
    )
}
