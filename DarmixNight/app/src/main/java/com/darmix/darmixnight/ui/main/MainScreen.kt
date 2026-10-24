package com.darmix.darmixnight.ui.main

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
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
import com.darmix.darmixnight.update.UpdateRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val TabContentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 120.dp)

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

    val inBlockWindow = ScheduleUtils.isWithinBlockRange(now, startMinutes, endMinutes)
    val editable = !inBlockWindow
    val emergencyOpenNow = ScheduleUtils.isWithinEmergencyUnlockWindow(now)

    var tab by rememberSaveable { mutableStateOf(MainTab.NIGHT) }
    var sheetFor by remember { mutableStateOf<ScheduleEdge?>(null) }
    var askPause by remember { mutableStateOf(false) }

    BackHandler(enabled = tab != MainTab.NIGHT) { tab = MainTab.NIGHT }

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

    SkyBackdrop(modifier = modifier) {
        Box(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
        ) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MoonMark()
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("DarmixNight", style = MaterialTheme.typography.titleSmall, color = colors.text)
                        Text(tab.label, style = MaterialTheme.typography.labelMedium, color = colors.textDim)
                    }
                    ThemeToggleButton(onClick = onToggleTheme)
                }

                AnimatedContent(
                    targetState = tab,
                    transitionSpec = {
                        val direction = if (targetState.ordinal > initialState.ordinal) 1 else -1
                        (slideInHorizontally(tween(380, easing = FastOutSlowInEasing)) { it / 6 * direction } +
                                fadeIn(tween(320, delayMillis = 60))) togetherWith
                                (slideOutHorizontally(tween(320, easing = FastOutSlowInEasing)) { -it / 8 * direction } +
                                        fadeOut(tween(180)))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    label = "mainTabs"
                ) { current ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(TabContentPadding)
                    ) {
                        when (current) {
                            MainTab.NIGHT -> NightTab(
                                startMinutes = startMinutes,
                                endMinutes = endMinutes,
                                paused = paused,
                                editable = editable,
                                onPick = { sheetFor = it }
                            )
                            MainTab.EMERGENCY -> {
                                SectionHeader(
                                    title = "DESBLOQUEO DE EMERGENCIA",
                                    hint = if (emergencyOpenNow) "Disponible ahora" else "No disponible de noche"
                                )
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
                            MainTab.SETTINGS -> {
                                SectionHeader(title = "SERVICIO", hint = if (paused) "En pausa" else "Activo")
                                Spacer(Modifier.height(12.dp))
                                ServiceCard(
                                    paused = paused,
                                    canToggle = editable,
                                    resumesAt = endMinutes,
                                    onToggle = { if (paused) setPaused(false) else askPause = true }
                                )
                                Spacer(Modifier.height(26.dp))
                                SectionHeader(title = "DURANTE EL DESCANSO", hint = "")
                                Spacer(Modifier.height(12.dp))
                                AllowedAppsNote()
                                Spacer(Modifier.height(28.dp))
                                Text(
                                    "DarmixNight ${remember { UpdateRepository.installedVersion(context) }}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = colors.textDim,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }

            MoonDock(
                selected = tab,
                onSelect = { tab = it },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 20.dp, end = 20.dp, bottom = 14.dp)
            )
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
