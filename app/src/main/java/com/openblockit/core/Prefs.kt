package com.openblockit.core

import android.content.Context
import android.content.SharedPreferences

object Prefs {
    private const val MAIN = "blockit"
    private const val LOCK = "lock_manager"
    const val SLOTS = 6

    lateinit var main: SharedPreferences
        private set
    lateinit var lock: SharedPreferences
        private set

    fun init(context: Context) {
        val ctx = context.applicationContext
        main = ctx.getSharedPreferences(MAIN, Context.MODE_PRIVATE)
        lock = ctx.getSharedPreferences(LOCK, Context.MODE_PRIVATE)
    }

    var endOfLockMillis: Long
        get() = lock.getLong("end_of_lock", 0L)
        set(v) { lock.edit().putLong("end_of_lock", v).commit() }

    var startOfLockMillis: Long
        get() = lock.getLong("start_of_lock", 0L)
        set(v) { lock.edit().putLong("start_of_lock", v).commit() }

    var secondsToEnd: Int
        get() = lock.getInt("seconds_to_end", 0)
        set(v) { lock.edit().putInt("seconds_to_end", v).commit() }

    var legEndMillis: Long
        get() = lock.getLong("leg_end", 0L)
        set(v) { lock.edit().putLong("leg_end", v).commit() }

    var statsLoggedUntil: Long
        get() = lock.getLong("stats_logged_until", 0L)
        set(v) { lock.edit().putLong("stats_logged_until", v).commit() }

    var isBoot: Boolean
        get() = lock.getBoolean("is_boot", false)
        set(v) { lock.edit().putBoolean("is_boot", v).commit() }

    var dndFilterBefore: Int
        get() = main.getInt("dnd_filter_before", 0)
        set(v) { main.edit().putInt("dnd_filter_before", v).apply() }

    fun bagSlots(bag: Int): MutableList<String> {
        val key = if (bag <= 0) "whitelist_apps" else "whitelist_apps_$bag"
        val parts = (main.getString(key, "") ?: "").split("#").toMutableList()
        while (parts.size < SLOTS) parts.add("")
        return parts.subList(0, SLOTS).toMutableList()
    }

    fun setBagSlots(bag: Int, apps: List<String>) {
        val key = if (bag <= 0) "whitelist_apps" else "whitelist_apps_$bag"
        val padded = apps.toMutableList()
        while (padded.size < SLOTS) padded.add("")
        main.edit().putString(key, padded.take(SLOTS).joinToString("#")).apply()
    }

    fun bagName(bag: Int): String = main.getString("bag_name_$bag", "") ?: ""

    fun setBagName(bag: Int, name: String) {
        main.edit().putString("bag_name_$bag", name).apply()
    }

    var activeBag: Int
        get() = main.getInt("active_bag", 0).coerceIn(0, 2)
        set(v) { main.edit().putInt("active_bag", v.coerceIn(0, 2)).apply() }

    var bagBeforeRoutine: Int
        get() = main.getInt("bag_before_routine", -1)
        set(v) { main.edit().putInt("bag_before_routine", v).apply() }

    var lastMinutes: Int
        get() = main.getInt("minutes", 30)
        set(v) { main.edit().putInt("minutes", v).apply() }

    var customMinutes: Int
        get() = main.getInt("custom_minutes", 180)
        set(v) { main.edit().putInt("custom_minutes", v.coerceIn(180, 720)).apply() }

    var parachuteCount: Int
        get() = main.getInt("parachute_count", 3).coerceIn(0, 99)
        set(v) { main.edit().putInt("parachute_count", v.coerceIn(0, 99)).apply() }

    fun addParachutes(n: Int) {
        parachuteCount = parachuteCount + n
    }

    var themeMode: Int
        get() = main.getInt("theme_mode", 0)
        set(v) { main.edit().putInt("theme_mode", v).apply() }

    var hapticsEnabled: Boolean
        get() = main.getBoolean("haptics", true)
        set(v) { main.edit().putBoolean("haptics", v).apply() }

    var streakReminderEnabled: Boolean
        get() = main.getBoolean("streak_reminder_enabled", true)
        set(v) { main.edit().putBoolean("streak_reminder_enabled", v).apply() }

    var sessionSilence: Boolean
        get() = main.getBoolean("session_silence", true)
        set(v) { main.edit().putBoolean("session_silence", v).apply() }

    var soundMode: Int
        get() = main.getInt("sound_mode", 0)
        set(v) { main.edit().putInt("sound_mode", v).apply() }

    var onboardingDone: Boolean
        get() = main.getBoolean("onboarding_done", false)
        set(v) { main.edit().putBoolean("onboarding_done", v).apply() }

    var streakSince: String
        get() = main.getString("streak_since", "") ?: ""
        set(v) { main.edit().putString("streak_since", v).apply() }

    var repairedDays: MutableSet<String>
        get() = (main.getStringSet("repaired_days", emptySet()) ?: emptySet()).toMutableSet()
        set(v) { main.edit().putStringSet("repaired_days", v).apply() }

    var reserveBlocks: Int
        get() = main.getInt("reserve_blocks", 0).coerceIn(0, 3)
        set(v) { main.edit().putInt("reserve_blocks", v.coerceIn(0, 3)).apply() }

    var reserveGrantedTotal: Int
        get() = main.getInt("reserve_granted_total", 0)
        set(v) { main.edit().putInt("reserve_granted_total", v).apply() }

    var bestStreak: Int
        get() = main.getInt("best_streak", 0)
        set(v) { main.edit().putInt("best_streak", v).apply() }

    var hundredGiftGiven: Boolean
        get() = main.getBoolean("hundred_gift_given", false)
        set(v) { main.edit().putBoolean("hundred_gift_given", v).apply() }

    var hundredGiftPending: Boolean
        get() = main.getBoolean("hundred_gift_pending", false)
        set(v) { main.edit().putBoolean("hundred_gift_pending", v).apply() }

    var protectionBroke: Boolean
        get() = main.getBoolean("protection_broke", false)
        set(v) { main.edit().putBoolean("protection_broke", v).apply() }

    var preAlertMinutes: Int
        get() = main.getInt("prealert_minutes", 15)
        set(v) { main.edit().putInt("prealert_minutes", v).apply() }

    var activeRoutine: Int
        get() = main.getInt("active_routine", 0).coerceIn(0, 2)
        set(v) { main.edit().putInt("active_routine", v.coerceIn(0, 2)).apply() }

    var lastRepairOfferDay: String
        get() = main.getString("last_repair_offer", "") ?: ""
        set(v) { main.edit().putString("last_repair_offer", v).apply() }

    var lastRepairedNotice: String
        get() = main.getString("last_repaired_notice", "") ?: ""
        set(v) { main.edit().putString("last_repaired_notice", v).apply() }

    fun routine(index: Int): Routine = Routine(
        enabled = main.getBoolean("routine_enabled_$index", false),
        days = (main.getStringSet("routine_days_$index", setOf("1", "2", "3", "4", "5", "6", "7")) ?: emptySet())
            .mapNotNull { it.toIntOrNull() }.toSet(),
        hour = main.getInt("routine_hour_$index", 20),
        minute = main.getInt("routine_minute_$index", 0),
        minutes = main.getInt("routine_minutes_$index", 60),
    )

    fun setRoutine(index: Int, r: Routine) {
        main.edit()
            .putBoolean("routine_enabled_$index", r.enabled)
            .putStringSet("routine_days_$index", r.days.map { it.toString() }.toSet())
            .putInt("routine_hour_$index", r.hour)
            .putInt("routine_minute_$index", r.minute)
            .putInt("routine_minutes_$index", r.minutes)
            .apply()
    }
}

data class Routine(
    val enabled: Boolean = false,
    val days: Set<Int> = setOf(1, 2, 3, 4, 5, 6, 7),
    val hour: Int = 20,
    val minute: Int = 0,
    val minutes: Int = 60,
)
