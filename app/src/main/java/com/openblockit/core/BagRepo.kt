package com.openblockit.core

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.provider.Settings
import android.telecom.TelecomManager
import android.view.inputmethod.InputMethodInfo
import android.view.inputmethod.InputMethodManager

object EssentialApps {
    fun build(context: Context): Set<String> {
        val set = HashSet<String>()
        set.add(context.packageName)
        set.add("android")
        set.add("com.android.systemui")

        fun add(pkg: String?) {
            if (!pkg.isNullOrEmpty()) set.add(pkg)
        }

        try {
            add(
                context.packageManager.resolveActivity(
                    Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_MESSAGING), PackageManager.MATCH_DEFAULT_ONLY
                )?.activityInfo?.packageName
            )
        } catch (_: Exception) {
        }
        try {
            val telecom = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
            add(telecom?.defaultDialerPackage)
        } catch (_: Exception) {
        }
        try {
            add(
                context.packageManager.resolveActivity(Intent("android.media.action.IMAGE_CAPTURE"), PackageManager.MATCH_DEFAULT_ONLY)
                    ?.activityInfo?.packageName
            )
        } catch (_: Exception) {
        }
        try {
            val defaultIme = Settings.Secure.getString(context.contentResolver, "default_input_method")
            if (!defaultIme.isNullOrEmpty()) {
                val slash = defaultIme.indexOf('/')
                add(if (slash > 0) defaultIme.substring(0, slash) else defaultIme)
            }
        } catch (_: Exception) {
        }
        try {
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.enabledInputMethodList?.forEach { info: InputMethodInfo -> add(info.packageName) }
        } catch (_: Exception) {
        }
        try {
            add(
                context.packageManager.resolveActivity(Intent("android.intent.action.SHOW_ALARMS"), PackageManager.MATCH_DEFAULT_ONLY)
                    ?.activityInfo?.packageName
            )
        } catch (_: Exception) {
        }

        set.add("com.android.incallui")
        set.add("com.samsung.android.incallui")
        set.add("com.android.phone")
        set.add("com.android.server.telecom")
        set.add("com.android.dialer")
        set.add("com.google.android.dialer")
        set.add("com.android.contacts")
        set.add("com.miui.contacts")
        set.add("com.samsung.android.dialer")
        set.add("com.samsung.android.contacts")
        set.add("com.oneplus.dialer")
        set.add("com.coloros.contacts")
        set.add("com.oppo.contacts")
        set.add("com.vivo.contacts")
        set.add("com.hihonor.contacts")
        set.add("com.huawei.contacts")

        try {
            val home = context.packageManager.resolveActivity(
                Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), PackageManager.MATCH_DEFAULT_ONLY
            )
            add(home?.activityInfo?.packageName)
        } catch (_: Exception) {
        }

        val intents = listOf(
            Intent(Intent.ACTION_DIAL),
            Intent(Intent.ACTION_DIAL, Uri.parse("tel:123")),
            Intent(Intent.ACTION_VIEW, Uri.parse("tel:123")),
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_CONTACTS),
        )
        for (intent in intents) {
            try {
                context.packageManager.queryIntentActivities(intent, 0).forEach { ri ->
                    ri.activityInfo?.packageName?.let { add(it) }
                }
            } catch (_: Exception) {
            }
        }
        return set
    }
}

data class AppEntry(
    val packageName: String,
    val label: String,
    val icon: Drawable,
)

object BagRepo {
    private lateinit var appContext: Context
    private var essentials: Set<String> = emptySet()

    fun init(context: Context) {
        appContext = context.applicationContext
        refreshEssentials()
    }

    fun refreshEssentials() {
        essentials = EssentialApps.build(appContext)
    }

    fun isAllowed(pkg: String?): Boolean {
        if (pkg.isNullOrEmpty()) return false
        if (pkg.startsWith(appContext.packageName)) return true
        if (essentials.contains(pkg)) return true
        return Prefs.bagSlots(Prefs.activeBag).contains(pkg)
    }

    fun isEssential(pkg: String): Boolean = essentials.contains(pkg) || pkg.startsWith(appContext.packageName)

    fun appsIn(bag: Int): List<String> = Prefs.bagSlots(bag).filter { it.isNotEmpty() }

    fun addApp(bag: Int, pkg: String) {
        val slots = Prefs.bagSlots(bag)
        if (slots.contains(pkg)) return
        val index = slots.indexOfFirst { it.isEmpty() }
        if (index < 0) return
        slots[index] = pkg
        Prefs.setBagSlots(bag, slots)
    }

    fun removeApp(bag: Int, pkg: String) {
        val slots = Prefs.bagSlots(bag)
        val index = slots.indexOf(pkg)
        if (index < 0) return
        slots[index] = ""
        Prefs.setBagSlots(bag, slots)
    }

    fun installedApps(): List<AppEntry> {
        val pm = appContext.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val seen = HashSet<String>()
        val result = ArrayList<AppEntry>()
        try {
            pm.queryIntentActivities(intent, 0).forEach { ri ->
                val info = ri.activityInfo ?: return@forEach
                val pkg = info.packageName ?: return@forEach
                if (pkg == appContext.packageName) return@forEach
                if (!seen.add(pkg)) return@forEach
                result.add(
                    AppEntry(
                        packageName = pkg,
                        label = info.loadLabel(pm).toString(),
                        icon = info.loadIcon(pm),
                    )
                )
            }
        } catch (_: Exception) {
        }
        return result.sortedBy { it.label.lowercase() }
    }

    fun labelFor(pkg: String): String {
        return try {
            val pm = appContext.packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
        } catch (_: Exception) {
            pkg
        }
    }

    fun iconFor(pkg: String): Drawable? {
        return try {
            appContext.packageManager.getApplicationIcon(pkg)
        } catch (_: Exception) {
            null
        }
    }
}
