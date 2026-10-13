package com.openblockit.session

import android.accessibilityservice.AccessibilityService
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Point
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import android.view.inputmethod.InputMethodInfo
import android.view.inputmethod.InputMethodManager
import com.openblockit.core.BagRepo
import com.openblockit.core.Prefs
import com.openblockit.core.SessionManager
import java.lang.ref.WeakReference
import java.util.HashMap
import java.util.HashSet
import java.util.Locale

class BlockAccessibilityService : AccessibilityService() {

    companion object {
        const val TAG = "BlockEnforcer"

        const val EVENT_WINDOWS_CHANGED = 4194304
        const val EVENT_WINDOW_STATE_CHANGED = 32

        const val GLOBAL_BACK = 1
        const val GLOBAL_LOCK_SCREEN = 8
        const val GLOBAL_DISMISS_SHADE = 15

        const val WINDOW_TYPE_APPLICATION = 1
        const val WINDOW_TYPE_INPUT_METHOD = 2

        const val INSPECTION_BUDGET_MS = 600L
        const val SLOW_INSPECTION_MS = 700L
        const val INSPECTION_PAUSE_MS = 3000L
        const val FLOATING_CHECK_INTERVAL_MS = 1200L
        const val SHADE_DISMISS_INTERVAL_MS = 400L
        const val AUTH_CACHE_MS = 1500L
        const val AUTH_CACHE_SOFT_MS = 300L
        const val IME_WINDOW_CACHE_MS = 500L
        const val IME_LIST_CACHE_MS = 60_000L
        const val BAG_GRACE_MS = 15_000L
        const val SYSTEM_APP_FLAG_MASK = 129

        @Volatile
        var instance: BlockAccessibilityService? = null

        @Volatile
        var lastForegroundPackage: String? = null

        fun alive(): Boolean = instance != null

        fun dismissShade() {
            val service = instance ?: return
            try {
                if (Build.VERSION.SDK_INT >= 31) service.performGlobalAction(GLOBAL_DISMISS_SHADE)
            } catch (_: Exception) {
            }
        }
    }

    private val handler = Handler(Looper.getMainLooper())
    private var deadline = 0L
    private var pauseOptionalUntil = 0L
    private var lastShadeActionAt = 0L

    private var floatingCheckAt = 0L
    private var smallWindowCount = 0
    private var smallWindowLastAt = 0L

    private var authCacheAt = 0L
    private var authPromptVisible = false

    private var imeVisibleCacheAt = 0L
    private var imeVisibleCache = false

    private var lastAuthSurfacePackage: String? = null
    private var allowedAppOpenedAt = 0L
    private var lastAllowedSurfacePackage: String? = null
    private var lastFocusAllowedAt = 0L

    private var imePackages: Set<String> = emptySet()
    private var imePackagesAt = 0L

    private var screenSize: Point? = null
    private var settingsPackage: String? = null
    private var uninstallerPackage: String? = null
    private var launcherPackage: String? = null
    private var resolvedDefaults = false

    private val systemAppCache = HashMap<String, Boolean>()

    var currentForegroundPackage: String? = null
        private set

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.i(TAG, "session block service connected")
        if (SessionManager.started() && !LockService.running) {
            SessionManager.startLockService()
        }
    }

    override fun onUnbind(intent: Intent?): Boolean {
        handler.removeCallbacksAndMessages(null)
        instance = null
        lastForegroundPackage = null
        OverlayPill.hide()
        return super.onUnbind(intent)
    }

    override fun onInterrupt() = Unit

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (!SessionManager.started()) {
            if (OverlayPill.isVisible) OverlayPill.hide()
            return
        }
        when (event.eventType) {
            EVENT_WINDOWS_CHANGED -> {
                handleWindowsChanged(event)
            }
            EVENT_WINDOW_STATE_CHANGED -> {
                val pkg = event.packageName?.toString() ?: return
                handleWindowStateChanged(event, pkg)
            }
        }
    }

    private fun handleWindowsChanged(event: AccessibilityEvent) {
        dismissShadeIfSessionActive()
        if (inspectionPaused()) return
        enforceNoFloatingWindows()
    }

    private fun dismissShadeIfSessionActive() {
        try {
            if (!SessionManager.started()) return
            if (isKeyguardLocked()) return
            if (isImeVisible()) return
            val now = SystemClock.uptimeMillis()
            if (now - lastShadeActionAt < SHADE_DISMISS_INTERVAL_MS) return
            if (isAuthPromptShown(soft = true)) return
            lastShadeActionAt = now
            if (Build.VERSION.SDK_INT >= 31) {
                performGlobalAction(GLOBAL_DISMISS_SHADE)
            }
        } catch (e: Exception) {
            Log.w(TAG, "shade enforcement failed", e)
        }
    }

    private fun enforceNoFloatingWindows() {
        val now = SystemClock.uptimeMillis()
        if (now - floatingCheckAt < FLOATING_CHECK_INTERVAL_MS) return
        floatingCheckAt = now
        if (isKeyguardLocked()) return
        if (!SessionManager.started()) return
        beginInspection()
        try {
            val screen = screenSize()
            val minSide = minOf(screen.x, screen.y)
            val maxSide = maxOf(screen.x, screen.y)
            val bounds = Rect()
            var ownSmallBounds: Rect? = null
            var otherAppsOnScreen = 0

            for (window in windows ?: emptyList()) {
                if (window == null || window.type != WINDOW_TYPE_APPLICATION) continue
                if (inspectionExpired()) return
                val root = window.root ?: continue
                val windowPkg = root.packageName?.toString() ?: continue

                if (windowPkg == packageName) {
                    if (isSmallWindow(window)) ownSmallBounds = Rect().also { window.getBoundsInScreen(it) }
                    continue
                }

                if (isOwnOrSystem(windowPkg)) continue
                if (isAllowed(windowPkg) || isKeyboard(windowPkg)) continue
                otherAppsOnScreen++
                if (windowPkg == lastAuthSurfacePackage) continue
                if (isMediaPicker(windowPkg)) continue

                window.getBoundsInScreen(bounds)
                if (!isSmallWindow(window)) continue

                if (focusedAppStillAllowed()) {
                    Log.i(TAG, "floating window of $windowPkg but focus is on an allowed app, treating as panel")
                    continue
                }
                if (isAuthSurface(windowPkg, null)) {
                    lastAuthSurfacePackage = windowPkg
                    continue
                }
                if (isAuthPromptShown(soft = true)) {
                    lastAuthSurfacePackage = windowPkg
                    continue
                }
                if (dismissFloatingWindow(root)) {
                    Log.i(TAG, "disallowed floating window of $windowPkg dismissed")
                    SessionManager.showBlockScreen()
                    continue
                }
                Log.i(TAG, "disallowed floating window of $windowPkg, bouncing and locking screen")
                SessionManager.showBlockScreen()
                SessionManager.lockScreenNow()
            }

            val ownBounds = ownSmallBounds
            if (ownBounds == null || otherAppsOnScreen <= 0) {
                smallWindowCount = 0
            } else {
                smallWindowCount++
                if (smallWindowCount >= 2 && now - smallWindowLastAt > 5000L) {
                    smallWindowLastAt = now
                    smallWindowCount = 0
                    Log.i(TAG, "we are in a small window with other apps on screen, bouncing and locking screen")
                    SessionManager.showBlockScreen()
                    SessionManager.lockScreenNow()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "floating window enforcement failed", e)
        } finally {
            endInspection("floatingWindows")
        }
    }

    private fun handleWindowStateChanged(event: AccessibilityEvent, pkg: String) {
        val className = event.className
        if (isPowerMenuClass(className) || pkg == "com.android.systemui" || pkg == "android") {
            if (enforceNoPowerMenu(className)) return
        }
        if (pkg == "com.android.systemui") {
            enforceShadeClosed(event)
            enforceNoRecents(event)
            return
        }
        if (isKeyboard(pkg)) return

        val ownFloatingWindow = pkg == packageName && windowTypeOf(event) != WINDOW_TYPE_APPLICATION

        if (!ownFloatingWindow && pkg != "android" && (pkg == currentForegroundPackage || isFromActiveWindow(event))) {
            currentForegroundPackage = pkg
            updatePill(pkg)
        }
        if (pkg == packageName && !ownFloatingWindow) {
            lastAllowedSurfacePackage = null
        }
        if (isOwnOrSystem(pkg)) return

        if (isAuthSurface(pkg, className)) {
            lastAuthSurfacePackage = pkg
            return
        }

        if (pkg == "com.google.android.gms" || pkg == "com.google.android.gsf") {
            val cls = className?.toString()?.lowercase(Locale.ROOT) ?: ""
            val authFlow = cls.contains("signin") || cls.contains("sign_in") || cls.contains(".auth.") ||
                cls.contains("accountchooser") || cls.contains("accountpicker") || cls.contains("addaccount") ||
                cls.contains("consent") || cls.contains("credential") || cls.contains("identity")
            if (authFlow && isSystemApp(pkg)) {
                lastAuthSurfacePackage = pkg
                return
            }
        }

        if (isMediaPicker(pkg)) return

        if (isAllowed(pkg)) {
            lastFocusAllowedAt = SystemClock.uptimeMillis()
            if (pkg != lastAllowedSurfacePackage) {
                lastAllowedSurfacePackage = pkg
                allowedAppOpenedAt = SystemClock.uptimeMillis()
            }
            if (pkg == currentForegroundPackage) lastFocusAllowedAt = SystemClock.uptimeMillis()
            return
        }

        if (windowTypeOf(event) == WINDOW_TYPE_INPUT_METHOD) {
            rememberImePackage(pkg)
            return
        }
        if (focusedAppStillAllowed()) {
            postRecheck(900L)
            return
        }
        if (isBagGraceActive(pkg)) {
            Log.i(TAG, "held back: system surface $pkg right after an allowed app")
            return
        }
        if (isAuthPromptShown(soft = true)) {
            lastAuthSurfacePackage = pkg
            return
        }
        if (systemSurfaceOverAllowedApp(pkg)) {
            lastAuthSurfacePackage = pkg
            return
        }
        if (isKeyguardLocked()) return

        Log.i(TAG, "disallowed app in foreground: $pkg -> returning to session block")
        lastAuthSurfacePackage = null
        SessionManager.showBlockScreen()
        postRecheck(700L)
    }

    private fun postRecheck(delay: Long) {
        handler.postDelayed({
            if (SessionManager.started()) SessionManager.showBlockScreen()
        }, delay)
    }

    private fun updatePill(pkg: String) {
        try {
            if (isAllowed(pkg) && !isOwnOrSystem(pkg) && SessionManager.started()) {
                val label = BagRepo.labelFor(pkg)
                OverlayPill.show(label, SessionManager.secondsLeft.value)
            } else {
                OverlayPill.hide()
            }
        } catch (_: Exception) {
        }
    }

    private fun enforceNoPowerMenu(className: CharSequence?): Boolean {
        try {
            if (!SessionManager.started()) return false
            val detected = if (className != null && isPowerMenuClass(className)) true else powerMenuWindowTitle() != null
            if (!detected) return false
            val now = SystemClock.uptimeMillis()
            if (now - lastShadeActionAt >= 120L) {
                lastShadeActionAt = now
                performGlobalAction(GLOBAL_BACK)
                Log.i(TAG, "session active, power menu closed")
            }
            handler.postDelayed({ if (SessionManager.started()) performGlobalAction(GLOBAL_BACK) }, 150L)
            handler.postDelayed({ if (SessionManager.started()) performGlobalAction(GLOBAL_BACK) }, 400L)
            handler.postDelayed({ if (SessionManager.started()) performGlobalAction(GLOBAL_BACK) }, 800L)
            return true
        } catch (e: Exception) {
            Log.w(TAG, "power menu enforcement failed", e)
            return false
        }
    }

    private fun enforceNoRecents(event: AccessibilityEvent) {
        try {
            if (!SessionManager.started()) return
            val className = event.className?.toString()?.lowercase(Locale.ROOT) ?: return
            val recents = className.contains("recent") || className.contains("overview") || className.contains("taskview") ||
                className.contains("taskswitcher") || className.contains("multiwindow") || className.contains("multitask") ||
                className.contains("splitscreen") || className.contains("freeform") || className.contains("smallwindow")
            val window = windowOf(event)
            val smallNonPip = window != null && window.type == WINDOW_TYPE_APPLICATION && !isSmallWindow(window)
            if (!recents && !smallNonPip) return
            if (isAuthClassName(className)) return
            if (isAuthPromptShown(soft = true)) return
            if (className.contains("emergency")) return
            val pkg = event.packageName?.toString() ?: "com.android.systemui"
            if (isEmergencySurface(pkg, event.className)) return
            if (!isFromActiveWindow(event)) return
            if (isImeVisible()) return
            Log.i(TAG, "system ui surface during session (multitasking?), bouncing ($className)")
            SessionManager.showBlockScreen()
        } catch (e: Exception) {
            Log.w(TAG, "recents enforcement failed", e)
        }
    }

    private fun enforceShadeClosed(event: AccessibilityEvent) {
        try {
            if (!SessionManager.started()) return
            val className = event.className?.toString() ?: ""
            val lower = className.lowercase(Locale.ROOT)
            if (isKeyguardLocked()) {
                if (isAuthClassName(lower)) return
                if (lower.contains("fgsmanager") || lower.contains("taskmanager") || lower.contains("runningapp") ||
                    lower.contains("foregroundservice") || lower.contains("dialog")
                ) {
                    performGlobalAction(GLOBAL_BACK)
                }
                return
            }
            if (isImeVisible()) return
            if (Build.VERSION.SDK_INT >= 31) {
                performGlobalAction(GLOBAL_DISMISS_SHADE)
            } else if (!isAuthPromptShown(soft = false)) {
                performGlobalAction(GLOBAL_BACK)
            }
            val authClass = isAuthClassName(lower)
            val foregroundAllowed = currentForegroundPackage?.let { BagRepo.isAllowed(it) } == true
            if (!authClass && isFromActiveWindow(event) && !(foregroundAllowed && !inspectionPaused())) {
                performGlobalAction(GLOBAL_BACK)
                Log.i(TAG, "session active, system ui shade/dialog closed ($className)")
            }
        } catch (e: Exception) {
            Log.w(TAG, "shade enforcement failed", e)
        }
    }

    private fun beginInspection() {
        deadline = SystemClock.uptimeMillis() + INSPECTION_BUDGET_MS
    }

    private fun inspectionExpired(): Boolean = SystemClock.uptimeMillis() > deadline

    private fun inspectionPaused(): Boolean = SystemClock.uptimeMillis() < pauseOptionalUntil

    private fun endInspection(name: String) {
        val elapsed = INSPECTION_BUDGET_MS - (deadline - SystemClock.uptimeMillis())
        if (elapsed > SLOW_INSPECTION_MS) {
            pauseOptionalUntil = SystemClock.uptimeMillis() + INSPECTION_PAUSE_MS
            Log.i(TAG, "slow inspection in $name (${elapsed}ms), pausing optional inspections")
        }
    }

    private fun isAllowed(pkg: String): Boolean = BagRepo.isAllowed(pkg)

    private fun isOwnOrSystem(pkg: String?): Boolean =
        pkg == null || pkg == packageName || pkg == "android" || pkg == "com.android.systemui"

    private fun isSystemApp(pkg: String): Boolean {
        systemAppCache[pkg]?.let { return it }
        if (systemAppCache.size > 128) systemAppCache.clear()
        val value = try {
            (packageManager.getApplicationInfo(pkg, 0).flags and SYSTEM_APP_FLAG_MASK) != 0
        } catch (_: Exception) {
            false
        }
        systemAppCache[pkg] = value
        return value
    }

    private fun isAuthClassName(raw: CharSequence?): Boolean {
        if (raw == null) return false
        val s = raw.toString().lowercase(Locale.ROOT)
        return s.contains("keyguard") || s.contains("bouncer") || s.contains("lockscreen") ||
            s.contains("pin") || s.contains("pattern") || s.contains("password") || s.contains("biometric") ||
            s.contains("fingerprint") || s.contains("face") || s.contains("unlock") || s.contains("credential") ||
            s.contains("puk") || s.contains("knox") || s.contains("secscreen")
    }

    private fun isPowerMenuClass(raw: CharSequence?): Boolean {
        if (raw == null) return false
        val s = raw.toString().lowercase(Locale.ROOT)
        return s.contains("globalaction") || s.contains("actionsdialog") || s.contains("powermenu") ||
            s.contains("power_menu") || s.contains("poweroff") || s.contains("powerdialog") ||
            s.contains("shutdown") || s.contains("rebootdialog")
    }

    private fun isMediaPicker(pkg: String): Boolean {
        val p = pkg.lowercase(Locale.ROOT)
        return p == "com.android.documentsui" || p == "com.google.android.documentsui" ||
            p == "com.google.android.photopicker" || p == "com.android.photopicker" ||
            p == "com.android.providers.media.module" || p == "com.google.android.providers.media.module" ||
            p == "com.android.providers.downloads.ui"
    }

    private fun isSettingsOrInstaller(pkg: String): Boolean {
        if (!resolvedDefaults) {
            resolvedDefaults = true
            try {
                settingsPackage = packageManager
                    .resolveActivity(Intent(Settings.ACTION_SETTINGS), PackageManager.MATCH_DEFAULT_ONLY)
                    ?.activityInfo?.packageName
            } catch (_: Exception) {
            }
            try {
                uninstallerPackage = packageManager
                    .resolveActivity(Intent(Intent.ACTION_DELETE, Uri.parse("package:$packageName")), PackageManager.MATCH_DEFAULT_ONLY)
                    ?.activityInfo?.packageName
            } catch (_: Exception) {
            }
            try {
                launcherPackage = packageManager
                    .resolveActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), PackageManager.MATCH_DEFAULT_ONLY)
                    ?.activityInfo?.packageName
            } catch (_: Exception) {
            }
        }
        if (pkg == settingsPackage || pkg == uninstallerPackage || pkg == launcherPackage) return true
        val lower = pkg.lowercase(Locale.ROOT)
        return lower.contains("settings") || lower.contains("installer") ||
            pkg == "com.android.vending" || pkg == "com.miui.securitycenter" ||
            pkg == "com.samsung.android.lool" || pkg == "com.samsung.android.sm_cn"
    }

    private fun isKeyboard(pkg: String): Boolean {
        val now = SystemClock.uptimeMillis()
        if (imePackages.isEmpty() || now - imePackagesAt > IME_LIST_CACHE_MS) {
            imePackagesAt = now
            val set = HashSet<String>()
            try {
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                imm?.enabledInputMethodList?.forEach { info: InputMethodInfo ->
                    info.packageName?.let { set.add(it) }
                }
                imm?.inputMethodList?.forEach { info: InputMethodInfo ->
                    info.packageName?.let { set.add(it) }
                }
                val defaultIme = Settings.Secure.getString(contentResolver, "default_input_method")
                if (!defaultIme.isNullOrEmpty()) {
                    val slash = defaultIme.indexOf('/')
                    set.add(if (slash > 0) defaultIme.substring(0, slash) else defaultIme)
                }
            } catch (_: Exception) {
            }
            set.remove("com.android.systemui")
            set.remove("android")
            set.remove(packageName)
            if (set.isNotEmpty()) imePackages = set
        }
        return imePackages.contains(pkg)
    }

    private fun rememberImePackage(pkg: String) {
        if (pkg == "com.android.systemui" || pkg == "android" || pkg == packageName) return
        if (imePackages.contains(pkg)) return
        imePackages = HashSet(imePackages).also { it.add(pkg) }
        Log.i(TAG, "keyboard recognised from IME window: $pkg")
    }

    private fun isImeVisible(): Boolean {
        val now = SystemClock.uptimeMillis()
        if (now - imeVisibleCacheAt < IME_WINDOW_CACHE_MS) return imeVisibleCache
        imeVisibleCacheAt = now
        var visible = false
        try {
            for (window in windows ?: emptyList()) {
                if (window != null && window.type == WINDOW_TYPE_INPUT_METHOD) {
                    visible = true
                    break
                }
            }
        } catch (_: Exception) {
        }
        imeVisibleCache = visible
        return visible
    }

    private fun isKeyguardLocked(): Boolean {
        val km = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager ?: return false
        return km.isKeyguardLocked
    }

    private fun isAuthPromptShown(soft: Boolean): Boolean {
        val now = SystemClock.uptimeMillis()
        val cacheMs = if (soft) AUTH_CACHE_SOFT_MS else AUTH_CACHE_MS
        if (now - authCacheAt < cacheMs) return authPromptVisible
        authCacheAt = now
        beginInspection()
        var found = false
        try {
            for (window in windows ?: emptyList()) {
                if (window == null) continue
                if (!window.isFocused && !window.isActive) continue
                val root = window.root ?: continue
                if (matchesAuthNode(root, 0)) {
                    found = true
                    break
                }
            }
        } catch (_: Exception) {
        } finally {
            endInspection("authPrompt")
        }
        authPromptVisible = found
        return found
    }

    private fun matchesAuthNode(node: AccessibilityNodeInfo?, depth: Int): Boolean {
        if (node == null || depth > 6) return false
        try {
            val cls = node.className?.toString()?.lowercase(Locale.ROOT) ?: ""
            val id = node.viewIdResourceName?.lowercase(Locale.ROOT) ?: ""
            if (node.isPassword) return true
            if (cls.contains("edittext") && (id.contains("password") || id.contains("pin") || id.contains("pattern"))) return true
            if (id.contains("password") || id.contains("credential") || id.contains("bouncer") ||
                id.contains("lockscreen") || id.contains("keyguard") || id.contains("pattern")
            ) {
                return true
            }
            for (i in 0 until node.childCount) {
                if (matchesAuthNode(node.getChild(i), depth + 1)) return true
            }
        } catch (_: Exception) {
        }
        return false
    }

    private fun focusedAppStillAllowed(): Boolean {
        if (inspectionPaused()) return false
        beginInspection()
        try {
            for (window in windows ?: emptyList()) {
                if (window == null || window.type != WINDOW_TYPE_APPLICATION) continue
                if (!window.isFocused && !window.isActive) continue
                if (inspectionExpired()) return false
                val root = window.root ?: return false
                val pkg = root.packageName?.toString() ?: return false
                if (pkg == packageName) return false
                val allowed = isAllowed(pkg)
                if (allowed) lastFocusAllowedAt = SystemClock.uptimeMillis()
                return allowed || pkg == lastAuthSurfacePackage
            }
            return false
        } catch (_: Exception) {
            return false
        } finally {
            endInspection("focusedApp")
        }
    }

    private fun isBagGraceActive(pkg: String): Boolean {
        val since = lastFocusAllowedAt
        if (since == 0L) return false
        if (SystemClock.uptimeMillis() - since > BAG_GRACE_MS) return false
        if (!isSystemApp(pkg)) return false
        return !isSettingsOrInstaller(pkg)
    }

    private fun isChooserOrClone(pkg: String, className: CharSequence?): Boolean {
        val cls = className?.toString()?.lowercase(Locale.ROOT) ?: ""
        val p = pkg.lowercase(Locale.ROOT)
        val chooser = cls.contains("resolver") || cls.contains("chooser") || cls.contains("xspace") ||
            cls.contains("dualapp") || cls.contains("dual_app")
        val resolverPkg = p == "com.android.intentresolver" || p.contains("securitycore")
        return (chooser || resolverPkg) && isSystemApp(pkg)
    }

    private fun isAuthSurface(pkg: String, className: CharSequence?): Boolean {
        val cls = className?.toString()?.lowercase(Locale.ROOT) ?: ""
        val p = pkg.lowercase(Locale.ROOT)
        val authClass = cls.contains("confirmdevicecredential") || cls.contains("confirmlock") ||
            cls.contains("biometric") || cls.contains("credential") || cls.contains("fingerprint") ||
            cls.contains("iris") || cls.contains("faceunlock") || cls.contains("keyguard")
        val samsung = p.contains("knox") || p.contains("samsung.android.bio") || p.contains("samsung.android.security") ||
            p.contains("samsung.android.lool") ||
            (p.contains("samsung") && (cls.contains("lock") || cls.contains("auth") || cls.contains("verify") || cls.contains("secur")))
        val appLock = cls.contains("applock") || cls.contains("applocker") || cls.contains("applicationlock") ||
            cls.contains("accesscontrol") || p.contains("applock") || p.contains("applocker")
        return (authClass || samsung || appLock) && isSystemApp(pkg)
    }

    private fun isEmergencySurface(pkg: String, className: CharSequence?): Boolean {
        val cls = className?.toString()?.lowercase(Locale.ROOT) ?: ""
        return (cls.contains("emergency") || pkg.lowercase(Locale.ROOT).contains("emergency")) && isSystemApp(pkg)
    }

    private fun windowOf(event: AccessibilityEvent): AccessibilityWindowInfo? {
        try {
            val windowId = event.windowId
            if (windowId == -1) return null
            for (window in windows ?: emptyList()) {
                if (window != null && window.id == windowId) return window
            }
        } catch (_: Exception) {
        }
        return null
    }

    private fun windowTypeOf(event: AccessibilityEvent): Int {
        val window = windowOf(event) ?: return -1
        return window.type
    }

    private fun isFromActiveWindow(event: AccessibilityEvent): Boolean {
        val window = windowOf(event) ?: return false
        return window.isActive || window.isFocused
    }

    private fun isSmallWindow(window: AccessibilityWindowInfo): Boolean {
        try {
            if (window.isInPictureInPictureMode) return true
            val screen = screenSize()
            val minSide = minOf(screen.x, screen.y)
            val maxSide = maxOf(screen.x, screen.y)
            val bounds = Rect()
            window.getBoundsInScreen(bounds)
            return bounds.width().coerceAtMost(bounds.height()) < minSide * 0.85f ||
                bounds.width().coerceAtLeast(bounds.height()) < maxSide * 0.6f
        } catch (_: Exception) {
            return false
        }
    }

    private fun screenSize(): Point {
        screenSize?.let { return it }
        var point: Point? = null
        try {
            if (Build.VERSION.SDK_INT >= 30) {
                val wm = getSystemService(Context.WINDOW_SERVICE) as? WindowManager
                val bounds = wm?.maximumWindowMetrics?.bounds
                if (bounds != null && bounds.width() > 0 && bounds.height() > 0) {
                    point = Point(bounds.width(), bounds.height())
                }
            }
        } catch (_: Exception) {
        }
        if (point == null) {
            val dm = resources.displayMetrics
            point = Point(dm.widthPixels, dm.heightPixels)
        }
        screenSize = point
        return point
    }

    private fun systemSurfaceOverAllowedApp(pkg: String): Boolean {
        if (!isSystemApp(pkg)) return false
        beginInspection()
        try {
            for (window in windows ?: emptyList()) {
                if (window == null || window.type != WINDOW_TYPE_APPLICATION) continue
                if (inspectionExpired()) return false
                val root = window.root ?: continue
                val windowPkg = root.packageName?.toString() ?: continue
                if (windowPkg != pkg && windowPkg != packageName && isAllowed(windowPkg)) return true
            }
            return false
        } catch (_: Exception) {
            return false
        } finally {
            endInspection("systemSurfaceOverAllowedApp")
        }
    }

    private fun powerMenuWindowTitle(): String? {
        return try {
            for (window in windows ?: emptyList()) {
                if (window == null || window.type == WINDOW_TYPE_APPLICATION) continue
                val title = window.title?.toString() ?: continue
                if (isPowerMenuClass(title)) return title
            }
            null
        } catch (_: Exception) {
            null
        }
    }

    private fun dismissFloatingWindow(node: AccessibilityNodeInfo): Boolean {
        return try {
            if (node.actionList.contains(AccessibilityNodeInfo.AccessibilityAction.ACTION_DISMISS) &&
                node.performAction(AccessibilityNodeInfo.ACTION_DISMISS)
            ) {
                return true
            }
            val queue = ArrayDeque<AccessibilityNodeInfo>()
            queue.add(node)
            var visited = 0
            while (queue.isNotEmpty() && visited < 30) {
                val current = queue.removeFirst() ?: continue
                visited++
                if (current !== node &&
                    current.actionList.contains(AccessibilityNodeInfo.AccessibilityAction.ACTION_DISMISS) &&
                    current.performAction(AccessibilityNodeInfo.ACTION_DISMISS)
                ) {
                    return true
                }
                for (i in 0 until current.childCount) {
                    current.getChild(i)?.let { queue.add(it) }
                }
            }
            false
        } catch (_: Exception) {
            false
        }
    }
}
