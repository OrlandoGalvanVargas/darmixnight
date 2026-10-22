package com.darmix.darmixnight

import android.content.Context
import android.os.SystemClock
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

object EmergencyUnlockManager {

    private val USAGE_TIMESTAMPS = stringPreferencesKey("emergency_usage_timestamps")

    private const val SEVEN_DAYS_MS = 7L * 24 * 60 * 60 * 1000
    private const val PRUNE_AFTER_MS = 30L * 24 * 60 * 60 * 1000

    @Volatile private var bypassUntilElapsed: Long = 0L

    fun isBypassActive(): Boolean = SystemClock.elapsedRealtime() < bypassUntilElapsed

    fun activateBypass(durationMinutes: Int) {
        bypassUntilElapsed = SystemClock.elapsedRealtime() + durationMinutes * 60_000L
    }

    fun weeklyUsageCountFlow(context: Context): Flow<Int> {
        return context.dataStore.data.map { prefs ->
            val now = System.currentTimeMillis()
            parseTimestamps(prefs[USAGE_TIMESTAMPS]).count { now - it in 0..SEVEN_DAYS_MS }
        }
    }

    suspend fun weeklyUsageCountOnce(context: Context): Int {
        return weeklyUsageCountFlow(context).first()
    }

    suspend fun recordUsage(context: Context) {
        context.dataStore.edit { prefs ->
            val now = System.currentTimeMillis()
            val existing = parseTimestamps(prefs[USAGE_TIMESTAMPS]).filter { now - it in 0..PRUNE_AFTER_MS }
            prefs[USAGE_TIMESTAMPS] = (existing + now).joinToString(",")
        }
    }

    private fun parseTimestamps(raw: String?): List<Long> =
        (raw ?: "").split(",").mapNotNull { it.trim().toLongOrNull() }
}
