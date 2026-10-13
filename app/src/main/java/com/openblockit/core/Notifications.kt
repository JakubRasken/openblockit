package com.openblockit.core

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.openblockit.MainActivity
import com.openblockit.R

object Notifications {
    const val CH_SESSION = "session"
    const val CH_COMPLETE = "complete"
    const val CH_ROUTINE = "routine"
    const val CH_STREAK = "streak"
    const val CH_PROTECTION = "protection"

    const val ID_COUNTDOWN = 1
    const val ID_COMPLETE = 2
    const val ID_PROTECTION = 3
    const val ID_PREALERT = 4001
    const val ID_STREAK = 2201
    const val ID_STREAK_DANGER = 2202

    fun createChannels(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(CH_SESSION, context.getString(R.string.notification_channel_session_name), NotificationManager.IMPORTANCE_LOW).apply {
                description = context.getString(R.string.notification_channel_session_desc)
                setShowBadge(false)
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(CH_COMPLETE, context.getString(R.string.notification_channel_complete_name), NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = context.getString(R.string.notification_channel_complete_desc)
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(CH_ROUTINE, context.getString(R.string.notification_channel_routine_name), NotificationManager.IMPORTANCE_HIGH).apply {
                description = context.getString(R.string.notification_channel_routine_desc)
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(CH_STREAK, context.getString(R.string.notification_channel_streak_name), NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = context.getString(R.string.notification_channel_streak_desc)
                enableVibration(true)
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(CH_PROTECTION, context.getString(R.string.notification_channel_protection_name), NotificationManager.IMPORTANCE_HIGH).apply {
                description = context.getString(R.string.notification_channel_protection_desc)
            }
        )
    }

    private fun contentIntent(context: Context): PendingIntent =
        PendingIntent.getActivity(
            context,
            50,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    fun countdown(context: Context, secondsLeft: Int, endMillis: Long): Notification {
        val title = "End of session at ${endTimeLabel(endMillis)}"
        val text = "${formatDurationMinutes((secondsLeft + 59) / 60)} remaining"
        return NotificationCompat.Builder(context, CH_SESSION)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setShowWhen(false)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setContentIntent(contentIntent(context))
            .build()
    }

    fun completion(context: Context, minutes: Int): Notification =
        NotificationCompat.Builder(context, CH_COMPLETE)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Congratulations")
            .setContentText("You completed a ${formatDurationMinutes(minutes)} session")
            .setAutoCancel(true)
            .setContentIntent(contentIntent(context))
            .build()

    fun preAlert(context: Context, routineIndex: Int, startsAt: String, minutes: Int): Notification =
        NotificationCompat.Builder(context, CH_ROUTINE)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Routine starting soon")
            .setContentText("Your block session begins at $startsAt, in $minutes minutes. Get ready.")
            .setAutoCancel(true)
            .setContentIntent(contentIntent(context))
            .build()

    fun streakReminder(context: Context, danger: Boolean, streak: Int): Notification {
        val title = if (danger) "The day is ending" else if (streak == 0) "Your first block" else "Time to build"
        val text = if (danger) "Five minutes of session save the tower" else "Five minutes of session lay today's block"
        return NotificationCompat.Builder(context, CH_STREAK)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .setContentIntent(contentIntent(context))
            .build()
    }

    fun protection(context: Context, stuck: Boolean): Notification {
        val title = if (stuck) "The lock is not protected" else "Block protection disabled"
        val text = if (stuck) {
            "The lock service looks active but the system is not running it. Turn the openblockit switch off and on again in accessibility settings."
        } else {
            "The lock service was turned off during the session. Open openblockit to turn it back on."
        }
        return NotificationCompat.Builder(context, CH_PROTECTION)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .setContentIntent(contentIntent(context))
            .build()
    }
}
