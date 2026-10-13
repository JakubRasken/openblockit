package com.openblockit.session

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import com.openblockit.core.Notifications
import com.openblockit.core.Prefs
import com.openblockit.core.SessionManager
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class LockService : Service() {
    companion object {
        const val ACTION_STOP = "com.openblockit.action.STOP_LOCK"

        @Volatile
        var running = false
            private set
    }

    private var executor: ScheduledExecutorService? = null
    private var receiver: BroadcastReceiver? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        if (!goForeground()) {
            stopSelf()
            return
        }
        running = true
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        val r = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                SessionManager.pushNotification(force = true)
            }
        }
        receiver = r
        try {
            registerReceiver(r, filter)
        } catch (_: Exception) {
        }
        startTicker()
    }

    private fun startTicker() {
        val exec = Executors.newSingleThreadScheduledExecutor()
        executor = exec
        exec.scheduleAtFixedRate({
            try {
                SessionManager.tick()
                if (!SessionManager.active.value) stopSelf()
            } catch (_: Throwable) {
            }
        }, 0L, 1L, TimeUnit.SECONDS)
    }

    private fun goForeground(): Boolean {
        val notification = Notifications.countdown(this, SessionManager.secondsRemaining(), Prefs.endOfLockMillis)
        return try {
            if (Build.VERSION.SDK_INT >= 34) {
                startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } else {
                startForeground(1, notification)
            }
            true
        } catch (_: Exception) {
            try {
                startForeground(1, notification)
                true
            } catch (_: Exception) {
                false
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            try {
                stopForeground(true)
            } catch (_: Exception) {
            }
            stopSelf()
            return START_NOT_STICKY
        }
        if (!goForeground()) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onDestroy() {
        running = false
        executor?.shutdownNow()
        executor = null
        receiver?.let {
            try {
                unregisterReceiver(it)
            } catch (_: Exception) {
            }
        }
        receiver = null
        if (SessionManager.active.value) {
            LockWatchdogReceiver.schedule(this, 5_000L)
        }
        super.onDestroy()
    }
}
