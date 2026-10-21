package com.darmix.darmixnight

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat

/**
 * Arranca KeepAliveService automáticamente después de reiniciar el teléfono.
 * El AccessibilityService NO necesita esto: si ya está activado en Ajustes,
 * el sistema lo vuelve a conectar solo tras un reinicio.
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SleepGuardBoot"

        // Algunos fabricantes (Xiaomi/HTC) usan una acción propia además de,
        // o en lugar de, la estándar de Android.
        private const val ACTION_QUICKBOOT_POWERON = "android.intent.action.QUICKBOOT_POWERON"
        private const val ACTION_HTC_QUICKBOOT_POWERON = "com.htc.intent.action.QUICKBOOT_POWERON"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            ACTION_QUICKBOOT_POWERON,
            ACTION_HTC_QUICKBOOT_POWERON -> {
                Log.d(TAG, "Dispositivo reiniciado, arrancando KeepAliveService")
                val serviceIntent = Intent(context, KeepAliveService::class.java)
                ContextCompat.startForegroundService(context, serviceIntent)
            }
        }
    }
}