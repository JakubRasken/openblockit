package com.openblockit.session

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.openblockit.core.SessionManager

class LockWatchdogReceiver : BroadcastReceiver() {
    companion object {
        private const val REQUEST_CODE = 1003

        fun schedule(context: Context, delayMs: Long) {
            val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val at = System.currentTimeMillis() + delayMs.coerceAtLeast(1000L)
            val pi = pendingIntent(context)
            try {
                if (Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()) {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
                } else {
                    am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
                }
            } catch (_: Exception) {
            }
        }

        fun cancel(context: Context) {
            val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            try {
                am.cancel(pendingIntent(context))
            } catch (_: Exception) {
            }
        }

        private fun pendingIntent(context: Context): PendingIntent =
            PendingIntent.getBroadcast(
                context,
                REQUEST_CODE,
                Intent(context, LockWatchdogReceiver::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
    }

    override fun onReceive(context: Context, intent: Intent?) {
        if (!SessionManager.active.value) {
            return
        }
        if (!LockService.running) {
            SessionManager.startLockService()
        }
        schedule(context, 15_000L)
    }
}
