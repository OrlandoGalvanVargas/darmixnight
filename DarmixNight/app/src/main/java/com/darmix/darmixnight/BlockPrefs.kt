package com.darmix.darmixnight

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

internal val Context.dataStore by preferencesDataStore(name = "sleepguard_prefs")

object BlockPrefs {

    private val START_MINUTES = intPreferencesKey("start_minutes")
    private val END_MINUTES = intPreferencesKey("end_minutes")

    const val DEFAULT_START = 23 * 60 + 30
    const val DEFAULT_END = 5 * 60

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

    fun getStartMinutesBlocking(context: Context): Int = runBlocking {
        startMinutesFlow(context).first()
    }

    fun getEndMinutesBlocking(context: Context): Int = runBlocking {
        endMinutesFlow(context).first()
    }

    fun emergencyDurationMinutesFlow(context: Context): Flow<Int> =
        context.dataStore.data.map { it[EMERGENCY_DURATION_MINUTES] ?: DEFAULT_EMERGENCY_DURATION_MINUTES }

    suspend fun setEmergencyDurationMinutes(context: Context, minutes: Int) {
        context.dataStore.edit { it[EMERGENCY_DURATION_MINUTES] = minutes }
    }

    fun getEmergencyDurationMinutesBlocking(context: Context): Int = runBlocking {
        emergencyDurationMinutesFlow(context).first()
    }

    private val SERVICE_PAUSED = booleanPreferencesKey("service_paused")

    fun servicePausedFlow(context: Context): Flow<Boolean> =
        context.dataStore.data.map { it[SERVICE_PAUSED] ?: false }

    suspend fun setServicePaused(context: Context, paused: Boolean) {
        context.dataStore.edit { it[SERVICE_PAUSED] = paused }
    }
}
