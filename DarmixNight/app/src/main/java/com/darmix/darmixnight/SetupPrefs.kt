package com.darmix.darmixnight

import android.content.Context
import androidx.core.content.edit

object SetupPrefs {

    private const val FILE = "darmixnight_setup"
    private const val KEY_COMPLETED = "setup_completed"
    private const val KEY_XIAOMI_DONE = "xiaomi_autostart_done"
    private const val KEY_RESTRICTION_ACK = "restriction_acknowledged"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun isSetupCompleted(context: Context): Boolean = prefs(context).getBoolean(KEY_COMPLETED, false)

    fun markSetupCompleted(context: Context) {
        prefs(context).edit { putBoolean(KEY_COMPLETED, true) }
    }

    fun isXiaomiAutostartDone(context: Context): Boolean = prefs(context).getBoolean(KEY_XIAOMI_DONE, false)

    fun setXiaomiAutostartDone(context: Context, done: Boolean) {
        prefs(context).edit { putBoolean(KEY_XIAOMI_DONE, done) }
    }

    fun isRestrictionAcknowledged(context: Context): Boolean = prefs(context).getBoolean(KEY_RESTRICTION_ACK, false)

    fun setRestrictionAcknowledged(context: Context, done: Boolean) {
        prefs(context).edit { putBoolean(KEY_RESTRICTION_ACK, done) }
    }
}
