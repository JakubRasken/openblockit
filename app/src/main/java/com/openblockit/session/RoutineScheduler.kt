package com.openblockit.session

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.openblockit.core.Prefs
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

object RoutineScheduler {
    private const val SESSION_REQUEST_BASE = 1001
    private const val PREALERT_REQUEST_BASE = 1002

    fun nextOccurrence(index: Int, afterMillis: Long): Long {
        val r = Prefs.routine(index)
        val zone = ZoneId.systemDefault()
        var date: LocalDate = Instant.ofEpochMilli(afterMillis).atZone(zone).toLocalDate()
        for (step in 0 until 15) {
            if (r.days.contains(date.dayOfWeek.value)) {
                val candidate = date.atTime(r.hour, r.minute).atZone(zone).toInstant().toEpochMilli()
                if (candidate > afterMillis) return candidate
            }
            date = date.plusDays(1)
        }
        return afterMillis + 7 * 24 * 60 * 60 * 1000L
    }

    fun scheduleAll(context: Context) {
        for (i in 0..2) {
            val r = Prefs.routine(i)
            if (r.enabled) scheduleNext(context, i) else cancel(context, i)
        }
    }

    fun scheduleNext(context: Context, index: Int) {
        val r = Prefs.routine(index)
        if (!r.enabled) return
        val start = nextOccurrence(index, System.currentTimeMillis())
        setAlarm(
            context,
            SESSION_REQUEST_BASE + index,
            start,
            Intent(context, RoutineReceiver::class.java).putExtra("routine_index", index),
        )
        val preAlert = Prefs.preAlertMinutes
        if (preAlert > 0) {
            val at = start - preAlert * 60_000L
            if (at > System.currentTimeMillis()) {
                setAlarm(
                    context,
                    PREALERT_REQUEST_BASE + index,
                    at,
                    Intent(context, RoutinePreAlertReceiver::class.java).putExtra("routine_index", index),
                )
            }
        }
    }

    fun cancel(context: Context, index: Int) {
        cancelAlarm(context, SESSION_REQUEST_BASE + index, Intent(context, RoutineReceiver::class.java))
        cancelAlarm(context, PREALERT_REQUEST_BASE + index, Intent(context, RoutinePreAlertReceiver::class.java))
    }

    private fun setAlarm(context: Context, requestCode: Int, at: Long, intent: Intent) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pi = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        try {
            if (Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            }
        } catch (_: Exception) {
        }
    }

    private fun cancelAlarm(context: Context, requestCode: Int, intent: Intent) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pi = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        try {
            am.cancel(pi)
        } catch (_: Exception) {
        }
    }
}
