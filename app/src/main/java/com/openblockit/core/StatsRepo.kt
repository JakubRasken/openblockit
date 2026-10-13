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
            addMinutes(Clock.dayKey(loggedUntil), minutes)
            Prefs.statsLoggedUntil = loggedUntil + minutes * 60_000L
        }
    }

    fun addMinutes(date: String, minutes: Int) {
        val ctx = appContext
        AppScope.launch(Dispatchers.IO) {
            val dao = AppDb.get(ctx).days()
            val existing = dao.byDate(date)
            dao.upsert(
                DayEntity(
                    date = date,
                    minutes = (existing?.minutes ?: 0) + minutes,
                    sessions = existing?.sessions ?: 0,
                )
            )
        }
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
