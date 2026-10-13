package com.openblockit.session

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.openblockit.core.Prefs
import com.openblockit.core.SessionManager

class RoutineReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val index = intent?.getIntExtra("routine_index", 0) ?: 0
        val r = Prefs.routine(index)
        if (!r.enabled) return
        if (Prefs.parachuteCount < 1) {
            Prefs.setRoutine(index, r.copy(enabled = false))
            RoutineScheduler.cancel(context, index)
            return
        }
        RoutineScheduler.scheduleNext(context, index)
        if (!SessionManager.active.value) {
            Prefs.activeRoutine = index
            Prefs.bagBeforeRoutine = Prefs.activeBag
            Prefs.activeBag = index
            val result = SessionManager.start(r.minutes)
            if (result == com.openblockit.core.StartResult.Started) {
                SessionManager.openMain()
            } else {
                val prev = Prefs.bagBeforeRoutine
                if (prev >= 0) {
                    Prefs.activeBag = prev
                    Prefs.bagBeforeRoutine = -1
                }
            }
        } else {
            SessionManager.absorbRoutine(r.minutes)
        }
    }
}
