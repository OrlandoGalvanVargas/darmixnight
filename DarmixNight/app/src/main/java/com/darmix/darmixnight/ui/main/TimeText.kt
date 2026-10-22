package com.darmix.darmixnight.ui.main

object TimeText {

    fun hour(minutes: Int): String {
        val h24 = (minutes / 60) % 24
        val h12 = if (h24 % 12 == 0) 12 else h24 % 12
        return "%d:%02d".format(h12, minutes % 60)
    }

    fun period(minutes: Int): String = if ((minutes / 60) % 24 < 12) "AM" else "PM"

    fun full(minutes: Int): String = "${hour(minutes)} ${period(minutes)}"

    fun span(totalMinutes: Int): String {
        val h = totalMinutes / 60
        val m = totalMinutes % 60
        return when {
            totalMinutes <= 0 -> "menos de 1 min"
            h == 0 -> "$m min"
            m == 0 -> "$h h"
            else -> "$h h $m min"
        }
    }

    fun between(fromMinutes: Int, toMinutes: Int): Int = ((toMinutes - fromMinutes) % 1440 + 1440) % 1440
}

enum class ScheduleEdge { START, END }
