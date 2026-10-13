package com.openblockit.session

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.openblockit.core.Notifications
import com.openblockit.core.Prefs
import com.openblockit.core.SessionManager
import com.openblockit.core.endTimeLabel

class RoutinePreAlertReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val index = intent?.getIntExtra("routine_index", 0) ?: 0
        val r = Prefs.routine(index)
        if (!r.enabled || Prefs.parachuteCount < 1) return
        if (SessionManager.active.value) return
        val minutes = Prefs.preAlertMinutes
        if (minutes <= 0) return
        val startsAt = endTimeLabel(RoutineScheduler.nextOccurrence(index, System.currentTimeMillis()))
        try {
            NotificationManagerCompat.from(context)
                .notify(Notifications.ID_PREALERT + index, Notifications.preAlert(context, index, startsAt, minutes))
        } catch (_: Exception) {
        }
    }
}
