package com.darmix.darmixnight

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class KeepAliveService : Service() {

    companion object {
        private const val TAG = "SleepGuardKeepAlive"
        private const val CHANNEL_ID = "sleepguard_keepalive"
        private const val ALERT_CHANNEL_ID = "sleepguard_alerts"
        private const val NOTIFICATION_ID = 1001
        private const val ALERT_NOTIFICATION_ID = 1002
        private const val WATCHDOG_INTERVAL_MS = 15 * 60 * 1000L // 15 minutos
        private const val WATCHDOG_REQUEST_CODE = 2001
    }

    // Solo para la lectura puntual del horario al refrescar la notificación;
    // no hay ningún bucle ni polling propio aquí.
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        // Arranca ya mismo con un texto genérico; en cuanto la lectura del
        // horario (async) termine, refreshScheduleNotification() la
        // reemplaza. startForeground no puede esperar a esa lectura.
        val notification = buildNotification("Protegiendo tu horario de sueño")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            @Suppress("DEPRECATION")
            startForeground(NOTIFICATION_ID, notification)
        }

        refreshScheduleNotification()
        Log.d(TAG, "KeepAliveService creado")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        scheduleWatchdog()
        checkCriticalPermissionsAndAlert()
        // Aprovecha este mismo despertar (cada ~15 min por el watchdog, o al
        // reiniciar) para refrescar el horario mostrado; no agrega ninguna
        // alarma ni ciclo nuevo.
        refreshScheduleNotification()
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Log.d(TAG, "Tarea eliminada de recientes; programando reinicio de seguridad")
        // Red de seguridad: en fabricantes agresivos (MIUI/HyperOS) deslizar
        // la app en "Recientes" a veces mata el proceso incluso siendo un
        // foreground service. Reprogramamos un reinicio casi inmediato.
        scheduleRestart(delayMs = 1000L)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        Log.d(TAG, "KeepAliveService destruido")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "SleepGuard en segundo plano",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Mantiene activo el bloqueo nocturno"
            setShowBadge(false)
        }
        val alertChannel = NotificationChannel(
            ALERT_CHANNEL_ID,
            "SleepGuard - Alertas de permisos",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Avisa si un permiso necesario fue revocado y el bloqueo dejó de funcionar"
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
        manager.createNotificationChannel(alertChannel)
    }

    /**
     * Abre la app al tocar la notificación. Es solo una comodidad de
     * navegación: no cambia nada de cuándo o cómo se activa el servicio.
     */
    private fun openAppPendingIntent(requestCode: Int): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun buildNotification(contentText: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SleepGuard activo")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_stat_sleepguard)
            .setColor(ContextCompat.getColor(this, R.color.dn_notification_accent))
            .setContentIntent(openAppPendingIntent(NOTIFICATION_ID))
            .setOngoing(true) // impide deslizar para quitarla
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()
    }

    /**
     * Lee el horario guardado (una sola vez, no un stream infinito) y
     * actualiza el texto de la notificación persistente con la hora de
     * inicio y fin, indicando además si el bloqueo está activo ahora mismo.
     * Solo se llama desde onCreate() y onStartCommand(), es decir, en los
     * mismos momentos en que el servicio ya se despierta por su cuenta.
     */
    private fun refreshScheduleNotification() {
        serviceScope.launch {
            try {
                val start = BlockPrefs.startMinutesFlow(applicationContext).first()
                val end = BlockPrefs.endMinutesFlow(applicationContext).first()
                val withinRange = ScheduleUtils.isWithinBlockRange(
                    ScheduleUtils.currentMinutesOfDay(), start, end
                )
                val text = if (withinRange) {
                    "Bloqueando ahora · hasta las ${formatTime12(end)}"
                } else {
                    "Horario: ${formatTime12(start)} - ${formatTime12(end)}"
                }
                getSystemService(NotificationManager::class.java)
                    .notify(NOTIFICATION_ID, buildNotification(text))
            } catch (e: Exception) {
                Log.e(TAG, "No se pudo actualizar el horario en la notificación: ${e.message}")
            }
        }
    }

    private fun formatTime12(minutes: Int): String {
        val h24 = (minutes / 60) % 24
        val h12 = if (h24 % 12 == 0) 12 else h24 % 12
        val suffix = if (h24 < 12) "AM" else "PM"
        return "%d:%02d %s".format(h12, minutes % 60, suffix)
    }

    private fun scheduleWatchdog() {
        scheduleRestart(delayMs = WATCHDOG_INTERVAL_MS)
    }

    private fun scheduleRestart(delayMs: Long) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = watchdogPendingIntent()
        val triggerAt = SystemClock.elapsedRealtime() + delayMs

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                !alarmManager.canScheduleExactAlarms()
            ) {
                // Sin permiso de alarmas exactas: usamos una aproximada como respaldo.
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pendingIntent
                )
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pendingIntent
                )
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "No se pudo programar el watchdog: ${e.message}")
        }
    }

    private fun watchdogPendingIntent(): PendingIntent {
        val intent = Intent(this, WatchdogReceiver::class.java)
        return PendingIntent.getBroadcast(
            this,
            WATCHDOG_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * Revisa los permisos críticos para que el bloqueo funcione de verdad
     * (accesibilidad, overlay, batería). Si falta alguno, muestra una
     * notificación de alta prioridad avisando al usuario. Si todo está bien,
     * quita la alerta si existía. Se llama cada vez que el watchdog dispara
     * (cada ~15 minutos) para detectar permisos revocados solos por el
     * sistema (frecuente en HyperOS tras actualizaciones).
     */
    private fun checkCriticalPermissionsAndAlert() {
        val status = PermissionUtils.checkAll(applicationContext)
        val manager = getSystemService(NotificationManager::class.java)

        val missing = mutableListOf<String>()
        if (!status.accessibilityEnabled) {
            missing.add("Servicio de accesibilidad")
        } else if (!BlockAccessibilityService.isHeartbeatFresh(maxAgeMs = 5 * 60 * 1000L)) {
            // El interruptor dice "activado" pero no ha dado señales de vida
            // en 5 minutos: probablemente esté congelado/con un ANR.
            missing.add("Servicio de accesibilidad (no responde, desactívalo y vuelve a activarlo)")
        }
        if (!status.overlayGranted) missing.add("Mostrar sobre otras apps")
        if (!status.batteryExempted) missing.add("Sin restricciones de batería")

        if (missing.isEmpty()) {
            manager.cancel(ALERT_NOTIFICATION_ID)
            return
        }

        Log.w(TAG, "Permisos críticos faltantes: $missing")

        val alertNotification = NotificationCompat.Builder(this, ALERT_CHANNEL_ID)
            .setContentTitle("SleepGuard no está protegiendo tu horario")
            .setContentText("Falta: ${missing.joinToString(", ")}. Toca para arreglarlo.")
            .setStyle(NotificationCompat.BigTextStyle().bigText(
                "El bloqueo nocturno no funcionará hasta que concedas: " +
                        "${missing.joinToString(", ")}. Toca para abrir la app."
            ))
            .setSmallIcon(R.drawable.ic_stat_sleepguard)
            .setColor(ContextCompat.getColor(this, R.color.dn_night_danger))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(Notification.CATEGORY_ERROR)
            .setContentIntent(openAppPendingIntent(ALERT_NOTIFICATION_ID))
            .setAutoCancel(false)
            .build()

        manager.notify(ALERT_NOTIFICATION_ID, alertNotification)
    }
}