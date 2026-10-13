package com.openblockit.session

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.openblockit.core.AppScope
import com.openblockit.core.SessionManager
import com.openblockit.core.StatsRepo
import com.openblockit.core.TowerRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == null) return
        val app = context.applicationContext
        RoutineScheduler.scheduleAll(app)
        StreakReminderReceiver.schedule(app)
        if (SessionManager.active.value) {
            SessionManager.startLockService()
            SessionManager.showBlockScreen()
        }
        AppScope.launch(Dispatchers.IO) {
            try {
                val days = StatsRepo.allDays()
                TowerRepo.resolveMissedDays(days)
                TowerRepo.brokenTowerReset(days)
            } catch (_: Exception) {
            }
        }
    }
}
