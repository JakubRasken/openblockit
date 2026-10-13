package com.openblockit.session

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import com.openblockit.core.AppScope
import com.openblockit.core.Clock
import com.openblockit.core.Notifications
import com.openblockit.core.Prefs
import com.openblockit.core.SessionManager
import com.openblockit.core.StatsRepo
import com.openblockit.core.TowerRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class StreakReminderReceiver : BroadcastReceiver() {
    companion object {
        private const val REQUEST_CODE = 2200

        fun schedule(context: Context) {
            val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 20)
                set(Calendar.MINUTE, 30)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (calendar.timeInMillis <= System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }
            val pi = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE,
                Intent(context, StreakReminderReceiver::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            try {
                if (Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()) {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pi)
                } else {
                    am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pi)
                }
            } catch (_: Exception) {
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val pending = goAsync()
        val app = context.applicationContext
        AppScope.launch(Dispatchers.IO) {
            try {
                val days = StatsRepo.allDays()
                TowerRepo.resolveMissedDays(days)
                val today = Clock.todayKey()
                val minutesToday = days.firstOrNull { it.date == today }?.minutes ?: 0
                if (Prefs.streakReminderEnabled && minutesToday < TowerRepo.BLOCK_MINUTES && !SessionManager.active.value) {
                    val streak = TowerRepo.currentStreak(days)
                    val danger = streak > 0
                    val id = if (danger) Notifications.ID_STREAK_DANGER else Notifications.ID_STREAK
                    withContext(Dispatchers.Main) {
                        try {
                            NotificationManagerCompat.from(app)
                                .notify(id, Notifications.streakReminder(app, danger, streak))
                        } catch (_: Exception) {
                        }
                    }
                }
            } catch (_: Exception) {
            } finally {
                schedule(app)
                pending.finish()
            }
        }
    }
}
