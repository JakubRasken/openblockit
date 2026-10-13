package com.openblockit.core

import android.app.NotificationManager
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.os.SystemClock
import androidx.core.app.NotificationManagerCompat
import com.openblockit.MainActivity
import com.openblockit.session.AdminReceiver
import com.openblockit.session.BlockAccessibilityService
import com.openblockit.session.LockService
import com.openblockit.session.LockWatchdogReceiver
import com.openblockit.session.RoutineScheduler
import com.openblockit.ui.block.BlockActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs

enum class StartResult { Started, AlreadyRunning, NeedsParachute }

object SessionManager {
    private lateinit var app: Context
    private var wakeLock: PowerManager.WakeLock? = null
    private var lastTickAt = 0L
    private var lastNotifiedMinute = -1
    private var lastNotifiedAt = 0L
    private var guardianTicks = 0

    private val _active = MutableStateFlow(false)
    val active: StateFlow<Boolean> = _active
    private val _secondsLeft = MutableStateFlow(0)
    val secondsLeft: StateFlow<Int> = _secondsLeft
    private val _totalSeconds = MutableStateFlow(0)
    val totalSeconds: StateFlow<Int> = _totalSeconds

    fun init(context: Context) {
        app = context.applicationContext
        _active.value = started()
        _secondsLeft.value = secondsRemaining()
        _totalSeconds.value = Prefs.secondsToEnd
    }

    fun started(): Boolean = Prefs.endOfLockMillis > System.currentTimeMillis()

    fun secondsRemaining(): Int {
        val end = Prefs.endOfLockMillis
        if (end <= 0L) return 0
        val ms = end - System.currentTimeMillis()
        return if (ms <= 0L) 0 else ((ms + 999L) / 1000L).toInt()
    }

    fun start(minutes: Int): StartResult {
        if (started()) return StartResult.AlreadyRunning
        if (minutes > 180 && Prefs.parachuteCount < 1) return StartResult.NeedsParachute
        val now = System.currentTimeMillis()
        val end = routineAwareEnd(now + minutes * 60_000L, now)
        val secs = ((end - now) / 1000L).toInt()
        Prefs.endOfLockMillis = end
        Prefs.startOfLockMillis = now
        Prefs.legEndMillis = now + minutes * 60_000L
        Prefs.secondsToEnd = secs
        Prefs.statsLoggedUntil = now
        lastNotifiedMinute = -1
        guardianTicks = 0
        _active.value = true
        _secondsLeft.value = secs
        _totalSeconds.value = secs
        acquireWakeLock(secs + 2)
        applySilence()
        startLockService()
        LockWatchdogReceiver.schedule(app, 15_000L)
        Sounds.start(app, Prefs.soundMode)
        return StartResult.Started
    }

    fun extend(minutes: Int = 15): Boolean {
        if (!started()) return false
        if (Prefs.parachuteCount < 1) return false
        absorb(minutes)
        return true
    }

    fun absorbRoutine(minutes: Int) = absorb(minutes)

    private fun absorb(minutes: Int) {
        if (!started()) return
        Prefs.secondsToEnd = Prefs.secondsToEnd + minutes * 60
        Prefs.endOfLockMillis = Prefs.endOfLockMillis + minutes * 60_000L
        _totalSeconds.value = Prefs.secondsToEnd
        acquireWakeLock(secondsRemaining() + 2)
        pushNotification(force = true)
    }

    fun endByParachute(): Boolean {
        if (!started()) return false
        if (Prefs.parachuteCount < 1) return false
        Prefs.parachuteCount = Prefs.parachuteCount - 1
        finish(completed = false)
        return true
    }

    fun tick() {
        val now = SystemClock.uptimeMillis()
        if (now - lastTickAt < 900L) return
        lastTickAt = now
        val end = Prefs.endOfLockMillis
        val start = Prefs.startOfLockMillis
        if (end <= 0L) {
            if (_active.value) finish(completed = false, silent = true)
            return
        }
        if (System.currentTimeMillis() >= end) {
            finish(completed = start > 0L)
            return
        }
        StatsRepo.flushLockedTime(System.currentTimeMillis())
        val left = secondsRemaining()
        _secondsLeft.value = left
        if (abs(Prefs.secondsToEnd - left) > 2) Prefs.secondsToEnd = left
        acquireWakeLock(left + 2)
        pushNotification(force = false)
        Sounds.update(left)
        com.openblockit.session.OverlayPill.refresh(left)
        enforceFallback()
        if (left <= 3 && Prefs.hapticsEnabled) Haptic.tick(app, (4 - left) * 100L)
    }

    private fun enforceFallback() {
        if (AccessibilityUtils.enabled(app)) {
            guardianTicks = 0
            return
        }
        guardianTicks++
        val last = UsageUtils.lastForegroundPackage(app)
        val keyguard = (app.getSystemService(Context.KEYGUARD_SERVICE) as? android.app.KeyguardManager)?.isKeyguardLocked == true
        if (!keyguard && last != null && !BagRepo.isAllowed(last) && !last.startsWith(app.packageName)) {
            showBlockScreen()
            if (AdminReceiver.isActive(app)) lockScreenNow()
        }
        if (guardianTicks == 5) {
            Prefs.protectionBroke = true
            try {
                NotificationManagerCompat.from(app)
                    .notify(Notifications.ID_PROTECTION, Notifications.protection(app, stuck = AccessibilityUtils.declared(app)))
            } catch (_: Exception) {
            }
        }
    }

    fun showBlockScreen() {
        try {
            val intent = Intent(app, BlockActivity::class.java).addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NO_ANIMATION or
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
            )
            app.startActivity(intent)
        } catch (_: Exception) {
        }
    }

    fun openMain(completed: Boolean = false) {
        try {
            val intent = Intent(app, MainActivity::class.java).addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            )
            if (completed) intent.putExtra("completed", true)
            app.startActivity(intent)
        } catch (_: Exception) {
        }
    }

    fun lockScreenNow() {
        val service = BlockAccessibilityService.instance
        if (service != null) {
            try {
                if (service.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN)) return
            } catch (_: Exception) {
            }
        }
        if (AdminReceiver.isActive(app)) {
            try {
                val dpm = app.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager ?: return
                dpm.lockNow()
            } catch (_: Exception) {
            }
        }
    }

    private fun finish(completed: Boolean, silent: Boolean = false) {
        val start = Prefs.startOfLockMillis
        val elapsedMinutes = if (start > 0) ((System.currentTimeMillis() - start) / 60_000L).toInt().coerceAtLeast(0) else 0
        StatsRepo.flushLockedTime(System.currentTimeMillis())
        Prefs.endOfLockMillis = 0L
        Prefs.secondsToEnd = 0
        Prefs.startOfLockMillis = 0L
        Prefs.legEndMillis = 0L
        _active.value = false
        _secondsLeft.value = 0
        _totalSeconds.value = 0
        restoreSilence()
        releaseWakeLock()
        stopLockService()
        LockWatchdogReceiver.cancel(app)
        Sounds.stop()
        val prevBag = Prefs.bagBeforeRoutine
        if (prevBag >= 0) {
            Prefs.activeBag = prevBag
            Prefs.bagBeforeRoutine = -1
        }
        AppScope.launch(Dispatchers.IO) {
            StatsRepo.completeSession(completed)
            val days = StatsRepo.allDays()
            TowerRepo.onSessionCompleted(days)
        }
        if (completed && !silent) {
            try {
                NotificationManagerCompat.from(app)
                    .notify(Notifications.ID_COMPLETE, Notifications.completion(app, elapsedMinutes))
            } catch (_: Exception) {
            }
        }
        if (!silent) openMain(completed)
    }

    fun reset() {
        Prefs.endOfLockMillis = 0L
        Prefs.secondsToEnd = 0
        Prefs.startOfLockMillis = 0L
        Prefs.legEndMillis = 0L
        _active.value = false
        _secondsLeft.value = 0
        _totalSeconds.value = 0
        releaseWakeLock()
        restoreSilence()
        stopLockService()
        LockWatchdogReceiver.cancel(app)
        Sounds.stop()
    }

    fun startLockService() {
        try {
            app.startForegroundService(Intent(app, LockService::class.java))
        } catch (_: Exception) {
        }
    }

    private fun stopLockService() {
        try {
            app.startForegroundService(Intent(app, LockService::class.java).setAction(LockService.ACTION_STOP))
        } catch (_: Exception) {
            try {
                app.stopService(Intent(app, LockService::class.java))
            } catch (_: Exception) {
            }
        }
    }

    fun pushNotification(force: Boolean) {
        val left = secondsRemaining()
        val minuteBucket = (left + 59) / 60
        val now = SystemClock.uptimeMillis()
        if (!force && minuteBucket == lastNotifiedMinute && now - lastNotifiedAt < 100_000L) return
        lastNotifiedMinute = minuteBucket
        lastNotifiedAt = now
        try {
            NotificationManagerCompat.from(app)
                .notify(Notifications.ID_COUNTDOWN, Notifications.countdown(app, left, Prefs.endOfLockMillis))
        } catch (_: Exception) {
        }
    }

    private fun acquireWakeLock(seconds: Int) {
        try {
            if (wakeLock == null) {
                val pm = app.getSystemService(Context.POWER_SERVICE) as PowerManager
                wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "openblockit:session").apply { setReferenceCounted(false) }
            }
            wakeLock?.acquire(seconds.coerceAtLeast(1) * 1000L)
        } catch (_: Exception) {
        }
    }

    private fun releaseWakeLock() {
        try {
            wakeLock?.release()
        } catch (_: Exception) {
        }
    }

    private fun applySilence() {
        if (!Prefs.sessionSilence) return
        try {
            val nm = app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (!nm.isNotificationPolicyAccessGranted) return
            val current = nm.currentInterruptionFilter
            if (current == NotificationManager.INTERRUPTION_FILTER_ALL) {
                Prefs.dndFilterBefore = current
                nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE)
            }
        } catch (_: Exception) {
        }
    }

    private fun restoreSilence() {
        try {
            val before = Prefs.dndFilterBefore
            if (before == 0) return
            val nm = app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (!nm.isNotificationPolicyAccessGranted) {
                Prefs.dndFilterBefore = 0
                return
            }
            if (nm.currentInterruptionFilter == NotificationManager.INTERRUPTION_FILTER_NONE) {
                nm.setInterruptionFilter(before)
            }
            Prefs.dndFilterBefore = 0
        } catch (_: Exception) {
        }
    }

    private fun routineAwareEnd(end: Long, now: Long): Long {
        return try {
            if (Prefs.parachuteCount < 1) return end
            var result = end
            val cap = now + 12 * 60 * 60 * 1000L
            for (i in 0..2) {
                val r = Prefs.routine(i)
                if (!r.enabled) continue
                val next = RoutineScheduler.nextOccurrence(i, now)
                if (next > now && next < result) {
                    result = (next + r.minutes * 60_000L).coerceAtMost(cap)
                }
            }
            if (result > cap) cap else result
        } catch (_: Exception) {
            end
        }
    }
}
