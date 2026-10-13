package com.openblockit.session

import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent

class AdminReceiver : DeviceAdminReceiver() {
    companion object {
        fun component(context: Context): ComponentName = ComponentName(context, AdminReceiver::class.java)

        fun isActive(context: Context): Boolean {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager ?: return false
            return try {
                dpm.isAdminActive(component(context))
            } catch (_: Exception) {
                false
            }
        }

        fun requestIntent(context: Context): Intent =
            Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
                .putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, component(context))
                .putExtra(
                    DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                    "The backup lock lets openblockit turn the screen off when the accessibility service is not available during a block.",
                )
    }
}
