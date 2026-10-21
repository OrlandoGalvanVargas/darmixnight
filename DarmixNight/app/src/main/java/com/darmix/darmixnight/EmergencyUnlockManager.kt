package com.darmix.darmixnight

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Maneja el desbloqueo de emergencia:
 * - Estado del "bypass" temporal (en memoria; si el proceso muere se pierde
 *   y el bloqueo se reanuda automáticamente al reiniciar -> falla seguro).
 * - Historial persistente de usos, para calcular cuántas veces se ha usado
 *   en los últimos 7 días.
 */
object EmergencyUnlockManager {

    private val USAGE_TIMESTAMPS = stringPreferencesKey("emergency_usage_timestamps")

    private const val SEVEN_DAYS_MS = 7L * 24 * 60 * 60 * 1000
    private const val PRUNE_AFTER_MS = 30L * 24 * 60 * 60 * 1000 // no acumular para siempre

    @Volatile private var bypassUntilMillis: Long = 0L

    fun isBypassActive(): Boolean = System.currentTimeMillis() < bypassUntilMillis

    fun activateBypass(durationMinutes: Int) {
        bypassUntilMillis = System.currentTimeMillis() + durationMinutes * 60_000L
    }

    /** Cuántos usos hay registrados en los últimos 7 días. */
    fun weeklyUsageCountFlow(context: Context): Flow<Int> {
        return context.dataStore.data.map { prefs ->
            val now = System.currentTimeMillis()
            (prefs[USAGE_TIMESTAMPS] ?: "")
                .split(",")
                .mapNotNull { it.trim().toLongOrNull() }
                .count { now - it <= SEVEN_DAYS_MS }
        }
    }

    suspend fun weeklyUsageCountOnce(context: Context): Int {
        return weeklyUsageCountFlow(context).first()
    }

    /** Registra un uso nuevo (llamar justo cuando se confirma el desbloqueo). */
    suspend fun recordUsage(context: Context) {
        context.dataStore.edit { prefs ->
            val now = System.currentTimeMillis()
            val existing = (prefs[USAGE_TIMESTAMPS] ?: "")
                .split(",")
                .mapNotNull { it.trim().toLongOrNull() }
                .filter { now - it <= PRUNE_AFTER_MS }
            prefs[USAGE_TIMESTAMPS] = (existing + now).joinToString(",")
        }
    }
}