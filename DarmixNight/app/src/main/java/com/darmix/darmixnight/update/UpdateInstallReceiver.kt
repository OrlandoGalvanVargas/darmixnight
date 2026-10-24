package com.darmix.darmixnight.update

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import androidx.core.content.IntentCompat

class UpdateInstallReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val confirm = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_INTENT, Intent::class.java)
                if (confirm != null) {
                    confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    try {
                        context.startActivity(confirm)
                        UpdateManager.onInstallerOpened()
                    } catch (e: Exception) {
                        UpdateManager.onInstallFailed("No se pudo abrir el instalador de Android.")
                    }
                } else {
                    UpdateManager.onInstallFailed("Android no devolvió el instalador.")
                }
            }
            PackageInstaller.STATUS_SUCCESS -> UpdateManager.onInstalled()
            PackageInstaller.STATUS_FAILURE_ABORTED -> UpdateManager.onInstallCancelled()
            else -> {
                val reason = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
                UpdateManager.onInstallFailed(describe(status, reason))
            }
        }
    }

    private fun describe(status: Int, reason: String?): String = when (status) {
        PackageInstaller.STATUS_FAILURE_CONFLICT ->
            "La nueva versión no coincide con la instalada (firma o versión distinta)."
        PackageInstaller.STATUS_FAILURE_INCOMPATIBLE ->
            "La nueva versión no es compatible con este teléfono."
        PackageInstaller.STATUS_FAILURE_STORAGE ->
            "No hay espacio suficiente para instalar la actualización."
        PackageInstaller.STATUS_FAILURE_INVALID ->
            "El archivo descargado no es válido. Intenta descargarlo otra vez."
        PackageInstaller.STATUS_FAILURE_BLOCKED ->
            "Android bloqueó la instalación."
        else -> reason?.takeIf { it.isNotBlank() } ?: "No se pudo instalar la actualización."
    }
}
