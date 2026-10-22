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
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class KeepAliveService : Service() {

    companion object {
        private const val TAG = "DarmixNightKeepAlive"
        private const val CHANNEL_ID = "sleepguard_keepalive"
        private const val ALERT_CHANNEL_ID = "sleepguard_alerts"
        private const val NOTIFICATION_ID = 1001
        private const val ALERT_NOTIFICATION_ID = 1002
        private const val WATCHDOG_INTERVAL_MS = 15 * 60 * 1000L
        private const val WATCHDOG_REQUEST_CODE = 2001

        @Volatile var isRunning: Boolean = false
            private set

        fun ensureRunning(context: Context) {
            if (isRunning) return
            try {
                ContextCompat.startForegroundService(
                    context.applicationContext,
                    Intent(context.applicationContext, KeepAliveService::class.java)
                )
            } catch (e: Exception) {
                Log.w(TAG, "No se pudo iniciar KeepAliveService: ${e.message}")
            }
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var scheduleObserver: Job? = null

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        createNotificationChannel()
        val notification = buildNotification(
            NotiState.ACTIVE,
            BlockPrefs.DEFAULT_START,
            BlockPrefs.DEFAULT_END
        )

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
        observeSchedule()
        Log.d(TAG, "KeepAliveService creado")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        scheduleWatchdog()
        checkCriticalPermissionsAndAlert()
        refreshScheduleNotification()
        observeSchedule()
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Log.d(TAG, "Tarea eliminada de recientes; programando reinicio de seguridad")
        scheduleRestart(delayMs = 1000L)
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        serviceScope.cancel()
        Log.d(TAG, "KeepAliveService destruido")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Estado de DarmixNight",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Aviso permanente que mantiene activo el bloqueo nocturno"
            setShowBadge(false)
        }
        val alertChannel = NotificationChannel(
            ALERT_CHANNEL_ID,
            "Alertas de permisos",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Avisa si un permiso necesario fue revocado y el bloqueo dejó de funcionar"
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
        manager.createNotificationChannel(alertChannel)
    }

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

    private enum class NotiState { ACTIVE, BLOCKING, PAUSED }

    private fun buildNotification(state: NotiState, start: Int, end: Int): Notification {
        val (title, text, detail, sub) = when (state) {
            NotiState.ACTIVE -> NotiContent(
                "Protección activa",
                "Tu descanso empieza a las ${formatTime12(start)}",
                "Descanso de ${formatTime12(start)} a ${formatTime12(end)}. A esa hora el teléfono se bloqueará solo, aunque cierres la app.",
                "Esperando la noche"
            )
            NotiState.BLOCKING -> NotiContent(
                "Modo descanso",
                "Bloqueado hasta las ${formatTime12(end)}",
                "Llamadas, mensajes y cámara siguen disponibles. Deja el teléfono y vuelve a dormir; se desbloquea a las ${formatTime12(end)}.",
                "Descansando"
            )
            NotiState.PAUSED -> NotiContent(
                "DarmixNight en pausa",
                "El bloqueo no se aplicará hasta que lo reactives",
                "Mientras esté en pausa el teléfono no se bloqueará, ni siquiera a las ${formatTime12(start)}. Abre DarmixNight para reactivarlo.",
                "En pausa"
            )
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSubText(sub)
            .setStyle(NotificationCompat.BigTextStyle().bigText(detail))
            .setSmallIcon(R.drawable.ic_stat_darmixnight)
            .setColor(ContextCompat.getColor(this, R.color.dn_notification_accent))
            .setContentIntent(openAppPendingIntent(NOTIFICATION_ID))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()
    }

    private data class NotiContent(val title: String, val text: String, val detail: String, val sub: String)

    private fun observeSchedule() {
        if (scheduleObserver?.isActive == true) return
        scheduleObserver = serviceScope.launch {
            combine(
                BlockPrefs.servicePausedFlow(applicationContext),
                BlockPrefs.startMinutesFlow(applicationContext),
                BlockPrefs.endMinutesFlow(applicationContext)
            ) { paused, start, end -> Triple(paused, start, end) }
                .distinctUntilChanged()
                .collectLatest { (paused, start, end) ->
                    while (true) {
                        postScheduleNotification(paused, start, end)
                        if (paused) break
                        delay(millisUntilNextBoundary(start, end))
                    }
                }
        }
    }

    private fun millisUntilNextBoundary(start: Int, end: Int): Long {
        val now = ScheduleUtils.currentMinutesOfDay()
        val toStart = ((start - now) % 1440 + 1440) % 1440
        val toEnd = ((end - now) % 1440 + 1440) % 1440
        val minutes = listOf(toStart, toEnd).filter { it > 0 }.minOrNull() ?: 1440
        val intoCurrentMinute = System.currentTimeMillis() % 60_000L
        return (minutes * 60_000L - intoCurrentMinute + 500L).coerceAtLeast(1_000L)
    }

    private fun postScheduleNotification(paused: Boolean, start: Int, end: Int) {
        try {
            val withinRange = ScheduleUtils.isWithinBlockRange(ScheduleUtils.currentMinutesOfDay(), start, end)
            val state = when {
                paused -> NotiState.PAUSED
                withinRange -> NotiState.BLOCKING
                else -> NotiState.ACTIVE
            }
            getSystemService(NotificationManager::class.java)
                .notify(NOTIFICATION_ID, buildNotification(state, start, end))
        } catch (e: Exception) {
            Log.e(TAG, "No se pudo actualizar la notificación: ${e.message}")
        }
    }

    private fun refreshScheduleNotification() {
        serviceScope.launch {
            try {
                val paused = BlockPrefs.servicePausedFlow(applicationContext).first()
                val start = BlockPrefs.startMinutesFlow(applicationContext).first()
                val end = BlockPrefs.endMinutesFlow(applicationContext).first()
                val withinRange = ScheduleUtils.isWithinBlockRange(
                    ScheduleUtils.currentMinutesOfDay(), start, end
                )
                val state = when {
                    paused -> NotiState.PAUSED
                    withinRange -> NotiState.BLOCKING
                    else -> NotiState.ACTIVE
                }
                getSystemService(NotificationManager::class.java)
                    .notify(NOTIFICATION_ID, buildNotification(state, start, end))
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

    private fun checkCriticalPermissionsAndAlert() {
        val status = PermissionUtils.checkAll(applicationContext)
        val manager = getSystemService(NotificationManager::class.java)

        val missing = mutableListOf<String>()
        if (!status.accessibilityEnabled) {
            missing.add("Servicio de accesibilidad")
        } else if (!BlockAccessibilityService.isHeartbeatFresh(maxAgeMs = 5 * 60 * 1000L)) {
            missing.add("Servicio de accesibilidad (no responde, desactívalo y vuelve a activarlo)")
        }
        if (!status.overlayGranted) missing.add("Mostrar sobre otras apps")
        if (!status.batteryExempted) missing.add("Sin restricciones de batería")

        if (missing.isEmpty()) {
            manager.cancel(ALERT_NOTIFICATION_ID)
            return
        }

        Log.w(TAG, "Permisos críticos faltantes: $missing")

        val missingText = missing.joinToString(", ")
        val alertNotification = NotificationCompat.Builder(this, ALERT_CHANNEL_ID)
            .setContentTitle("Tus noches están desprotegidas")
            .setContentText("Falta: $missingText")
            .setSubText("Requiere atención")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "El bloqueo nocturno no funcionará hasta que lo arregles. Falta: $missingText."
                )
            )
            .setSmallIcon(R.drawable.ic_stat_darmixnight)
            .setColor(ContextCompat.getColor(this, R.color.dn_day_danger))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(Notification.CATEGORY_ERROR)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .setContentIntent(openAppPendingIntent(ALERT_NOTIFICATION_ID))
            .addAction(0, "Arreglar ahora", openAppPendingIntent(ALERT_NOTIFICATION_ID + 1))
            .setAutoCancel(false)
            .build()

        manager.notify(ALERT_NOTIFICATION_ID, alertNotification)
    }
}