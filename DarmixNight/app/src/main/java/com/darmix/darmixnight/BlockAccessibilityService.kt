package com.darmix.darmixnight

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class BlockAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "SleepGuardA11y"
        private const val CHECK_INTERVAL_MS = 2000L

        /**
         * Marca de tiempo del último chequeo exitoso (evento o periódico).
         * Pública para que KeepAliveService pueda saber si el servicio sigue
         * "vivo" de verdad, más allá de que el interruptor de Ajustes diga
         * que está activado.
         */
        @Volatile var lastHeartbeatMillis: Long = 0L
            private set

        fun isHeartbeatFresh(maxAgeMs: Long): Boolean {
            if (lastHeartbeatMillis == 0L) return false
            return System.currentTimeMillis() - lastHeartbeatMillis < maxAgeMs
        }

        private val WHITELIST_PACKAGES = setOf(
            "com.android.dialer",
            "com.google.android.dialer",
            "com.samsung.android.dialer",
            "com.android.contacts",
            "com.google.android.contacts",
            "com.samsung.android.contacts",
            "com.google.android.apps.messaging",
            "com.android.mms",
            "com.samsung.android.messaging",
            "com.android.settings",
            "com.android.camera",
            "com.android.camera2",
            "com.google.android.GoogleCamera",
            "com.samsung.android.camera"
        )

        private val IGNORED_TRANSIENT_PACKAGES = setOf(
            "android",
            "com.android.systemui",
            "com.miui.systemui",
            "com.miui.home",
            "com.mi.android.globallauncher",
            "com.mi.appfinder",
            "com.miui.securitycenter",
            "com.lbe.security.miui",
            "com.android.launcher3",
            "com.google.android.apps.nexuslauncher",
            "com.sec.android.app.launcher",
            "com.huawei.android.launcher"
        )
    }

    private val handler = Handler(Looper.getMainLooper())
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    @Volatile private var cachedStartMinutes = BlockPrefs.DEFAULT_START
    @Volatile private var cachedEndMinutes = BlockPrefs.DEFAULT_END
    @Volatile private var lastForegroundPackage: String? = null
    @Volatile private var isBlocking = false

    private val periodicCheck = object : Runnable {
        override fun run() {
            try {
                evaluate()
            } catch (e: Exception) {
                Log.e(TAG, "Error en chequeo periódico: ${e.message}", e)
            }
            handler.postDelayed(this, CHECK_INTERVAL_MS)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "Servicio de accesibilidad conectado")
        lastHeartbeatMillis = System.currentTimeMillis()

        serviceScope.launch {
            BlockPrefs.startMinutesFlow(applicationContext).collect { cachedStartMinutes = it }
        }
        serviceScope.launch {
            BlockPrefs.endMinutesFlow(applicationContext).collect { cachedEndMinutes = it }
        }

        handler.post(periodicCheck)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        try {
            handleEvent(event)
        } catch (e: Exception) {
            // Nunca dejar que una excepción puntual tumbe el proceso completo:
            // eso mataría de paso a KeepAliveService (mismo proceso) y, en
            // exceso, Android podría desactivar el servicio por considerarlo
            // "mal comportado".
            Log.e(TAG, "Error manejando evento de accesibilidad: ${e.message}", e)
        }
    }

    private fun handleEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val packageName = event.packageName?.toString() ?: return

        if (packageName in IGNORED_TRANSIENT_PACKAGES) return

        // La propia ventana del overlay (añadida con WindowManager, sin ser
        // una Activity) también genera este evento y se reporta con nuestro
        // propio packageName. Si no es realmente MainActivity, es el overlay
        // hablando de sí mismo: lo ignoramos para no auto-desbloquearnos justo
        // después de mostrarlo.
        val className = event.className?.toString()
        if (packageName == applicationContext.packageName &&
            className != MainActivity::class.java.name
        ) {
            return
        }

        lastForegroundPackage = packageName
        evaluate()
    }

    override fun onInterrupt() {
        Log.d(TAG, "Servicio de accesibilidad interrumpido")
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(periodicCheck)
        serviceScope.cancel()
    }

    private fun evaluate() {
        lastHeartbeatMillis = System.currentTimeMillis()

        val pkg = lastForegroundPackage ?: return

        // Si hay un desbloqueo de emergencia activo, no bloquear nada hasta
        // que expire (el propio chequeo periódico se encargará de reanudar
        // el bloqueo automáticamente cuando termine el tiempo).
        if (EmergencyUnlockManager.isBypassActive()) {
            if (isBlocking) {
                OverlayManager.hideOverlay(applicationContext)
                isBlocking = false
            }
            return
        }

        val withinBlockRange = ScheduleUtils.isWithinBlockRange(
            ScheduleUtils.currentMinutesOfDay(),
            cachedStartMinutes,
            cachedEndMinutes
        )

        val shouldBlock = withinBlockRange &&
                pkg != applicationContext.packageName &&
                pkg !in WHITELIST_PACKAGES

        if (shouldBlock) {
            if (!isBlocking) {
                Log.d(TAG, "Bloqueando app: $pkg")
                goHome()
                OverlayManager.showOverlay(applicationContext)
                isBlocking = true
            }
        } else {
            if (isBlocking) {
                Log.d(TAG, "Desbloqueando (pkg permitido o fuera de horario): $pkg")
                OverlayManager.hideOverlay(applicationContext)
                isBlocking = false
            }
        }
    }

    private fun goHome() {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(homeIntent)
    }
}