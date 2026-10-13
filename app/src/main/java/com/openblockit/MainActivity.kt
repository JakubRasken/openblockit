package com.openblockit

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.view.animation.PathInterpolator
import android.widget.BaseAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.openblockit.core.BagRepo
import com.openblockit.core.Clock
import com.openblockit.core.Prefs
import com.openblockit.core.Routine
import com.openblockit.core.SessionManager
import com.openblockit.core.StartResult
import com.openblockit.core.StatsRepo
import com.openblockit.core.TowerRepo
import com.openblockit.core.endTimeLabel
import com.openblockit.core.formatDurationMinutes
import com.openblockit.session.RoutineScheduler
import it.mirko.widget.HapticBuyTile
import it.mirko.widget.BagTiles
import it.mirko.widget.DayFaders
import it.mirko.widget.HapticPreset
import it.mirko.widget.HapticSlider
import it.mirko.widget.PowerTiles
import it.mirko.widget.RombiKnob
import it.mirko.widget.RotaryWheel
import it.mirko.widget.SledSwitch
import it.mirko.widget.SlideToStart
import it.mirko.widget.SquareContainer
import it.mirko.widget.SwipeSections
import it.mirko.widget.TimelineBar
import it.mirko.widget.display.TileDisplay
import it.mirko.widget.navigation.AltNavigationBar
import it.mirko.widget.numbers.SquareNumber
import it.mirko.widget.stats.StatisticsGraph
import kotlinx.coroutines.launch

open class MainActivity : AppCompatActivity() {

    private lateinit var root: SwipeSections
    private lateinit var nav: AltNavigationBar
    private lateinit var navContainer: View
    private lateinit var slide: SlideToStart
    private lateinit var wheel: HapticSlider
    private lateinit var hourLeft: SquareNumber
    private lateinit var hourRight: SquareNumber
    private lateinit var minuteLeft: SquareNumber
    private lateinit var minuteRight: SquareNumber
    private lateinit var separator: SquareNumber
    private lateinit var customValue: TextView
    private lateinit var sessionMiddle: View
    private lateinit var blockMiddle: View
    private lateinit var blockValigia: View
    private lateinit var extendBox: View

    private val handler = Handler(Looper.getMainLooper())
    private var selectedMinutes = 30
    private var statsMode = 1
    private var statsOffset = 0
    private var statsSelection = -1
    private var blockMode = false
    private var streakPulse: android.animation.ValueAnimator? = null
    private var extendedMinutes = 0

    private val standard = PathInterpolator(0.2f, 0f, 0f, 1f)
    private val emphasized = PathInterpolator(0.05f, 0.7f, 0.1f, 1f)
    private val accelerate = PathInterpolator(0.3f, 0f, 0.8f, 0.15f)

    private val ticker = object : Runnable {
        override fun run() {
            if (!blockMode) return
            updateBlockTiles()
            handler.postDelayed(this, 1000L)
        }
    }

    private val extendHoldAnimator by lazy { ValueAnimator.ofFloat(0f, 1f) }

    private val notificationPermission =
        registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!Prefs.onboardingDone) {
            startActivity(Intent(this, OnboardingActivity::class.java))
            finish()
            return
        }
        requestNotificationPermission()
        setContentView(R.layout._activity_home)
        selectedMinutes = Prefs.lastMinutes.coerceIn(1, 720)
        extendedMinutes = Prefs.main.getInt("extended_minutes", 0)
        hidePremium()
        bindToolbar()
        bindSession()
        bindNav()
        bindControl()
        bindBag()
        bindStats()
        applySessionMetrics()
        updateTimer(selectedMinutes)
        renderCustom()
        if (SessionManager.started()) enterBlockMode(animate = false)
        if (intent?.getBooleanExtra("completed", false) == true) showCompletion()
        maybeAskForBatteryExemption()
        lifecycleScope.launch {
            SessionManager.active.collect { active ->
                if (active && !blockMode) {
                    enterBlockMode(animate = false)
                } else if (!active && blockMode) {
                    if (hostsBlockScreen()) finish() else exitBlockMode(animate = false)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.getBooleanExtra("completed", false)) showCompletion()
    }

    override fun onResume() {
        super.onResume()
        if (SessionManager.started() && !blockMode) enterBlockMode(animate = false)
        if (!SessionManager.started() && blockMode) exitBlockMode(animate = false)
        bindControl()
        bindBag()
    }

    private fun hidePremium() {
        listOf(
            R.id.controlBuyX1,
            R.id.controlBuyX3,
            R.id.controlBuyX10,
            R.id.controlChuteBadge,
            R.id.starLock,
            R.id.buyParachute,
            R.id.sliderWarningBuy,
            R.id.confirmIconP,
        ).forEach { id ->
            findViewById<View?>(id)?.visibility = View.GONE
        }
        (findViewById<View?>(R.id.homeFrame)?.layoutParams as? android.widget.FrameLayout.LayoutParams)?.let {
            it.topMargin = dp(12)
            findViewById<View>(R.id.homeFrame).layoutParams = it
        }
        findViewById<View>(R.id.homeRoot).fitsSystemWindows = true
    }

    private fun bindToolbar() {
        findViewById<View>(R.id.streakBadge).visibility = View.VISIBLE
        findViewById<TextView>(R.id.toolbarChuteCount).text = Prefs.parachuteCount.toString()
        findViewById<View>(R.id.toolbarChutePill).setOnClickListener { selectSection(3) }
        lifecycleScope.launch {
            val days = runCatching { StatsRepo.allDays() }.getOrDefault(emptyList())
            val streak = TowerRepo.currentStreak(days)
            val earned = runCatching { StatsRepo.sessionsOn(TodayKey()) }.getOrDefault(0) > 0
            findViewById<TextView>(R.id.streakCount).text = streak.toString()
            styleStreakTile(earned, streak)
        }
    }

    private fun styleStreakTile(earned: Boolean, streak: Int) {
        val tile = findViewById<View>(R.id.streakTile)
        val active = getColor(R.color.colorPrimaryContainer)
        val idle = getColor(R.color.colorOnSurfaceVariant)
        tile.alpha = if (earned) 1f else 0.55f
        tile.backgroundTintList = android.content.res.ColorStateList.valueOf(if (earned) active else idle)
        val evening = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY) >= 19
        streakPulse?.cancel()
        streakPulse = null
        if (!earned && streak > 0 && evening) {
            val evaluator = android.animation.ArgbEvaluator()
            streakPulse = android.animation.ValueAnimator.ofFloat(0f, 1f, 0f).apply {
                duration = 1800L
                repeatCount = android.animation.ValueAnimator.INFINITE
                addUpdateListener { anim ->
                    val fraction = anim.animatedValue as Float
                    tile.backgroundTintList = android.content.res.ColorStateList.valueOf(
                        evaluator.evaluate(fraction, idle, active) as Int,
                    )
                }
                start()
            }
        }
    }

    override fun onDestroy() {
        handler.removeCallbacks(ticker)
        streakPulse?.cancel()
        streakPulse = null
        super.onDestroy()
    }

    private fun TodayKey(): String {
        return java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
    }

    private fun bindSession() {
        hourLeft = findViewById(R.id.hourLeft)
        hourRight = findViewById(R.id.hourRight)
        minuteLeft = findViewById(R.id.minuteLeft)
        minuteRight = findViewById(R.id.minuteRight)
        separator = findViewById(R.id.timerSeparator)
        separator.setColon()
        val digitColor = getColor(R.color.colorOnSurface)
        listOf(hourLeft, hourRight, minuteLeft, minuteRight, separator).forEach { it.setTextColor(digitColor) }
        customValue = findViewById(R.id.starValue)
        wheel = findViewById(R.id.wheel)
        sessionMiddle = findViewById(R.id.sessionMiddle)
        blockMiddle = findViewById(R.id.blockMiddle)
        blockValigia = findViewById(R.id.blockValigia)
        extendBox = findViewById(R.id.extendBox)

        wheel.setLimitMin(1)
        wheel.setLimitMax(720)
        wheel.setMinutes(selectedMinutes)
        wheel.onMinutesChanged = { minutes ->
            selectedMinutes = minutes
            Prefs.lastMinutes = minutes
            updateTimer(minutes)
            renderCustom()
            accentCustomCard()
        }
        wheel.onTimePicked = {
            Prefs.lastMinutes = wheel.minutesCount
        }

        findViewById<HapticPreset>(R.id.preset_fifteen_minutes).apply {
            setMinutes(15)
            setOnClickListener { exitCustomMode(); animateWheelTo(15) }
        }
        findViewById<HapticPreset>(R.id.preset_thirty_minutes).apply {
            setMinutes(45)
            setOnClickListener { exitCustomMode(); animateWheelTo(45) }
        }
        findViewById<HapticPreset>(R.id.preset_one_hour).apply {
            setMinutes(90)
            setOnClickListener { exitCustomMode(); animateWheelTo(90) }
        }
        findViewById<HapticPreset>(R.id.preset_two_hours).apply {
            setMinutes(120)
            setOnClickListener { exitCustomMode(); animateWheelTo(120) }
        }
        findViewById<HapticPreset>(R.id.preset_three_hours).apply {
            setMinutes(180)
            setOnClickListener { exitCustomMode(); animateWheelTo(180) }
        }
        findViewById<View>(R.id.preset_custom).setOnClickListener {
            enterCustomMode()
        }

        findViewById<View>(R.id.sliderUp).setOnClickListener {
            wheel.setMinutesAnimate((wheel.minutesCount + 5).coerceAtMost(720))
        }
        findViewById<View>(R.id.sliderDown).setOnClickListener {
            wheel.setMinutesAnimate((wheel.minutesCount - 5).coerceAtLeast(1))
        }

        slide = findViewById(R.id.slideToStart)
        slide.setHintFadeFast(true)
        slide.reset()
        slide.setOnSlideCompleteListener { startSession() }
        slide.setParachuteDeniedListener {
            Toast.makeText(this, R.string.parachute_required_long, Toast.LENGTH_LONG).show()
        }

        findViewById<View>(R.id.fab).setOnClickListener { startSession() }
        (findViewById<View?>(R.id.fab) as? com.google.android.material.floatingactionbutton.FloatingActionButton)?.let { fab ->
            fab.backgroundTintList = android.content.res.ColorStateList.valueOf(getColor(R.color.colorPrimary))
            fab.imageTintList = android.content.res.ColorStateList.valueOf(getColor(R.color.colorOnPrimary))
            (fab.parent as? View)?.visibility = View.GONE
        }
        findViewById<View>(R.id.parachuteBox).setOnClickListener { selectSection(3) }
        bindExtendHold()
        bindSoundBox()
        bindValigia()
    }

    private fun bindExtendHold() {
        val segIds = listOf(R.id.extendSeg0, R.id.extendSeg1, R.id.extendSeg2, R.id.extendSeg3)
        val normal = getColor(R.color.colorSurfaceBright)
        val active = getColor(R.color.colorPrimaryContainer)

        fun renderSegments() {
            val lit = extendedMinutes / 15
            segIds.forEachIndexed { index, id ->
                findViewById<View?>(id)?.backgroundTintList =
                    android.content.res.ColorStateList.valueOf(if (index < lit) active else normal)
            }
        }
        renderSegments()

        extendHoldAnimator.apply {
            duration = 650L
            interpolator = LinearInterpolator()
            removeAllUpdateListeners()
            addUpdateListener { animator ->
                val fraction = animator.animatedValue as Float
                if (extendBox is com.google.android.material.card.MaterialCardView) {
                    (extendBox as com.google.android.material.card.MaterialCardView).setCardBackgroundColor(
                        ArgbEvaluator().evaluate(fraction, getColor(R.color.block_tile), getColor(R.color.colorPrimaryContainer)) as Int
                    )
                }
            }
        }
        extendBox.setOnTouchListener { _, event ->
            if (!blockMode) return@setOnTouchListener false
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    if (extendedMinutes >= 60) {
                        Toast.makeText(this, R.string.countdown_extend_need_parachute, Toast.LENGTH_SHORT).show()
                        return@setOnTouchListener true
                    }
                    extendHoldAnimator.start()
                    return@setOnTouchListener true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (extendHoldAnimator.isRunning) {
                        val fraction = extendHoldAnimator.animatedValue as Float
                        extendHoldAnimator.cancel()
                        if (fraction >= 0.95f) {
                            SessionManager.extend(15)
                            extendedMinutes += 15
                            Prefs.main.edit().putInt("extended_minutes", extendedMinutes).apply()
                            renderSegments()
                            updateBlockTiles()
                        }
                    }
                    if (extendBox is com.google.android.material.card.MaterialCardView) {
                        (extendBox as com.google.android.material.card.MaterialCardView).setCardBackgroundColor(getColor(R.color.block_tile))
                    }
                    return@setOnTouchListener true
                }
            }
            false
        }
    }

    private fun bindSoundBox() {
        findViewById<View>(R.id.soundBox).setOnClickListener {
            val next = (Prefs.soundMode + 1) % 5
            Prefs.soundMode = next
            com.openblockit.core.Sounds.start(this, next)
            val host = findViewById<View>(R.id.soundEqHost)
            host.animate().cancel()
            host.translationX = dp(-4).toFloat()
            host.animate().translationX(0f).setDuration(120L).setInterpolator(standard).start()
        }
    }

    private fun bindValigia() {
        val cellIds = listOf(R.id.valigiaCell0, R.id.valigiaCell1, R.id.valigiaCell2, R.id.valigiaCell3, R.id.valigiaCell4, R.id.valigiaCell5)
        val iconIds = listOf(R.id.valigiaIcon0, R.id.valigiaIcon1, R.id.valigiaIcon2, R.id.valigiaIcon3, R.id.valigiaIcon4, R.id.valigiaIcon5)
        val apps = BagRepo.appsIn(Prefs.activeBag)
        cellIds.forEachIndexed { index, cellId ->
            val cell = findViewById<View>(cellId)
            val icon = findViewById<ImageView>(iconIds[index])
            val pkg = apps.getOrNull(index)
            if (pkg == null) {
                cell.visibility = View.GONE
            } else {
                cell.visibility = View.VISIBLE
                icon.setImageDrawable(BagRepo.iconFor(pkg))
                cell.setOnClickListener {
                    try {
                        val i = packageManager.getLaunchIntentForPackage(pkg)
                        if (i != null) startActivity(i)
                    } catch (_: Exception) {
                    }
                }
            }
        }
        findViewById<View>(R.id.valigiaPhone).setOnClickListener {
            try {
                startActivity(Intent(Intent.ACTION_DIAL, android.net.Uri.parse("tel:")))
            } catch (_: Exception) {
            }
        }
    }

    private fun bindNav() {
        root = findViewById(R.id.homeRoot)
        nav = findViewById(R.id.altNavigation)
        navContainer = findViewById(R.id.altNavBarContainer)
        root.attachSections(
            listOf(
                findViewById(R.id.sessionFrame),
                findViewById(R.id.statsFrame),
                findViewById(R.id.controlFrame),
                findViewById(R.id.bagFrame),
            )
        )
        root.onSectionChanged = { sectionIndex -> nav.setSelectedItemAnim(navFor(sectionIndex)) }
        nav.onItemSelected = { navIndex -> root.setCurrentSection(sectionFor(navIndex), true) }
        nav.setSelectedItem(0)
    }

    private fun sectionFor(navIndex: Int): Int = when (navIndex) {
        0 -> 0
        1 -> 3
        2 -> 2
        else -> 1
    }

    private fun navFor(sectionIndex: Int): Int = when (sectionIndex) {
        0 -> 0
        3 -> 1
        2 -> 2
        else -> 3
    }

    private fun selectSection(navIndex: Int) {
        root.setCurrentSection(sectionFor(navIndex), true)
        nav.setSelectedItemAnim(navIndex)
    }

    private fun enterCustomMode() {
        wheel.setLimitMin(180)
        val custom = Prefs.customMinutes.coerceAtLeast(180)
        wheel.setMinutesAnimate(custom)
        findViewById<View>(R.id.preset_custom).alpha = 1f
    }

    private fun exitCustomMode() {
        wheel.setLimitMin(1)
        findViewById<View>(R.id.preset_custom).alpha = 0.5f
    }

    private fun animateWheelTo(minutes: Int) {
        wheel.setMinutesAnimate(minutes)
        Prefs.lastMinutes = minutes
    }

    private fun updateTimer(minutes: Int) {
        val hours = (minutes / 60).coerceAtMost(99)
        hourLeft.setValue(hours / 10)
        hourRight.setValue(hours % 10)
        minuteLeft.setValue((minutes % 60) / 10)
        minuteRight.setValue(minutes % 10)
    }

    private fun renderCustom() {
        customValue.text = "%02d:%02d".format(selectedMinutes / 60, selectedMinutes % 60)
    }

    private fun accentCustomCard() {
        val card = findViewById<View?>(R.id.preset_custom) ?: return
        if (card !is com.google.android.material.card.MaterialCardView) return
        val value = selectedMinutes
        val progress = if (value <= 180) 1f else 0f
        card.setCardBackgroundColor(
            ArgbEvaluator().evaluate(
                progress,
                getColor(R.color.colorSurfaceVariant),
                getColor(R.color.colorPrimaryContainer),
            ) as Int
        )
    }

    private fun applySessionMetrics() {
        val glass = findViewById<View>(R.id.timerGlass)
        glass.addOnLayoutChangeListener { view, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
            if (right - left != oldRight - oldLeft || bottom - top != oldBottom - oldTop) {
                adjustTimerSize(view)
            }
        }
        glass.post { adjustTimerSize(glass) }
        findViewById<View>(R.id.appBarLayout)?.setPadding(0, 0, 0, 0)
        (sessionMiddle as? SquareContainer)?.setRatio(0.86f)
        val screen = findViewById<View>(R.id.screen)
        (screen.layoutParams as? android.widget.LinearLayout.LayoutParams)?.let {
            it.weight = 0f
            it.height = dp(200)
            screen.layoutParams = it
        }
        findViewById<View?>(R.id.homeFrame)?.setPadding(0, dp(48), 0, dp(128))
        findViewById<View?>(R.id.sharableContent)?.let { content ->
            (content.layoutParams as? android.widget.LinearLayout.LayoutParams)?.let { params ->
                params.weight = 0f
                params.height = dp(148)
                content.layoutParams = params
            }
        }
        findViewById<View?>(R.id.statsPeriodOut)?.let { caption ->
            (caption.layoutParams as? android.widget.FrameLayout.LayoutParams)?.let { params ->
                params.height = dp(26)
                params.topMargin = dp(6)
                caption.layoutParams = params
            }
            caption.visibility = View.VISIBLE
            (caption.parent as? View)?.let { frame ->
                (frame.layoutParams as? android.widget.LinearLayout.LayoutParams)?.let { params ->
                    params.weight = 0f
                    params.height = dp(34)
                    frame.layoutParams = params
                }
            }
        }
        (findViewById<View?>(R.id.statsSquare) as? SquareContainer)?.setRatio(0.80f)
    }

    private fun adjustTimerSize(glass: View) {
        val density = resources.displayMetrics.density
        val byWidth = (glass.width - 16f * density - 12f * density) / 20f
        val byHeight = (glass.height - 8f * density) / 5f
        val cellPx = minOf(byWidth, byHeight)
        val multiplier = (cellPx / density).toInt().coerceIn(4, 20)
        listOf(hourLeft, hourRight, minuteLeft, minuteRight, separator).forEach { it.setSizeMultiplier(multiplier) }
        val spaceWidth = (multiplier * density).toInt()
        (findViewById<View>(R.id.spaceHour).layoutParams).apply { width = spaceWidth }
        (findViewById<View>(R.id.spaceMinutes).layoutParams).apply { width = spaceWidth }
        findViewById<SquareNumber>(R.id.secLeft).setSizeMultiplier(10)
        findViewById<SquareNumber>(R.id.secRight).setSizeMultiplier(10)
    }

    private fun enforcementLive(): Boolean = com.openblockit.core.AccessibilityUtils.enabled(this)

    private fun startSession() {
        if (!enforcementLive()) {
            com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle(R.string.accessibility_stuck_title)
                .setMessage(R.string.accessibility_stuck_message)
                .setPositiveButton(R.string.accessibility_stuck_open) { _, _ ->
                    com.openblockit.core.Permissions.openAccessibility(this)
                }
                .setNegativeButton(R.string.accessibility_stuck_start_anyway) { _, _ -> startSessionNow() }
                .show()
            return
        }
        startSessionNow()
    }

    private fun startSessionNow() {
        when (SessionManager.start(selectedMinutes)) {
            StartResult.Started -> enterBlockMode(animate = true)
            StartResult.AlreadyRunning -> enterBlockMode(animate = false)
            StartResult.NeedsParachute ->
                Toast.makeText(this, R.string.parachute_required_long, Toast.LENGTH_LONG).show()
        }
    }

    private fun enterBlockMode(animate: Boolean) {
        if (blockMode) {
            updateBlockTiles()
            return
        }
        blockMode = true
        extendedMinutes = Prefs.main.getInt("extended_minutes", 0)
        val width = resources.displayMetrics.widthPixels.toFloat()

        if (animate) {
            sessionMiddle.animate().translationX(width).setDuration(340L)
                .setInterpolator(standard).withEndAction {
                    sessionMiddle.visibility = View.GONE
                    sessionMiddle.translationX = 0f
                }.start()
            blockMiddle.visibility = View.VISIBLE
            blockMiddle.translationX = -width
            blockMiddle.animate().translationX(0f).setDuration(340L)
                .setInterpolator(standard).start()
            navContainer.animate().translationY(dp(120).toFloat()).setDuration(280L)
                .setInterpolator(accelerate).start()
            blockValigia.visibility = View.VISIBLE
            blockValigia.translationY = dp(400).toFloat()
            blockValigia.animate().translationY(0f).setDuration(280L)
                .setInterpolator(accelerate).start()
        } else {
            sessionMiddle.visibility = View.GONE
            blockMiddle.visibility = View.VISIBLE
            blockMiddle.translationX = 0f
            navContainer.translationY = dp(120).toFloat()
            blockValigia.visibility = View.VISIBLE
            blockValigia.translationY = 0f
        }
        slide.armExit()
        slide.setOnSlideCompleteListener { confirmParachute() }
        (findViewById<View?>(R.id.fab)?.parent as? View)?.visibility = View.VISIBLE
        updateBlockTiles()
        handler.removeCallbacks(ticker)
        handler.post(ticker)
    }

    private fun exitBlockMode(animate: Boolean) {
        if (!blockMode) return
        blockMode = false
        handler.removeCallbacks(ticker)
        val width = resources.displayMetrics.widthPixels.toFloat()

        if (animate) {
            blockMiddle.animate().translationX(-width).setDuration(340L)
                .setInterpolator(standard).withEndAction {
                    blockMiddle.visibility = View.GONE
                    blockMiddle.translationX = 0f
                }.start()
            sessionMiddle.visibility = View.VISIBLE
            sessionMiddle.translationX = width
            sessionMiddle.animate().translationX(0f).setDuration(340L)
                .setInterpolator(standard).start()
            navContainer.animate().translationY(0f).setStartDelay(340L)
                .setDuration(300L).setInterpolator(emphasized).start()
            blockValigia.animate().translationY(dp(400).toFloat()).setDuration(280L)
                .setInterpolator(accelerate).withEndAction {
                    blockValigia.visibility = View.GONE
                    blockValigia.translationY = 0f
                }.start()
        } else {
            blockMiddle.visibility = View.GONE
            sessionMiddle.visibility = View.VISIBLE
            sessionMiddle.translationX = 0f
            navContainer.translationY = 0f
            blockValigia.visibility = View.GONE
        }
        slide.disarm()
        slide.reset()
        slide.setHint(R.string.slide_to_start, R.drawable.ic_play)
        slide.setOnSlideCompleteListener { startSession() }
        (findViewById<View?>(R.id.fab)?.parent as? View)?.visibility = View.GONE
        findViewById<TextView>(R.id.toolbarChuteCount).text = Prefs.parachuteCount.toString()
    }

    private fun updateBlockTiles() {
        val secondsLeft = SessionManager.secondsRemaining()
        if (secondsLeft <= 0) {
            if (blockMode) exitBlockMode(animate = true)
            return
        }
        findViewById<SquareNumber>(R.id.secLeft).setValue((secondsLeft % 60) / 10)
        findViewById<SquareNumber>(R.id.secRight).setValue(secondsLeft % 10)
        findViewById<TextView>(R.id.endTimeValue).text =
            endTimeLabel(System.currentTimeMillis() + secondsLeft * 1000L)
        val parachutes = Prefs.parachuteCount
        findViewById<TextView>(R.id.blockParachuteCount).apply {
            text = parachutes.toString()
            alpha = if (parachutes > 0) 1f else 0.4f
        }
        (findViewById<View?>(R.id.parachuteBox) as? com.google.android.material.card.MaterialCardView)?.setCardBackgroundColor(
            if (parachutes > 0) getColor(R.color.colorPrimaryContainer) else getColor(R.color.block_tile)
        )
        (findViewById<View?>(R.id.parachuteToken) as? com.google.android.material.card.MaterialCardView)?.alpha =
            if (parachutes > 0) 1f else 0.4f
    }

    private fun bindControl() {
        val selected = Prefs.activeRoutine
        for (i in 0..2) {
            val routine = Prefs.routine(i)
            val tab = findViewById<View>(
                when (i) {
                    0 -> R.id.routineTab0
                    1 -> R.id.routineTab1
                    else -> R.id.routineTab2
                }
            )
            tab.alpha = if (i == selected) 1f else 0.55f
            tab.setOnClickListener {
                Prefs.activeRoutine = i
                bindControl()
            }
            findViewById<TextView>(
                when (i) {
                    0 -> R.id.routineTabLabel0
                    1 -> R.id.routineTabLabel1
                    else -> R.id.routineTabLabel2
                }
            ).text = "ROUTINE ${i + 1}"
            findViewById<TextView>(
                when (i) {
                    0 -> R.id.routineTabTime0
                    1 -> R.id.routineTabTime1
                    else -> R.id.routineTabTime2
                }
            ).text = "%02d:%02d".format(routine.hour, routine.minute)
            findViewById<PowerTiles>(
                when (i) {
                    0 -> R.id.routinePower0
                    1 -> R.id.routinePower1
                    else -> R.id.routinePower2
                }
            ).litCount = if (routine.enabled) 4 else 1
        }

        val routine = Prefs.routine(selected)
        val endMinutesValue = (routine.hour * 60 + routine.minute + routine.minutes) % (24 * 60)

        findViewById<TileDisplay>(R.id.controlStartDigits).setTime(routine.hour, routine.minute)
        findViewById<TileDisplay>(R.id.controlEndDigits).setTime(endMinutesValue / 60, endMinutesValue % 60)
        findViewById<TimelineBar>(R.id.controlTimeline).apply {
            startMinutes = routine.hour * 60 + routine.minute
            endMinutes = endMinutesValue
            invalidate()
        }

        findViewById<SledSwitch>(R.id.routineSled).let { sled ->
            sled.onCheckedChange = null
            sled.setCheckedAnimated(routine.enabled, false)
            sled.onCheckedChange = { checked ->
                if (checked && Prefs.parachuteCount < 1) {
                    Toast.makeText(this, R.string.parachute_required_routine, Toast.LENGTH_LONG).show()
                    sled.setCheckedAnimated(false, false)
                } else if (checked && needsExactAlarmPermission()) {
                    askForExactAlarms(sled, routine)
                } else {
                    persistRoutine(routine.copy(enabled = checked))
                }
            }
        }

        findViewById<DayFaders>(R.id.controlWeek).let { week ->
            week.activeDays = routine.days
            week.onDayToggled = { day ->
                val days = if (routine.days.contains(day)) routine.days - day else routine.days + day
                if (days.isNotEmpty()) {
                    week.setDayActive(day, days.contains(day))
                    persistRoutine(routine.copy(days = days))
                }
            }
        }

        findViewById<RombiKnob>(R.id.controlStartKnob).let { knob ->
            knob.setMax(1439)
            knob.setValue(routine.hour * 60 + routine.minute)
            knob.onValueChanged = { value ->
                persistRoutine(routine.copy(hour = value / 60, minute = value % 60))
            }
        }
        findViewById<RombiKnob>(R.id.controlEndKnob).let { knob ->
            knob.setMax(1439)
            knob.setValue(endMinutesValue)
            knob.onValueChanged = { value ->
                val start = routine.hour * 60 + routine.minute
                val duration = ((value - start) + 1440) % 1440
                persistRoutine(routine.copy(minutes = duration.coerceAtLeast(15)))
            }
        }

        findViewById<TextView>(R.id.routineNextStart).text = if (routine.enabled) {
            endTimeLabel(RoutineScheduler.nextOccurrence(selected, System.currentTimeMillis()))
        } else {
            getString(R.string.routine_next_off)
        }
        findViewById<TextView>(R.id.routineDurationValue).text = formatDurationMinutes(routine.minutes)
        findViewById<TextView>(R.id.controlChuteCount).text = Prefs.parachuteCount.toString()
        val armedColor = getColor(R.color.colorOnSurface)
        val idleColor = (getColor(R.color.colorOnSurfaceVariant) and 0x00FFFFFF) or (0x9E shl 24)
        val digitColor = if (routine.enabled) armedColor else idleColor
        findViewById<TileDisplay>(R.id.controlStartDigits).setTileColor(digitColor)
        findViewById<TileDisplay>(R.id.controlEndDigits).setTileColor(digitColor)
        findViewById<View>(R.id.controlWeek).alpha = if (routine.enabled) 1f else 0.55f
        findViewById<View>(R.id.routineTabRow).alpha = 1f
        findViewById<View>(R.id.controlTimeline).alpha = if (routine.enabled) 1f else 0.55f
        findViewById<View>(R.id.controlChuteInfo).setOnClickListener { showChuteInfo() }
    }

    private fun needsExactAlarmPermission(): Boolean {
        if (Build.VERSION.SDK_INT < 31) return false
        val am = getSystemService(android.app.AlarmManager::class.java) ?: return false
        return !am.canScheduleExactAlarms()
    }

    private fun askForExactAlarms(sled: it.mirko.widget.SledSwitch, routine: Routine) {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle(R.string.routine_sheet_title)
            .setMessage(R.string.routine_sheet_body)
            .setPositiveButton(R.string.routine_sheet_confirm) { _, _ ->
                persistRoutine(routine.copy(enabled = true))
                com.openblockit.core.Permissions.openAlarms(this)
            }
            .setNegativeButton(R.string.routine_sheet_cancel) { _, _ ->
                sled.setCheckedAnimated(false, false)
            }
            .setOnCancelListener { sled.setCheckedAnimated(false, false) }
            .show()
    }

    private fun maybeAskForBatteryExemption() {
        if (Prefs.main.getBoolean("battery_prompt_done", false)) return
        if (com.openblockit.core.BatteryUtils.ignoreRequested(this)) return
        Prefs.main.edit().putBoolean("battery_prompt_done", true).apply()
        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle(R.string.battery_prompt_title)
            .setMessage(getString(R.string.battery_prompt_desc))
            .setPositiveButton(R.string.notification_protection_stuck_action) { _, _ ->
                com.openblockit.core.Permissions.openBattery(this)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT < 33) return
        val granted = checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
        if (!granted) notificationPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun showChuteInfo() {
        AlertDialog.Builder(this)
            .setTitle(R.string.parachute_info_title)
            .setMessage(R.string.parachute_info_desc)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private fun persistRoutine(routine: Routine) {
        Prefs.setRoutine(Prefs.activeRoutine, routine)
        if (routine.enabled) RoutineScheduler.scheduleNext(this, Prefs.activeRoutine)
        else RoutineScheduler.cancel(this, Prefs.activeRoutine)
        bindControl()
    }

    private fun bindBag() {
        val tagIds = listOf(R.id.bagTag0, R.id.bagTag1, R.id.bagTag2)
        val labelIds = listOf(R.id.bagTagLabel0, R.id.bagTagLabel1, R.id.bagTagLabel2)
        for (i in 0..2) {
            val active = i == Prefs.activeBag
            findViewById<TextView>(labelIds[i]).text = Prefs.bagName(i).ifEmpty { "Bag ${i + 1}" }
            findViewById<View>(tagIds[i]).apply {
                alpha = if (active) 1f else 0.55f
                setOnClickListener {
                    Prefs.activeBag = i
                    bindBag()
                    bindValigia()
                }
            }
        }
        findViewById<BagTiles>(R.id.bagGlassTiles).select(Prefs.activeBag)

        val slotIds = listOf(R.id.bagSlot0, R.id.bagSlot1, R.id.bagSlot2, R.id.bagSlot3, R.id.bagSlot4, R.id.bagSlot5)
        val iconIds = listOf(R.id.bagIcon0, R.id.bagIcon1, R.id.bagIcon2, R.id.bagIcon3, R.id.bagIcon4, R.id.bagIcon5)
        val slots = Prefs.bagSlots(Prefs.activeBag)
        slotIds.forEachIndexed { index, slotId ->
            val pkg = slots.getOrNull(index)?.takeIf { it.isNotEmpty() }
            val icon = findViewById<ImageView>(iconIds[index])
            if (pkg != null) {
                icon.setImageDrawable(BagRepo.iconFor(pkg))
                icon.imageTintList = null
                icon.alpha = 1f
            } else {
                icon.setImageResource(R.drawable.ic_add)
                icon.imageTintList = android.content.res.ColorStateList.valueOf(getColor(R.color.colorOnSurface))
                icon.alpha = 1f
            }
            findViewById<View>(slotId).setOnClickListener { showAppPicker(Prefs.activeBag, index) }
        }
    }

    private fun showAppPicker(bag: Int, slot: Int) {
        val installed = BagRepo.installedApps()
        if (installed.isEmpty()) return
        val density = resources.displayMetrics.density
        fun dp(value: Int) = (value * density).toInt()

        val sheet = com.google.android.material.bottomsheet.BottomSheetDialog(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(20))
            setBackgroundColor(getColor(R.color.colorSurface))
        }
        root.addView(TextView(this).apply {
            text = getString(R.string.bag_picker_title)
            setTextColor(getColor(R.color.colorOnSurface))
            textSize = 20f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        })

        val current = Prefs.bagSlots(bag).getOrNull(slot)?.takeIf { it.isNotEmpty() }
        if (current != null) {
            root.addView(com.google.android.material.button.MaterialButton(this).apply {
                text = getString(R.string.bag_empty_slot)
                setOnClickListener {
                    BagRepo.setApp(bag, slot, null)
                    sheet.dismiss()
                    bindBag()
                    bindValigia()
                }
            }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(12) })
        }

        val search = EditText(this).apply {
            hint = getString(R.string.bag_search_hint)
            setTextColor(getColor(R.color.colorOnSurface))
            setHintTextColor(getColor(R.color.colorOnSurfaceVariant))
        }
        root.addView(search, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(8) })

        val list = ListView(this)
        root.addView(list, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(380)))

        val rows = installed.toMutableList()
        val adapter = object : BaseAdapter() {
            override fun getCount() = rows.size
            override fun getItem(position: Int) = rows[position]
            override fun getItemId(position: Int) = position.toLong()
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val entry = rows[position]
                val row = LinearLayout(this@MainActivity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = android.view.Gravity.CENTER_VERTICAL
                    setPadding(dp(8), dp(10), dp(8), dp(10))
                }
                row.addView(ImageView(this@MainActivity).apply {
                    setImageDrawable(BagRepo.iconFor(entry.packageName))
                }, LinearLayout.LayoutParams(dp(36), dp(36)))
                row.addView(TextView(this@MainActivity).apply {
                    text = if (entry.packageName == current) "\u2713  ${entry.label}" else entry.label
                    setTextColor(getColor(R.color.colorOnSurface))
                    textSize = 16f
                    setPadding(dp(14), 0, 0, 0)
                })
                return row
            }
        }
        list.adapter = adapter
        list.setOnItemClickListener { _, _, position, _ ->
            BagRepo.setApp(bag, slot, rows[position].packageName)
            sheet.dismiss()
            bindBag()
            bindValigia()
        }
        search.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                val query = s?.toString()?.trim().orEmpty()
                rows.clear()
                rows.addAll(installed.filter { query.isEmpty() || it.label.contains(query, true) })
                adapter.notifyDataSetChanged()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
        })

        sheet.setContentView(root)
        sheet.show()
    }

    private fun bindStats() {
        val modeTiles = listOf(R.id.statsModeDay, R.id.statsModeMonth, R.id.statsModeYear)
        val modeLabels = listOf(R.string.stats_mode_day, R.string.stats_mode_month, R.string.stats_mode_year)
        modeTiles.forEachIndexed { index, id ->
            findViewById<HapticBuyTile>(id).apply {
                setSingleText(getString(modeLabels[index]))
                setActive(index == statsMode)
                setOnClickListener {
                    if (statsMode != index) {
                        statsMode = index
                        statsOffset = 0
                        statsSelection = -1
                        bindStats()
                    }
                }
            }
        }
        findViewById<View>(R.id.statsPrev).setOnClickListener {
            statsOffset -= 1
            statsSelection = -1
            bindStats()
        }
        findViewById<View>(R.id.statsNext).setOnClickListener {
            if (statsOffset < 0) {
                statsOffset += 1
                statsSelection = -1
                bindStats()
            }
        }
        findViewById<View>(R.id.statsNext).alpha = if (statsOffset < 0) 1f else 0.4f
        findViewById<View>(R.id.statsPrev).alpha = 1f

        lifecycleScope.launch {
            val days = runCatching { StatsRepo.allDays() }.getOrDefault(emptyList())
            val byDate = days.associateBy { it.date }
            val calendar = java.util.Calendar.getInstance()
            val locale = resources.configuration.locales.get(0) ?: java.util.Locale.getDefault()

            val bars: FloatArray
            val periodLabel: String
            val caption: String

            when (statsMode) {
                0 -> {
                    calendar.add(java.util.Calendar.DAY_OF_YEAR, statsOffset)
                    val key = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(calendar.time)
                    val hourly = runCatching { StatsRepo.hourlyOn(key) }.getOrDefault(IntArray(24))
                    bars = FloatArray(24) { hour -> hourly[hour].toFloat() }
                    val weekday = java.text.SimpleDateFormat("EEEE", locale).format(calendar.time)
                    val dayNumber = calendar.get(java.util.Calendar.DAY_OF_MONTH)
                    periodLabel = if (statsOffset == 0) getString(R.string.stats_today)
                    else if (statsOffset == -1) getString(R.string.stats_yesterday)
                    else "$weekday $dayNumber".uppercase(locale)
                    caption = "$weekday $dayNumber".uppercase(locale)
                }
                1 -> {
                    calendar.add(java.util.Calendar.MONTH, statsOffset)
                    val daysInMonth = calendar.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
                    bars = FloatArray(daysInMonth)
                    for (day in 1..daysInMonth) {
                        calendar.set(java.util.Calendar.DAY_OF_MONTH, day)
                        val key = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(calendar.time)
                        bars[day - 1] = (byDate[key]?.minutes ?: 0).toFloat()
                    }
                    periodLabel = java.text.SimpleDateFormat("d. MMMM yyyy", locale)
                        .format(calendar.time).substringAfter(' ').uppercase(locale)
                    caption = java.text.SimpleDateFormat("d. MMMM yyyy", locale)
                        .format(calendar.time).uppercase(locale)
                }
                else -> {
                    calendar.add(java.util.Calendar.YEAR, statsOffset)
                    val year = calendar.get(java.util.Calendar.YEAR)
                    bars = FloatArray(12)
                    for (month in 0 until 12) {
                        var total = 0
                        for (day in days) {
                            if (day.date.startsWith("%04d-%02d".format(year, month + 1))) total += day.minutes
                        }
                        bars[month] = total.toFloat()
                    }
                    periodLabel = year.toString()
                    caption = year.toString()
                }
            }

            val total = bars.sum().toInt()
            findViewById<TextView>(R.id.statsPeriodValue).text = periodLabel
            findViewById<TileDisplay>(R.id.statsDisplay).setBars(bars)

            fun labelFor(index: Int): String {
                if (index !in bars.indices) return caption
                val cal = calendar.clone() as java.util.Calendar
                return when (statsMode) {
                    0 -> "%02d:00".format(index)
                    1 -> {
                        cal.set(java.util.Calendar.DAY_OF_MONTH, index + 1)
                        java.text.SimpleDateFormat("EEEE d", locale).format(cal.time)
                    }
                    else -> {
                        cal.set(java.util.Calendar.MONTH, index)
                        cal.set(java.util.Calendar.DAY_OF_MONTH, 1)
                        java.text.SimpleDateFormat("LLLL", locale).format(cal.time)
                    }
                }.uppercase(locale)
            }

            fun showSelection(index: Int) {
                findViewById<TextView>(R.id.statsPeriodOut).text =
                    "${labelFor(index)}  ·  ${bars[index].toInt()} MIN"
            }

            val today = java.util.Calendar.getInstance()
            val defaultSelection = when (statsMode) {
                0 -> today.get(java.util.Calendar.HOUR_OF_DAY)
                1 -> today.get(java.util.Calendar.DAY_OF_MONTH) - 1
                else -> today.get(java.util.Calendar.MONTH)
            }
            val selection = (if (statsSelection in bars.indices) statsSelection else defaultSelection)
                .coerceIn(0, bars.size - 1)

            findViewById<RotaryWheel>(R.id.statsRotary).let { wheel ->
                wheel.setBars(bars.size, selection, bars.size - 1)
                wheel.onBarSelected = { index ->
                    statsSelection = index
                    showSelection(index)
                }
            }
            showSelection(selection)
        }
    }

    private fun confirmParachute() {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle(R.string.take_a_parachute)
            .setMessage(getString(R.string.parachute_info_desc))
            .setPositiveButton(android.R.string.ok) { _, _ ->
                SessionManager.endByParachute()
                exitBlockMode(animate = true)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    open fun hostsBlockScreen(): Boolean = false

    private fun showCompletion() {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle(R.string.complete_session_title)
            .setMessage(getString(R.string.complete_session_desc))
            .setPositiveButton(R.string.complete_session_completed_action) { _, _ ->
                if (hostsBlockScreen()) finish()
            }
            .setOnCancelListener { if (hostsBlockScreen()) finish() }
            .show()
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
