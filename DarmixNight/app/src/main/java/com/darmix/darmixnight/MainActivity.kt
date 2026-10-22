package com.darmix.darmixnight

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.lifecycleScope
import com.darmix.darmixnight.ui.main.MainScreen
import com.darmix.darmixnight.ui.permissions.PermissionGate
import com.darmix.darmixnight.ui.splash.NightIntro
import com.darmix.darmixnight.ui.theme.DarmixNightTheme
import com.darmix.darmixnight.ui.theme.DnTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var themeReady by mutableStateOf(false)
    private var storedDark by mutableStateOf<Boolean?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        splash.setKeepOnScreenCondition { !themeReady }
        splash.setOnExitAnimationListener { provider ->
            provider.view.animate()
                .alpha(0f)
                .setDuration(220L)
                .withEndAction { provider.remove() }
                .start()
        }

        ContextCompat.startForegroundService(this, Intent(this, KeepAliveService::class.java))

        lifecycleScope.launch {
            ThemePrefs.darkModeFlow(applicationContext).collect {
                storedDark = it
                themeReady = true
            }
        }

        val playIntro = savedInstanceState == null

        setContent {
            val systemDark = isSystemInDarkTheme()
            val darkTheme = storedDark ?: systemDark
            var introVisible by rememberSaveable { mutableStateOf(playIntro) }

            val view = LocalView.current
            val lightBars = !darkTheme && !introVisible
            SideEffect {
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = lightBars
                    isAppearanceLightNavigationBars = lightBars
                }
            }

            DarmixNightTheme(darkTheme = darkTheme) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(DnTheme.colors.bg)
                ) {
                    AppRoot(
                        onToggleTheme = {
                            lifecycleScope.launch {
                                ThemePrefs.setDarkMode(applicationContext, !darkTheme)
                            }
                        }
                    )
                    if (introVisible) {
                        NightIntro(
                            start = themeReady,
                            onFinished = { introVisible = false }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppRoot(onToggleTheme: () -> Unit) {
    val context = LocalContext.current
    var status by remember { mutableStateOf(PermissionUtils.checkAll(context)) }
    val refresh = { status = PermissionUtils.checkAll(context) }

    LifecycleResumeEffect(Unit) {
        refresh()
        onPauseOrDispose { }
    }

    var showMain by rememberSaveable { mutableStateOf(status.readyForMain) }
    var revoked by remember { mutableStateOf(SetupPrefs.isSetupCompleted(context)) }

    LaunchedEffect(status.readyForMain) {
        if (status.readyForMain) {
            SetupPrefs.markSetupCompleted(context)
            if (!showMain) {
                delay(1200)
                showMain = true
            }
        } else {
            revoked = SetupPrefs.isSetupCompleted(context)
            showMain = false
        }
    }

    AnimatedContent(
        targetState = showMain,
        transitionSpec = {
            (fadeIn(tween(520, delayMillis = 120)) +
                    scaleIn(tween(620, easing = FastOutSlowInEasing), initialScale = 0.96f))
                .togetherWith(
                    fadeOut(tween(320)) +
                            scaleOut(tween(420, easing = FastOutSlowInEasing), targetScale = 1.04f)
                )
        },
        label = "gate"
    ) { main ->
        if (main) {
            MainScreen(onToggleTheme = onToggleTheme)
        } else {
            PermissionGate(
                status = status,
                revoked = revoked,
                onRefresh = refresh,
                onToggleTheme = onToggleTheme
            )
        }
    }
}
