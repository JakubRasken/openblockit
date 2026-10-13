package com.openblockit

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.openblockit.core.BagRepo
import com.openblockit.core.Clock
import com.openblockit.core.Prefs
import com.openblockit.core.SessionManager
import com.openblockit.core.StartResult
import com.openblockit.core.StatsRepo
import com.openblockit.core.TowerRepo
import com.openblockit.core.endTimeLabel
import it.mirko.widget.HapticPreset
import it.mirko.widget.HapticSlider
import it.mirko.widget.SlideToStart
import it.mirko.widget.SwipeSections
import it.mirko.widget.navigation.AltNavigationBar
import it.mirko.widget.numbers.SquareNumber
import androidx.lifecycle.lifecycleScope
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
        bindToolbar()
        bindSession()
        bindNav()
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
    }

    private fun bindToolbar() {
        findViewById<View>(R.id.streakBadge).visibility = View.VISIBLE
        findViewById<TextView>(R.id.toolbarChuteCount).text = Prefs.parachuteCount.toString()
        findViewById<View>(R.id.toolbarChutePill).setOnClickListener {
            selectSection(3)
        }
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
        findViewById<View>(R.id.parachuteBox).setOnClickListener {
            selectSection(3)
        }
        bindValigia()
    }

    private fun bindValigia() {
        val cellIds = listOf(R.id.valigiaCell0, R.id.valigiaCell1, R.id.valigiaCell2, R.id.valigiaCell3, R.id.valigiaCell4, R.id.valigiaCell5)
        val iconIds = listOf(R.id.valigiaIcon0, R.id.valigiaIcon1, R.id.valigiaIcon2, R.id.valigiaIcon3, R.id.valigiaIcon4, R.id.valigiaIcon5)
        val apps = BagRepo.appsIn(Prefs.activeBag)
        cellIds.forEachIndexed { index, cellId ->
            val cell = findViewById<View>(cellId)
            val icon = findViewById<android.widget.ImageView>(iconIds[index])
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
        val dim = hours == 0
        minuteLeft.alpha = if (dim) 0.7f else 1f
        minuteRight.alpha = if (dim) 0.7f else 1f
        findViewById<View>(R.id.timerSeparator).alpha = 0.7f
        hourLeft.visibility = View.VISIBLE
        hourRight.visibility = View.VISIBLE
        findViewById<View>(R.id.spaceHour).visibility = View.VISIBLE
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
        findViewById<View>(R.id.customEditor).visibility = View.GONE
        findViewById<View>(R.id.blockValigia).visibility = View.VISIBLE
        slide.armExit()
        slide.setOnSlideCompleteListener { confirmParachute() }
        findViewById<View>(R.id.sliderUp).visibility = View.GONE
        findViewById<View>(R.id.sliderDown).visibility = View.GONE
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
        findViewById<View>(R.id.sliderUp).visibility = View.VISIBLE
        findViewById<View>(R.id.sliderDown).visibility = View.VISIBLE
        refreshIdle()
    }

    private fun updateBlockTiles() {
        val secondsLeft = SessionManager.secondsRemaining()
        if (secondsLeft <= 0) {
            if (blockMode) exitBlockMode()
            return
        }
        findViewById<SquareNumber>(R.id.secLeft).setValue((secondsLeft % 60) / 10)
        findViewById<SquareNumber>(R.id.secRight).setValue(secondsLeft % 10)
        findViewById<TextView>(R.id.endTimeValue).text =
            endTimeLabel(System.currentTimeMillis() + secondsLeft * 1000L)
        findViewById<TextView>(R.id.blockParachuteCount).text = Prefs.parachuteCount.toString()
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
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(R.string.complete_session_title)
            .setMessage(getString(R.string.complete_session_desc))
            .setPositiveButton(R.string.complete_session_completed_action, null)
            .show()
    }
}
