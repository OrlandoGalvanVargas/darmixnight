package com.darmix.darmixnight

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat

class WatchdogReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("DarmixNightWatchdog", "Watchdog disparado: asegurando que KeepAliveService esté activo")
        try {
            ContextCompat.startForegroundService(context, Intent(context, KeepAliveService::class.java))
        } catch (e: Exception) {
            Log.w("DarmixNightWatchdog", "No se pudo iniciar KeepAliveService: ${e.message}")
        }
    }
}
