package com.openblockit.ui.block

import android.app.AlertDialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.openblockit.R
import com.openblockit.core.Prefs
import com.openblockit.core.SessionManager
import com.openblockit.core.endTimeLabel
import it.mirko.widget.SlideToStart
import it.mirko.widget.numbers.SquareNumber

class BlockActivity : AppCompatActivity() {

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var slide: SlideToStart

    private val ticker = object : Runnable {
        override fun run() {
            if (!SessionManager.started()) {
                finish()
                return
            }
            refresh()
            handler.postDelayed(this, 1000L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout._activity_home)

        findViewById<View>(R.id.altNavBarContainer).visibility = View.GONE
        (findViewById<View>(R.id.fab).parent as? View)?.visibility = View.GONE
        findViewById<View>(R.id.toolbarChutePill).visibility = View.VISIBLE
        findViewById<TextView>(R.id.toolbarChuteCount).text = Prefs.parachuteCount.toString()
        findViewById<View>(R.id.homeLogo).visibility = View.GONE
        findViewById<View>(R.id.sessionMiddle).visibility = View.GONE
        findViewById<View>(R.id.blockMiddle).visibility = View.VISIBLE
        findViewById<View>(R.id.blockValigia).visibility = View.VISIBLE

        slide = findViewById(R.id.slideToStart)
        slide.armExit()
        slide.setOnSlideCompleteListener { confirmParachute() }

        findViewById<View>(R.id.extendBox).setOnClickListener {
            if (SessionManager.extend(15)) refresh()
        }
        findViewById<View>(R.id.soundBox).setOnClickListener {
            val next = (Prefs.soundMode + 1) % 5
            Prefs.soundMode = next
            com.openblockit.core.Sounds.start(this, next)
        }

        refresh()
        handler.post(ticker)
    }

    private fun refresh() {
        val secondsLeft = SessionManager.secondsRemaining()
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
                if (SessionManager.endByParachute()) finish()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    override fun onDestroy() {
        handler.removeCallbacks(ticker)
        super.onDestroy()
    }
}
