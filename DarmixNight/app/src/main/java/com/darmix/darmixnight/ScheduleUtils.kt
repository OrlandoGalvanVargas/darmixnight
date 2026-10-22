package com.darmix.darmixnight

import java.util.Calendar

object ScheduleUtils {

    const val EMERGENCY_WINDOW_START = 5 * 60
    const val EMERGENCY_WINDOW_END = 23 * 60

    fun currentMinutesOfDay(calendar: Calendar = Calendar.getInstance()): Int {
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        return hour * 60 + minute
    }

    fun isWithinBlockRange(nowMinutes: Int, startMinutes: Int, endMinutes: Int): Boolean {
        if (startMinutes == endMinutes) return true
        return if (startMinutes < endMinutes) {
            nowMinutes in startMinutes until endMinutes
        } else {
            nowMinutes >= startMinutes || nowMinutes < endMinutes
        }
    }

    fun isCurrentlyWithinBlockRange(startMinutes: Int, endMinutes: Int): Boolean {
        return isWithinBlockRange(currentMinutesOfDay(), startMinutes, endMinutes)
    }

    fun isWithinEmergencyUnlockWindow(nowMinutes: Int): Boolean {
        return nowMinutes in EMERGENCY_WINDOW_START until EMERGENCY_WINDOW_END
    }

    fun formatMinutes(minutes: Int): String {
        val h = minutes / 60
        val m = minutes % 60
        return "%02d:%02d".format(h, m)
    }
}
