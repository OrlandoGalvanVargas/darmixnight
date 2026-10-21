package com.darmix.darmixnight

import java.util.Calendar

/**
 * Lógica pura de horario. No depende de Android para poder probarla fácil
 * y para poder usarla desde cualquier componente (Service, Activity, etc).
 */
object ScheduleUtils {

    /**
     * Devuelve los minutos transcurridos desde medianoche para el instante actual.
     * Ej: 23:45 -> 1425, 00:10 -> 10.
     */
    fun currentMinutesOfDay(calendar: Calendar = Calendar.getInstance()): Int {
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        return hour * 60 + minute
    }

    /**
     * Indica si `nowMinutes` cae dentro del rango [startMinutes, endMinutes).
     * Soporta rangos que cruzan la medianoche (ej. start=1410 "23:30", end=300 "05:00").
     *
     * Casos:
     * - start < end  (ej. 08:00 -> 17:00, mismo día): dentro si nowMinutes está entre ambos.
     * - start > end  (ej. 23:30 -> 05:00, cruza medianoche): dentro si nowMinutes >= start
     *                 O nowMinutes < end.
     * - start == end: se interpreta como "bloqueo 24h" (caso extremo, evitar en la UI).
     */
    fun isWithinBlockRange(nowMinutes: Int, startMinutes: Int, endMinutes: Int): Boolean {
        if (startMinutes == endMinutes) return true

        return if (startMinutes < endMinutes) {
            nowMinutes in startMinutes until endMinutes
        } else {
            // Cruza medianoche
            nowMinutes >= startMinutes || nowMinutes < endMinutes
        }
    }

    /**
     * Azúcar sintáctica: usa el reloj real del sistema.
     */
    fun isCurrentlyWithinBlockRange(startMinutes: Int, endMinutes: Int): Boolean {
        return isWithinBlockRange(currentMinutesOfDay(), startMinutes, endMinutes)
    }

    /**
     * Ventana horaria en la que el desbloqueo de emergencia puede usarse:
     * 05:00 AM a 11:00 PM. Fuera de este rango (23:00-05:00, que cubre el
     * horario de sueño real por defecto) el desbloqueo de emergencia no
     * está disponible bajo ninguna circunstancia.
     */
    const val EMERGENCY_WINDOW_START = 5 * 60   // 05:00
    const val EMERGENCY_WINDOW_END = 23 * 60    // 23:00

    fun isWithinEmergencyUnlockWindow(nowMinutes: Int): Boolean {
        return nowMinutes in EMERGENCY_WINDOW_START until EMERGENCY_WINDOW_END
    }

    /**
     * Formatea minutos-desde-medianoche como "HH:mm" (24h), útil para mostrar en UI de depuración.
     */
    fun formatMinutes(minutes: Int): String {
        val h = minutes / 60
        val m = minutes % 60
        return "%02d:%02d".format(h, m)
    }
}