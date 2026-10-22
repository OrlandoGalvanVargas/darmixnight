package com.darmix.darmixnight

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat

class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "DarmixNightBoot"
        private const val ACTION_QUICKBOOT_POWERON = "android.intent.action.QUICKBOOT_POWERON"
        private const val ACTION_HTC_QUICKBOOT_POWERON = "com.htc.intent.action.QUICKBOOT_POWERON"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            ACTION_QUICKBOOT_POWERON,
            ACTION_HTC_QUICKBOOT_POWERON -> {
                Log.d(TAG, "Arrancando KeepAliveService por ${intent.action}")
                try {
                    ContextCompat.startForegroundService(context, Intent(context, KeepAliveService::class.java))
                } catch (e: Exception) {
                    Log.w(TAG, "No se pudo iniciar KeepAliveService: ${e.message}")
                }
            }
        }
    }
}
