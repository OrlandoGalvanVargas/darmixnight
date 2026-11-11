package com.darmix.darmixnight.ui.main

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.darmix.darmixnight.BlockPrefs
import com.darmix.darmixnight.KeepAliveService
import com.darmix.darmixnight.ScheduleUtils
import com.darmix.darmixnight.ui.main.emergency.EmergencyScene
import com.darmix.darmixnight.ui.main.emergency.EmergencyTab
import com.darmix.darmixnight.ui.main.night.NightScene
import com.darmix.darmixnight.ui.main.night.NightTab
import com.darmix.darmixnight.ui.main.night.nightModeOf
import com.darmix.darmixnight.ui.main.settings.PauseDialog
import com.darmix.darmixnight.ui.main.settings.SettingsScene
import com.darmix.darmixnight.ui.main.settings.SettingsTab
import com.darmix.darmixnight.ui.components.SkyBackdrop
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MainScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val startMinutes by remember(context) { BlockPrefs.startMinutesFlow(context) }
        .collectAsStateWithLifecycle(initialValue = BlockPrefs.DEFAULT_START)
    val endMinutes by remember(context) { BlockPrefs.endMinutesFlow(context) }
        .collectAsStateWithLifecycle(initialValue = BlockPrefs.DEFAULT_END)
    val emergencyMinutes by remember(context) { BlockPrefs.emergencyDurationMinutesFlow(context) }
        .collectAsStateWithLifecycle(initialValue = BlockPrefs.DEFAULT_EMERGENCY_DURATION_MINUTES)
    val paused by remember(context) { BlockPrefs.servicePausedFlow(context) }
        .collectAsStateWithLifecycle(initialValue = false)
    val dialSound by remember(context) { BlockPrefs.dialSoundFlow(context) }
        .collectAsStateWithLifecycle(initialValue = true)
    val dialVibration by remember(context) { BlockPrefs.dialVibrationFlow(context) }
        .collectAsStateWithLifecycle(initialValue = true)

    var now by remember { mutableIntStateOf(ScheduleUtils.currentMinutesOfDay()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = ScheduleUtils.currentMinutesOfDay()
            delay(60_000L - (System.currentTimeMillis() % 60_000L) + 50L)
        }
    }

    val inBlockWindow = ScheduleUtils.isWithinBlockRange(now, startMinutes, endMinutes)
    val editable = !inBlockWindow

    var tab by rememberSaveable { mutableStateOf(MainTab.NIGHT) }
    val nightMode = nightModeOf(now, startMinutes, endMinutes, paused)
    val nightAlpha by animateFloatAsState(if (tab == MainTab.NIGHT) 1f else 0f, tween(450), label = "nightScene")
    val emergencyAlpha by animateFloatAsState(if (tab == MainTab.EMERGENCY) 1f else 0f, tween(450), label = "emergencyScene")
    val settingsAlpha by animateFloatAsState(if (tab == MainTab.SETTINGS) 1f else 0f, tween(450), label = "settingsScene")
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
        if (nightAlpha > 0f) {
            NightScene(
                mode = nightMode,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = nightAlpha }
            )
        }
        if (emergencyAlpha > 0f) {
            EmergencyScene(
                mode = nightMode,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = emergencyAlpha }
            )
        }
        if (settingsAlpha > 0f) {
            SettingsScene(
                mode = nightMode,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = settingsAlpha }
            )
        }
        Column(Modifier.fillMaxSize()) {
            MainHeader(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(start = 24.dp, end = 24.dp, top = 8.dp)
            )

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
                when (current) {
                    MainTab.NIGHT -> NightTab(
                        startMinutes = startMinutes,
                        endMinutes = endMinutes,
                        paused = paused,
                        editable = editable,
                        onPick = { sheetFor = it },
                        modifier = Modifier.fillMaxSize()
                    )
                    MainTab.EMERGENCY -> EmergencyTab(
                        durationMinutes = emergencyMinutes,
                        mode = nightMode,
                        tickSound = dialSound,
                        tickHaptics = dialVibration,
                        onChangeDuration = { m ->
                            scope.launch { BlockPrefs.setEmergencyDurationMinutes(context, m) }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                    MainTab.SETTINGS -> SettingsTab(
                        paused = paused,
                        canToggle = editable,
                        resumesAt = endMinutes,
                        soundOn = dialSound,
                        vibrationOn = dialVibration,
                        onToggle = { if (paused) setPaused(false) else askPause = true },
                        onSound = { value -> scope.launch { BlockPrefs.setDialSound(context, value) } },
                        onVibration = { value -> scope.launch { BlockPrefs.setDialVibration(context, value) } },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            NightBar(
                selected = tab,
                onSelect = { tab = it }
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
