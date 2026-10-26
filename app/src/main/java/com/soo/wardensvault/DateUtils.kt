package com.soo.wardensvault

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

//Shared date helpers used across the app
object DateUtils {

    fun startOfCurrentMonth(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        return startOfDay(cal.timeInMillis)
    }

    fun startOfDay(millis: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    fun endOfDay(millis: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return cal.timeInMillis
    }

    fun endOfToday(): Long = endOfDay(System.currentTimeMillis())

    fun startOfToday(): Long = startOfDay(System.currentTimeMillis())

    //first millisecond of the month that contains [millis]
    fun startOfMonth(millis: Long): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        cal.set(Calendar.DAY_OF_MONTH, 1)
        return startOfDay(cal.timeInMillis)
    }

    //last millisecond of the month that contains [millis]
    fun endOfMonth(millis: Long): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        return endOfDay(cal.timeInMillis)
    }

    //a time inside the month before the one containing [millis]
    fun previousMonth(millis: Long): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.add(Calendar.MONTH, -1)
        return cal.timeInMillis
    }

    //e.g. 202610 - used to key "once per month" flags
    fun monthKey(millis: Long): String =
        SimpleDateFormat("yyyyMM", Locale.US).format(millis)

    //moves a due date forward by one billing cycle (Weekly / Monthly / Yearly)
    fun addFrequency(millis: Long, frequency: String): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        when (frequency) {
            "Weekly" -> cal.add(Calendar.DAY_OF_YEAR, 7)
            "Yearly" -> cal.add(Calendar.YEAR, 1)
            else -> cal.add(Calendar.MONTH, 1)
        }
        return cal.timeInMillis
    }

    //whole days from [from] to [to] (negative when [to] is earlier)
    fun daysBetween(from: Long, to: Long): Int {
        val a = Calendar.getInstance().apply { timeInMillis = startOfDay(from) }
        val b = Calendar.getInstance().apply { timeInMillis = startOfDay(to) }
        val diff = b.timeInMillis - a.timeInMillis
        return Math.round(diff / 86400000.0).toInt()
    }
}
