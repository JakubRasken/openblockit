package com.openblockit.core

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

val AppScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

object Clock {
    private val dayFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    fun todayKey(): String = dayFormatter.format(Instant.now().atZone(ZoneId.systemDefault()))

    fun dayKey(epochMillis: Long): String =
        dayFormatter.format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()))

    fun localDate(key: String) = java.time.LocalDate.parse(key, dayFormatter)
}

fun formatClock(totalSeconds: Int): String {
    val s = totalSeconds.coerceAtLeast(0)
    val h = s / 3600
    val m = (s % 3600) / 60
    val sec = s % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%02d:%02d".format(m, sec)
}

fun formatDurationMinutes(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h > 0 && m > 0 -> "${h}h ${m}m"
        h > 0 -> "${h}h"
        else -> "${m}m"
    }
}

fun endTimeLabel(endMillis: Long): String {
    val fmt = java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT)
    return fmt.format(java.util.Date(endMillis))
}

object AccessibilityUtils {
    fun enabled(context: Context): Boolean {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager ?: return false
        if (!am.isEnabled) return false
        val list = am.getEnabledAccessibilityServiceList(android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        for (info in list) {
            val id = info.id ?: continue
            if (id.contains(context.packageName)) return true
        }
        return false
    }

    fun declared(context: Context): Boolean {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager ?: return false
        for (info in am.getInstalledAccessibilityServiceList()) {
            val id = info.id ?: continue
            if (id.contains(context.packageName)) return true
        }
        return false
    }
}

object UsageUtils {
    fun lastForegroundPackage(context: Context): String? {
        return try {
            if (!Permissions.usageGranted(context)) return null
            val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? android.app.usage.UsageStatsManager
                ?: return null
            val now = System.currentTimeMillis()
            val events = usm.queryEvents(now - 60_000, now + 1_000)
            var last: String? = null
            val event = android.app.usage.UsageEvents.Event()
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                if (event.eventType == android.app.usage.UsageEvents.Event.MOVE_TO_FOREGROUND ||
                    event.eventType == android.app.usage.UsageEvents.Event.ACTIVITY_RESUMED
                ) {
                    last = event.packageName
                }
            }
            last
        } catch (_: Exception) {
            null
        }
    }
}

object Haptic {
    fun tick(context: Context, ms: Long = 8) {
        if (!Prefs.hapticsEnabled) return
        try {
            val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
            v.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
        } catch (_: Exception) {
        }
    }
}

object OverlayPermission {
    fun granted(context: Context): Boolean = Settings.canDrawOverlays(context)

    fun openSettings(context: Context) {
        try {
            val i = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, android.net.Uri.parse("package:${context.packageName}"))
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(i)
        } catch (_: Exception) {
        }
    }
}

object BatteryUtils {
    fun ignoreRequested(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager ?: return false
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun openSettings(context: Context) {
        try {
            val i = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, android.net.Uri.parse("package:${context.packageName}"))
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(i)
        } catch (_: Exception) {
            try {
                val i = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(i)
            } catch (_: Exception) {
            }
        }
    }
}

fun supportsExactAlarms(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < 31) return true
    val am = context.getSystemService(Context.ALARM_SERVICE) as? android.app.AlarmManager ?: return false
    return am.canScheduleExactAlarms()
}
