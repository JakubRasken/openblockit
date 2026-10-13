package com.openblockit

import android.app.Application
import com.openblockit.core.AppScope
import com.openblockit.core.BagRepo
import com.openblockit.core.Notifications
import com.openblockit.core.Prefs
import com.openblockit.core.SessionManager
import com.openblockit.core.StatsRepo
import com.openblockit.core.TowerRepo
import com.openblockit.session.RoutineScheduler
import com.openblockit.session.StreakReminderReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class OpenBlockitApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Prefs.init(this)
        StatsRepo.init(this)
        BagRepo.init(this)
        Notifications.createChannels(this)
        SessionManager.init(this)
        com.openblockit.session.OverlayPill.init(this)
        RoutineScheduler.scheduleAll(this)
        StreakReminderReceiver.schedule(this)
        if (SessionManager.active.value) {
            SessionManager.startLockService()
        }
        AppScope.launch(Dispatchers.IO) {
            try {
                val days = StatsRepo.allDays()
                TowerRepo.resolveMissedDays(days)
                TowerRepo.brokenTowerReset(days)
                TowerRepo.maybeGrantReserves(TowerRepo.currentStreak(days))
            } catch (_: Exception) {
            }
        }
    }
}
