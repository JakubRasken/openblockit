package com.openblockit.core

import java.time.LocalDate

object TowerRepo {
    const val BLOCK_MINUTES = 5
    const val FLOOR_DAYS = 7
    const val RESERVE_EVERY = 10
    const val MAX_RESERVES = 3
    const val HUNDRED_GIFT_DAYS = 100

    fun activeDays(days: List<com.openblockit.data.DayEntity>): Set<String> {
        val set = HashSet<String>()
        days.forEach { if (it.minutes >= BLOCK_MINUTES) set.add(it.date) }
        set.addAll(Prefs.repairedDays)
        return set
    }

    fun currentStreak(days: List<com.openblockit.data.DayEntity>): Int {
        val active = activeDays(days)
        if (active.isEmpty()) return 0
        val today = Clock.todayKey()
        var cursor = if (active.contains(today)) LocalDate.parse(today) else LocalDate.parse(today).minusDays(1)
        if (!active.contains(cursor.toString())) return 0
        var streak = 0
        while (active.contains(cursor.toString())) {
            streak++
            cursor = cursor.minusDays(1)
        }
        return streak
    }

    fun floors(streak: Int): Int = streak / FLOOR_DAYS

    fun blocksInFloor(streak: Int): Int = streak % FLOOR_DAYS

    fun maybeGrantReserves(streak: Int) {
        val due = (streak / RESERVE_EVERY).coerceAtMost(MAX_RESERVES)
        val granted = Prefs.reserveGrantedTotal
        if (due > granted) {
            Prefs.reserveBlocks = Prefs.reserveBlocks + (due - granted)
            Prefs.reserveGrantedTotal = due
            Prefs.addParachutes(due - granted)
        }
    }

    fun maybeHundredGift(streak: Int) {
        if (streak >= HUNDRED_GIFT_DAYS && !Prefs.hundredGiftGiven) {
            Prefs.hundredGiftGiven = true
            Prefs.hundredGiftPending = true
            Prefs.addParachutes(1)
        }
    }

    fun updateBest(streak: Int) {
        if (streak > Prefs.bestStreak) Prefs.bestStreak = streak
    }

    fun resolveMissedDays(days: List<com.openblockit.data.DayEntity>) {
        val activeDates = activeDays(days).mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }.toSortedSet()
        val today = LocalDate.parse(Clock.todayKey())
        val lastActive = activeDates.lastOrNull() ?: return
        var cursor = lastActive.plusDays(1)
        var reserve = Prefs.reserveBlocks
        val repaired = Prefs.repairedDays
        var repairedSomething = false
        var lastRepaired: String? = null
        while (cursor.isBefore(today)) {
            val key = cursor.toString()
            if (reserve > 0) {
                reserve--
                repaired.add(key)
                repairedSomething = true
                lastRepaired = key
            }
            cursor = cursor.plusDays(1)
        }
        if (repairedSomething) {
            Prefs.reserveBlocks = reserve
            Prefs.repairedDays = repaired
            lastRepaired?.let { Prefs.lastRepairedNotice = it }
        }
    }

    fun repairDayWithParachute(date: String): Boolean {
        if (Prefs.parachuteCount < 1) return false
        Prefs.parachuteCount = Prefs.parachuteCount - 1
        val repaired = Prefs.repairedDays
        repaired.add(date)
        Prefs.repairedDays = repaired
        return true
    }

    fun brokenTowerReset(days: List<com.openblockit.data.DayEntity>) {
        val active = activeDays(days)
        if (active.isEmpty()) return
        val today = LocalDate.parse(Clock.todayKey())
        val yesterday = today.minusDays(1).toString()
        if (active.contains(today.toString())) return
        if (!active.contains(yesterday) && Prefs.reserveBlocks <= 0) {
            Prefs.reserveGrantedTotal = 0
            Prefs.repairedDays = mutableSetOf()
        }
    }

    fun onSessionCompleted(days: List<com.openblockit.data.DayEntity>) {
        val streak = currentStreak(days)
        maybeGrantReserves(streak)
        maybeHundredGift(streak)
        updateBest(streak)
    }

    fun foundedDate(days: List<com.openblockit.data.DayEntity>): String? {
        val first = days.filter { it.minutes >= BLOCK_MINUTES }.minByOrNull { it.date } ?: return null
        return first.date
    }
}
