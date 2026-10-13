package com.openblockit.core

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.openblockit.session.AdminReceiver

object Permissions {
    fun openAccessibility(context: Context) {
        start(context, Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    fun openOverlay(context: Context) = OverlayPermission.openSettings(context)

    fun openBattery(context: Context) = BatteryUtils.openSettings(context)

    fun openNotifications(context: Context) {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        start(context, intent)
    }

    fun openAlarms(context: Context) {
        if (Build.VERSION.SDK_INT >= 31) {
            start(context, Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")))
        }
    }

    fun openUsageAccess(context: Context) {
        start(context, Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
    }

    fun openDnd(context: Context) {
        start(context, Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
    }

    fun requestAdmin(context: Context) {
        try {
            context.startActivity(AdminReceiver.requestIntent(context).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: Exception) {
        }
    }

    fun notificationsGranted(context: Context): Boolean {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return nm.areNotificationsEnabled()
    }

    fun usageGranted(context: Context): Boolean {
        return try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager
            val mode = if (Build.VERSION.SDK_INT >= 29) {
                appOps.unsafeCheckOpNoThrow("android:get_usage_stats", android.os.Process.myUid(), context.packageName)
            } else {
                @Suppress("DEPRECATION")
                appOps.checkOpNoThrow("android:get_usage_stats", android.os.Process.myUid(), context.packageName)
            }
            mode == android.app.AppOpsManager.MODE_ALLOWED
        } catch (_: Exception) {
            false
        }
    }

    fun dndGranted(context: Context): Boolean {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return nm.isNotificationPolicyAccessGranted
    }

    private fun start(context: Context, intent: Intent) {
        try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (_: Exception) {
        }
    }
}
