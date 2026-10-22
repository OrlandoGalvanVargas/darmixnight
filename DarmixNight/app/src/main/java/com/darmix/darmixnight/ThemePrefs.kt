package com.darmix.darmixnight

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.themeDataStore by preferencesDataStore(name = "sleepguard_theme")

object ThemePrefs {

    private val DARK_MODE = booleanPreferencesKey("dark_mode")

    fun darkModeFlow(context: Context): Flow<Boolean?> =
        context.themeDataStore.data.map { it[DARK_MODE] }

    suspend fun setDarkMode(context: Context, dark: Boolean) {
        context.themeDataStore.edit { it[DARK_MODE] = dark }
    }
}
