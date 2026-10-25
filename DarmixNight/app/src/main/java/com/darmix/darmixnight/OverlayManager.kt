package com.darmix.darmixnight

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.darmix.darmixnight.ui.block.BlockScreen
import com.darmix.darmixnight.ui.block.BlockStep
import com.darmix.darmixnight.ui.theme.DarmixNightTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

object OverlayManager {

    private const val TAG = "DarmixNightOverlay"

    const val HOLD_DURATION_MS = 20_000L

    const val REQUIRED_PHRASE = "necesito desbloquear"

    private var overlayView: FrameLayout? = null
    private var composeView: ComposeView? = null
    private var lifecycleOwner: OverlayLifecycleOwner? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    fun canDrawOverlays(context: Context): Boolean = Settings.canDrawOverlays(context)

    fun showOverlay(context: Context) {
        if (overlayView != null) return
        if (!canDrawOverlays(context)) {
            Log.w(TAG, "No hay permiso SYSTEM_ALERT_WINDOW, no se puede mostrar overlay")
            return
        }

        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val appContext = context.applicationContext

        val nowMinutes = ScheduleUtils.currentMinutesOfDay()
        val emergencyAllowed = ScheduleUtils.isWithinEmergencyUnlockWindow(nowMinutes)

        val owner = OverlayLifecycleOwner().also { it.start() }
        val step = mutableStateOf(BlockStep.HOLD)

        val root = BlockFrame(context).apply {
            isFocusableInTouchMode = true
        }

        val compose = ComposeView(context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                val startMinutes by remember { BlockPrefs.startMinutesFlow(appContext) }
                    .collectAsState(initial = BlockPrefs.DEFAULT_START)
                val endMinutes by remember { BlockPrefs.endMinutesFlow(appContext) }
                    .collectAsState(initial = BlockPrefs.DEFAULT_END)
                DarmixNightTheme(darkTheme = true) {
                    BlockScreen(
                        emergencyAllowed = emergencyAllowed,
                        holdDurationMs = HOLD_DURATION_MS,
                        requiredPhrase = REQUIRED_PHRASE,
                        startMinutes = startMinutes,
                        endMinutes = endMinutes,
                        loadWeeklyUses = { EmergencyUnlockManager.weeklyUsageCountOnce(appContext) },
                        step = step.value,
                        onStepChange = { step.value = it },
                        onConfirmPhrase = { confirmUnlock(appContext) }
                    )
                }
            }
        }

        root.setViewTreeLifecycleOwner(owner)
        root.setViewTreeSavedStateRegistryOwner(owner)
        root.addView(
            compose,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        )

        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            @Suppress("DEPRECATION")
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        }

        try {
            windowManager.addView(root, params)
            overlayView = root
            composeView = compose
            lifecycleOwner = owner
            Log.d(TAG, "Overlay mostrado (emergencia disponible: $emergencyAllowed)")
        } catch (e: Exception) {
            owner.stop()
            Log.e(TAG, "Error mostrando overlay: ${e.message}")
        }
    }

    fun hideOverlay(context: Context) {
        val view = overlayView ?: return
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        try {
            composeView?.disposeComposition()
            windowManager.removeView(view)
        } catch (e: Exception) {
            Log.e(TAG, "Error ocultando overlay: ${e.message}")
        } finally {
            lifecycleOwner?.stop()
            overlayView = null
            composeView = null
            lifecycleOwner = null
        }
    }

    private fun confirmUnlock(appContext: Context) {
        scope.launch {
            val duration = BlockPrefs.emergencyDurationMinutesFlow(appContext).first()
            EmergencyUnlockManager.activateBypass(duration)
            EmergencyUnlockManager.recordUsage(appContext)
            hideOverlay(appContext)
        }
    }

    private class BlockFrame(context: Context) : FrameLayout(context) {

        override fun dispatchKeyEvent(event: KeyEvent): Boolean {
            if (event.keyCode == KeyEvent.KEYCODE_BACK) {
                if (event.action == KeyEvent.ACTION_UP) hideKeyboard()
                return true
            }
            return super.dispatchKeyEvent(event)
        }

        private fun hideKeyboard() {
            val imm = context.getSystemService(InputMethodManager::class.java) ?: return
            imm.hideSoftInputFromWindow(windowToken, 0)
        }
    }

    private class OverlayLifecycleOwner : LifecycleOwner, SavedStateRegistryOwner {
        private val registry = LifecycleRegistry(this)
        private val savedState = SavedStateRegistryController.create(this)

        override val lifecycle: Lifecycle
            get() = registry

        override val savedStateRegistry: SavedStateRegistry
            get() = savedState.savedStateRegistry

        fun start() {
            savedState.performAttach()
            savedState.performRestore(null)
            registry.currentState = Lifecycle.State.RESUMED
        }

        fun stop() {
            if (registry.currentState != Lifecycle.State.INITIALIZED) {
                registry.currentState = Lifecycle.State.DESTROYED
            }
        }
    }
}
