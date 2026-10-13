package com.openblockit

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageView
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
import it.mirko.widget.BagTiles
import it.mirko.widget.DayFaders
import it.mirko.widget.HapticPreset
import it.mirko.widget.HapticSlider
import it.mirko.widget.PowerTiles
import it.mirko.widget.RombiKnob
import it.mirko.widget.SledSwitch
import it.mirko.widget.SlideToStart
import it.mirko.widget.SwipeSections
import it.mirko.widget.TimelineBar
import it.mirko.widget.display.TileDisplay
import it.mirko.widget.navigation.AltNavigationBar
import it.mirko.widget.numbers.SquareNumber
import it.mirko.widget.stats.StatisticsGraph
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var root: SwipeSections
    private lateinit var nav: AltNavigationBar
    private lateinit var slide: SlideToStart
    private lateinit var wheel: HapticSlider
    private lateinit var hourLeft: SquareNumber
    private lateinit var hourRight: SquareNumber
    private lateinit var minuteLeft: SquareNumber
    private lateinit var minuteRight: SquareNumber
    private lateinit var customValue: TextView

    private val handler = Handler(Looper.getMainLooper())
    private var selectedMinutes = 30
    private var blockMode = false

    private val ticker = object : Runnable {
        override fun run() {
            if (!blockMode) return
            updateBlockTiles()
            handler.postDelayed(this, 1000L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!Prefs.onboardingDone) {
            startActivity(Intent(this, OnboardingActivity::class.java))
            finish()
            return
        }
        setContentView(R.layout._activity_home)
        selectedMinutes = Prefs.lastMinutes.coerceIn(5, 720)
        hidePremium()
        bindToolbar()
        bindSession()
        bindNav()
        bindControl()
        bindBag()
        bindStats()
        refreshIdle()
        if (SessionManager.started()) enterBlockMode()
        if (intent?.getBooleanExtra("completed", false) == true) showCompletion()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.getBooleanExtra("completed", false)) showCompletion()
    }

    override fun onResume() {
        super.onResume()
        if (SessionManager.started() && !blockMode) enterBlockMode()
        if (!SessionManager.started() && blockMode) exitBlockMode()
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
    }

    private fun bindToolbar() {
        findViewById<View>(R.id.streakBadge).visibility = View.VISIBLE
        findViewById<TextView>(R.id.toolbarChuteCount).text = Prefs.parachuteCount.toString()
        findViewById<View>(R.id.toolbarChutePill).setOnClickListener { selectSection(3) }
        findViewById<View>(R.id.profileButton).setOnClickListener {
            startActivity(Intent(this, OnboardingActivity::class.java))
        }
        lifecycleScope.launch {
            val streak = runCatching { StatsRepo.allDays() }.getOrDefault(emptyList()).let { TowerRepo.currentStreak(it) }
            findViewById<TextView>(R.id.streakCount).text = streak.toString()
        }
    }

    private fun bindSession() {
        hourLeft = findViewById(R.id.hourLeft)
        hourRight = findViewById(R.id.hourRight)
        minuteLeft = findViewById(R.id.minuteLeft)
        minuteRight = findViewById(R.id.minuteRight)
        customValue = findViewById(R.id.starValue)
        wheel = findViewById(R.id.wheel)

        listOf(hourLeft, hourRight, minuteLeft, minuteRight, findViewById<SquareNumber>(R.id.timerSeparator))
            .forEach { it.setSizeMultiplier(11) }
        minuteLeft.alpha = 0.7f
        minuteRight.alpha = 0.7f
        findViewById<View>(R.id.timerSeparator).alpha = 0.7f

        wheel.setRange(5, 720)
        wheel.setValue(selectedMinutes)
        wheel.onValueChanged = { value ->
            selectedMinutes = value
            Prefs.lastMinutes = value
            updateTimer(value / 60, value % 60)
            renderCustom()
        }

        findViewById<HapticPreset>(R.id.preset_fifteen_minutes).apply {
            setMinutes(15)
            setOnClickListener { chooseMinutes(15) }
        }
        findViewById<HapticPreset>(R.id.preset_thirty_minutes).apply {
            setMinutes(45)
            setOnClickListener { chooseMinutes(45) }
        }
        findViewById<HapticPreset>(R.id.preset_three_hours).apply {
            setMinutes(180)
            setOnClickListener { chooseMinutes(180) }
        }
        findViewById<View>(R.id.preset_custom).setOnClickListener {
            selectedMinutes = wheel.value
            Prefs.lastMinutes = selectedMinutes
            updateTimer(selectedMinutes / 60, selectedMinutes % 60)
        }
        findViewById<View>(R.id.starLabel).setOnClickListener {
            selectedMinutes = wheel.value
            updateTimer(selectedMinutes / 60, selectedMinutes % 60)
        }

        findViewById<View>(R.id.sliderUp).setOnClickListener {
            wheel.setValue((selectedMinutes + 5).coerceAtMost(720))
            selectedMinutes = wheel.value
            Prefs.lastMinutes = selectedMinutes
            updateTimer(selectedMinutes / 60, selectedMinutes % 60)
            renderCustom()
        }
        findViewById<View>(R.id.sliderDown).setOnClickListener {
            wheel.setValue((selectedMinutes - 5).coerceAtLeast(5))
            selectedMinutes = wheel.value
            Prefs.lastMinutes = selectedMinutes
            updateTimer(selectedMinutes / 60, selectedMinutes % 60)
            renderCustom()
        }

        slide = findViewById(R.id.slideToStart)
        slide.setOnSlideCompleteListener { startSession() }

        findViewById<View>(R.id.fab).setOnClickListener { startSession() }

        findViewById<View>(R.id.extendBox).setOnClickListener {
            if (SessionManager.extend(15)) updateBlockTiles()
        }
        findViewById<View>(R.id.soundBox).setOnClickListener {
            val next = (Prefs.soundMode + 1) % 5
            Prefs.soundMode = next
            com.openblockit.core.Sounds.start(this, next)
        }
        findViewById<View>(R.id.parachuteBox).setOnClickListener { selectSection(3) }
        bindValigia()
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

    private fun chooseMinutes(minutes: Int) {
        selectedMinutes = minutes
        Prefs.lastMinutes = minutes
        wheel.setValue(minutes)
        updateTimer(minutes / 60, minutes % 60)
        renderCustom()
    }

    private fun renderCustom() {
        customValue.text = "%02d:%02d".format(selectedMinutes / 60, selectedMinutes % 60)
    }

    private fun updateTimer(hours: Int, minutes: Int) {
        hourLeft.setValue(hours / 10)
        hourRight.setValue(hours % 10)
        minuteLeft.setValue(minutes / 10)
        minuteRight.setValue(minutes % 10)
    }

    private fun refreshIdle() {
        updateTimer(selectedMinutes / 60, selectedMinutes % 60)
        renderCustom()
    }

    private fun startSession() {
        when (SessionManager.start(selectedMinutes)) {
            StartResult.Started -> enterBlockMode()
            StartResult.AlreadyRunning -> enterBlockMode()
            StartResult.NeedsParachute ->
                Toast.makeText(this, R.string.parachute_required_long, Toast.LENGTH_LONG).show()
        }
    }

    private fun enterBlockMode() {
        blockMode = true
        findViewById<View>(R.id.sessionMiddle).visibility = View.GONE
        findViewById<View>(R.id.blockMiddle).visibility = View.VISIBLE
        findViewById<View>(R.id.blockValigia).visibility = View.VISIBLE
        slide.armExit()
        slide.setOnSlideCompleteListener { confirmParachute() }
        updateBlockTiles()
        handler.removeCallbacks(ticker)
        handler.post(ticker)
    }

    private fun exitBlockMode() {
        blockMode = false
        handler.removeCallbacks(ticker)
        findViewById<View>(R.id.sessionMiddle).visibility = View.VISIBLE
        findViewById<View>(R.id.blockMiddle).visibility = View.GONE
        findViewById<View>(R.id.blockValigia).visibility = View.GONE
        slide.reset()
        slide.setHint(R.string.slide_to_start, R.drawable.ic_play)
        slide.setOnSlideCompleteListener { startSession() }
        refreshIdle()
        findViewById<TextView>(R.id.toolbarChuteCount).text = Prefs.parachuteCount.toString()
    }

    private fun updateBlockTiles() {
        val secondsLeft = SessionManager.secondsRemaining()
        if (secondsLeft <= 0) {
            if (blockMode) exitBlockMode()
            return
        }
        findViewById<SquareNumber>(R.id.secLeft).apply {
            setSizeMultiplier(9)
            setValue((secondsLeft % 60) / 10)
        }
        findViewById<SquareNumber>(R.id.secRight).apply {
            setSizeMultiplier(9)
            setValue(secondsLeft % 10)
        }
        findViewById<TextView>(R.id.endTimeValue).text =
            endTimeLabel(System.currentTimeMillis() + secondsLeft * 1000L)
        findViewById<TextView>(R.id.blockParachuteCount).text = Prefs.parachuteCount.toString()
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
                } else {
                    persistRoutine(routine.copy(enabled = checked))
                }
            }
        }

        findViewById<DayFaders>(R.id.controlWeek).let { week ->
            week.activeDays = routine.days
            week.onDayToggled = { day ->
                val days = if (routine.days.contains(day)) routine.days - day else routine.days + day
                if (days.isNotEmpty()) persistRoutine(routine.copy(days = days))
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
        val apps = BagRepo.appsIn(Prefs.activeBag)
        slotIds.forEachIndexed { index, slotId ->
            val pkg = apps.getOrNull(index)
            val icon = findViewById<ImageView>(iconIds[index])
            icon.setImageDrawable(if (pkg != null) BagRepo.iconFor(pkg) else null)
            findViewById<View>(slotId).setOnClickListener {
                showAppPicker(Prefs.activeBag)
            }
        }
    }

    private fun showAppPicker(bag: Int) {
        val apps = BagRepo.installedApps()
        if (apps.isEmpty()) return
        val labels = apps.map { it.label }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle(R.string.bag_picker_title)
            .setItems(labels) { _, which ->
                BagRepo.addApp(bag, apps[which].packageName)
                bindBag()
                bindValigia()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun bindStats() {
        lifecycleScope.launch {
            val days = runCatching { StatsRepo.allDays() }.getOrDefault(emptyList())
            val last = (0 until 14).map { Clock.dayKey(System.currentTimeMillis() - it * 86_400_000L) }.reversed()
            val values = last.map { key -> (days.firstOrNull { it.date == key }?.minutes ?: 0).toFloat() }
            findViewById<StatisticsGraph>(R.id.statisticsGraph).values = values
            val todayMinutes = days.firstOrNull { it.date == Clock.todayKey() }?.minutes ?: 0
            findViewById<TileDisplay>(R.id.statsDisplay).setTime(todayMinutes / 60, todayMinutes % 60)
        }
    }

    private fun confirmParachute() {
        AlertDialog.Builder(this)
            .setTitle(R.string.take_a_parachute)
            .setMessage(getString(R.string.countdown_parachute_explain))
            .setPositiveButton(android.R.string.ok) { _, _ ->
                SessionManager.endByParachute()
                exitBlockMode()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showCompletion() {
        AlertDialog.Builder(this)
            .setTitle(R.string.complete_session_title)
            .setMessage(getString(R.string.complete_session_desc))
            .setPositiveButton(R.string.complete_session_completed_action, null)
            .show()
    }
}
