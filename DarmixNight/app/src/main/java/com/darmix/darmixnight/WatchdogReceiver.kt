package com.darmix.darmixnight

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat

/**
 * Se dispara cada 15 minutos (o casi inmediatamente si el proceso fue
 * eliminado de "Recientes") para asegurarse de que KeepAliveService siga
 * corriendo. Si ya está activo, volver a iniciarlo es inofensivo: Android
 * simplemente vuelve a llamar a onStartCommand().
 */
class WatchdogReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("SleepGuardWatchdog", "Watchdog disparado: asegurando que KeepAliveService esté activo")
        val serviceIntent = Intent(context, KeepAliveService::class.java)
        ContextCompat.startForegroundService(context, serviceIntent)
    }
}