package com.darmix.darmixnight.ui.main

import android.content.Intent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.darmix.darmixnight.BlockPrefs
import com.darmix.darmixnight.EmergencyUnlockManager
import com.darmix.darmixnight.KeepAliveService
import com.darmix.darmixnight.ScheduleUtils
import com.darmix.darmixnight.ui.components.DnGlyph
import com.darmix.darmixnight.ui.components.Glyph
import com.darmix.darmixnight.ui.components.MoonMark
import com.darmix.darmixnight.ui.components.SkyBackdrop
import com.darmix.darmixnight.ui.components.ThemeToggleButton
import com.darmix.darmixnight.ui.theme.DnTheme
import com.darmix.darmixnight.ui.theme.DnType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MainScreen(
    onToggleTheme: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val colors = DnTheme.colors

    val startMinutes by remember(context) { BlockPrefs.startMinutesFlow(context) }
        .collectAsStateWithLifecycle(initialValue = BlockPrefs.DEFAULT_START)
    val endMinutes by remember(context) { BlockPrefs.endMinutesFlow(context) }
        .collectAsStateWithLifecycle(initialValue = BlockPrefs.DEFAULT_END)
    val emergencyMinutes by remember(context) { BlockPrefs.emergencyDurationMinutesFlow(context) }
        .collectAsStateWithLifecycle(initialValue = BlockPrefs.DEFAULT_EMERGENCY_DURATION_MINUTES)
    val weeklyUses by remember(context) { EmergencyUnlockManager.weeklyUsageCountFlow(context) }
        .collectAsStateWithLifecycle(initialValue = 0)
    val paused by remember(context) { BlockPrefs.servicePausedFlow(context) }
        .collectAsStateWithLifecycle(initialValue = false)

    var now by remember { mutableIntStateOf(ScheduleUtils.currentMinutesOfDay()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = ScheduleUtils.currentMinutesOfDay()
            delay(60_000L - (System.currentTimeMillis() % 60_000L) + 50L)
        }
    }

    val blocking = !paused && ScheduleUtils.isWithinBlockRange(now, startMinutes, endMinutes)
    val inBlockWindow = ScheduleUtils.isWithinBlockRange(now, startMinutes, endMinutes)
    val editable = !inBlockWindow
    val emergencyOpenNow = ScheduleUtils.isWithinEmergencyUnlockWindow(now)

    var sheetFor by remember { mutableStateOf<ScheduleEdge?>(null) }
    var askPause by remember { mutableStateOf(false) }

    val saveSchedule: (Int, Int) -> Unit = { s, e ->
        if (s != e) {
            scope.launch {
                if (s != startMinutes) BlockPrefs.setStartMinutes(context, s)
                if (e != endMinutes) BlockPrefs.setEndMinutes(context, e)
            }
        }
    }

    val setPaused: (Boolean) -> Unit = { value ->
        scope.launch {
            BlockPrefs.setServicePaused(context, value)
            ContextCompat.startForegroundService(context, Intent(context, KeepAliveService::class.java))
        }
    }

    val enter = remember { Animatable(0f) }
    LaunchedEffect(Unit) { enter.animateTo(1f, tween(1000, easing = FastOutSlowInEasing)) }

    SkyBackdrop(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MoonMark()
                Spacer(Modifier.width(10.dp))
                Text(
                    "DarmixNight",
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.text,
                    modifier = Modifier.weight(1f)
                )
                ThemeToggleButton(onClick = onToggleTheme)
            }

            Spacer(Modifier.height(18.dp))

            Rise(enter.value, 0) {
                ServiceCard(
                    paused = paused,
                    canToggle = editable,
                    resumesAt = endMinutes,
                    onToggle = { if (paused) setPaused(false) else askPause = true }
                )
            }

            Spacer(Modifier.height(14.dp))

            Rise(enter.value, 1) {
                SkyHero(
                    startMinutes = startMinutes,
                    endMinutes = endMinutes,
                    now = now,
                    blocking = blocking,
                    paused = paused
                )
            }

            Spacer(Modifier.height(26.dp))

            Rise(enter.value, 2) {
                Column {
                    SectionHeader(
                        title = "TU HORARIO",
                        hint = if (editable) "Toca una tarjeta para cambiar la hora" else "Bloqueado hasta las ${TimeText.full(endMinutes)}"
                    )
                    Spacer(Modifier.height(12.dp))
                    ScheduleCards(
                        startMinutes = startMinutes,
                        endMinutes = endMinutes,
                        editable = editable,
                        onPick = { sheetFor = it }
                    )
                }
            }

            Spacer(Modifier.height(26.dp))

            Rise(enter.value, 3) {
                Column {
                    SectionHeader(title = "EMERGENCIA", hint = if (emergencyOpenNow) "Disponible ahora" else "No disponible de noche")
                    Spacer(Modifier.height(12.dp))
                    EmergencyCard(
                        weeklyUses = weeklyUses,
                        durationMinutes = emergencyMinutes,
                        now = now,
                        openNow = emergencyOpenNow,
                        editable = editable,
                        onChangeDuration = { m ->
                            scope.launch { BlockPrefs.setEmergencyDurationMinutes(context, m) }
                        }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Rise(enter.value, 4) { AllowedAppsNote() }

            Spacer(Modifier.height(32.dp))
        }
    }

    val edge = sheetFor
    if (edge != null && editable) {
        TimeWheelSheet(
            handle = edge,
            initialMinutes = if (edge == ScheduleEdge.START) startMinutes else endMinutes,
            otherMinutes = if (edge == ScheduleEdge.START) endMinutes else startMinutes,
            onDismiss = { sheetFor = null },
            onConfirm = { m ->
                if (edge == ScheduleEdge.START) saveSchedule(m, endMinutes) else saveSchedule(startMinutes, m)
                sheetFor = null
            }
        )
    }

    if (askPause) {
        PauseDialog(
            startMinutes = startMinutes,
            onConfirm = {
                askPause = false
                setPaused(true)
            },
            onDismiss = { askPause = false }
        )
    }
}

@Composable
private fun Rise(progress: Float, index: Int, content: @Composable () -> Unit) {
    Box(
        Modifier.graphicsLayer {
            val start = index * 0.08f
            val t = ((progress - start) / 0.5f).coerceIn(0f, 1f)
            alpha = t
            translationY = (1f - t) * 24.dp.toPx()
        }
    ) { content() }
}

@Composable
private fun SectionHeader(title: String, hint: String) {
    val colors = DnTheme.colors
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = DnType.overline, color = colors.textDim, modifier = Modifier.weight(1f))
        Text(hint, style = MaterialTheme.typography.labelMedium, color = colors.accent)
    }
}

@Composable
private fun AllowedAppsNote() {
    val colors = DnTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.raised.copy(alpha = 0.55f), RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Glyph(DnGlyph.PHONE, colors.accentAlt, size = 22.dp)
        Spacer(Modifier.width(12.dp))
        Text(
            "Durante el descanso puedes seguir usando llamadas, mensajes, contactos, cámara y ajustes.",
            style = MaterialTheme.typography.bodySmall,
            color = colors.textDim
        )
    }
}
