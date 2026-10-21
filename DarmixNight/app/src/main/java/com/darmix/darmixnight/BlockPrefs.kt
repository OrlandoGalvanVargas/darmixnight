package com.darmix.darmixnight

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

// IMPORTANTE: no debe ser "private". Otros archivos (EmergencyUnlockManager)
// necesitan usar esta MISMA instancia de DataStore. Declarar una segunda
// instancia apuntando al mismo nombre de archivo desde otro sitio provoca
// un crash en tiempo de ejecución ("multiple DataStores active").
internal val Context.dataStore by preferencesDataStore(name = "sleepguard_prefs")

/**
 * Guarda y expone el horario de bloqueo como minutos desde medianoche (0..1439).
 * Ej: 23:30 -> 1410, 05:00 -> 300.
 */
object BlockPrefs {

    private val START_MINUTES = intPreferencesKey("start_minutes")
    private val END_MINUTES = intPreferencesKey("end_minutes")

    // Valores por defecto: 23:30 y 05:00
    const val DEFAULT_START = 23 * 60 + 30 // 1410
    const val DEFAULT_END = 5 * 60         // 300

    private val EMERGENCY_DURATION_MINUTES = intPreferencesKey("emergency_duration_minutes")
    const val DEFAULT_EMERGENCY_DURATION_MINUTES = 10

    fun startMinutesFlow(context: Context): Flow<Int> =
        context.dataStore.data.map { it[START_MINUTES] ?: DEFAULT_START }

    fun endMinutesFlow(context: Context): Flow<Int> =
        context.dataStore.data.map { it[END_MINUTES] ?: DEFAULT_END }

    suspend fun setStartMinutes(context: Context, minutes: Int) {
        context.dataStore.edit { it[START_MINUTES] = minutes }
    }

    suspend fun setEndMinutes(context: Context, minutes: Int) {
        context.dataStore.edit { it[END_MINUTES] = minutes }
    }

    /**
     * Lecturas síncronas/bloqueantes, para usarlas desde el AccessibilityService
     * o el ForegroundService, donde no siempre es cómodo manejar corrutinas.
     * OJO: nunca llamar esto desde el hilo principal (Main thread) de la UI.
     */
    fun getStartMinutesBlocking(context: Context): Int = runBlocking {
        startMinutesFlow(context).first()
    }

    fun getEndMinutesBlocking(context: Context): Int = runBlocking {
        endMinutesFlow(context).first()
    }

    // --- Duración del desbloqueo de emergencia (en minutos) ---

    fun emergencyDurationMinutesFlow(context: Context): Flow<Int> =
        context.dataStore.data.map { it[EMERGENCY_DURATION_MINUTES] ?: DEFAULT_EMERGENCY_DURATION_MINUTES }

    suspend fun setEmergencyDurationMinutes(context: Context, minutes: Int) {
        context.dataStore.edit { it[EMERGENCY_DURATION_MINUTES] = minutes }
    }

    fun getEmergencyDurationMinutesBlocking(context: Context): Int = runBlocking {
        emergencyDurationMinutesFlow(context).first()
    }
}