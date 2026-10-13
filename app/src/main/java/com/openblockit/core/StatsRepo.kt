package com.openblockit.core

import android.content.Context
import com.openblockit.data.AppDb
import com.openblockit.data.DayEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object StatsRepo {
    private lateinit var appContext: Context

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun flushLockedTime(now: Long) {
        val loggedUntil = Prefs.statsLoggedUntil
        if (loggedUntil <= 0L) {
            Prefs.statsLoggedUntil = now
            return
        }
        val delta = now - loggedUntil
        if (delta < 60_000L) return
        val minutes = (delta / 60_000L).toInt().coerceAtMost(1440)
        if (minutes > 0) {
            val hour = java.util.Calendar.getInstance().apply { timeInMillis = loggedUntil }
                .get(java.util.Calendar.HOUR_OF_DAY)
            addMinutes(Clock.dayKey(loggedUntil), minutes, hour)
            Prefs.statsLoggedUntil = loggedUntil + minutes * 60_000L
        }
    }

    fun addMinutes(date: String, minutes: Int, hour: Int = -1) {
        val ctx = appContext
        AppScope.launch(Dispatchers.IO) {
            val dao = AppDb.get(ctx).days()
            val existing = dao.byDate(date)
            val buckets = parseHourly(existing?.hourly)
            if (hour in 0..23) buckets[hour] += minutes
            dao.upsert(
                DayEntity(
                    date = date,
                    minutes = (existing?.minutes ?: 0) + minutes,
                    sessions = existing?.sessions ?: 0,
                    hourly = formatHourly(buckets),
                )
            )
        }
    }

    fun parseHourly(value: String?): IntArray {
        val result = IntArray(24)
        if (value.isNullOrEmpty()) return result
        value.split(',').forEachIndexed { index, part ->
            if (index < 24) result[index] = part.toIntOrNull() ?: 0
        }
        return result
    }

    private fun formatHourly(buckets: IntArray): String = buckets.joinToString(",")

    suspend fun hourlyOn(date: String): IntArray = withContext(Dispatchers.IO) {
        parseHourly(AppDb.get(appContext).days().byDate(date)?.hourly)
    }

    fun completeSession(completed: Boolean) {
        if (!completed) return
        val ctx = appContext
        AppScope.launch(Dispatchers.IO) {
            val date = Clock.todayKey()
            val dao = AppDb.get(ctx).days()
            val existing = dao.byDate(date)
            dao.upsert(
                DayEntity(
                    date = date,
                    minutes = existing?.minutes ?: 0,
                    sessions = (existing?.sessions ?: 0) + 1,
                    hourly = existing?.hourly ?: "",
                )
            )
        }
    }

    suspend fun minutesOn(date: String): Int = withContext(Dispatchers.IO) {
        AppDb.get(appContext).days().byDate(date)?.minutes ?: 0
    }

    suspend fun sessionsOn(date: String): Int = withContext(Dispatchers.IO) {
        AppDb.get(appContext).days().byDate(date)?.sessions ?: 0
    }

    suspend fun allDays(): List<DayEntity> = withContext(Dispatchers.IO) {
        AppDb.get(appContext).days().all()
    }

    suspend fun totalMinutes(): Int = withContext(Dispatchers.IO) {
        AppDb.get(appContext).days().all().sumOf { it.minutes }
    }
}
