package it.mirko.widget

import android.animation.ArgbEvaluator
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import com.google.android.material.card.MaterialCardView
import com.openblockit.R
import com.openblockit.core.Prefs

class SlideToStart @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    private val handle: MaterialCardView
    private val handleIcon: AppCompatImageView
    private val hint: TextView
    private val argb = ArgbEvaluator()
    private val colorOnSurface = context.getColor(R.color.colorOnSurface)
    private val colorAccent = context.getColor(R.color.colorPrimaryContainer)

    private var hintTextRes = R.string.slide_to_start
    private var iconRes = R.drawable.ic_play
    private var dragging = false
    private var downX = 0f
    private var completed = false

    var onSlideComplete: (() -> Unit)? = null
    var onParachuteDenied: (() -> Unit)? = null

    init {
        LayoutInflater.from(context).inflate(R.layout._slide_to_start, this, true)
        handle = findViewById(R.id.slideHandle)
        handleIcon = findViewById(R.id.slideHandleIcon)
        hint = findViewById(R.id.slideHint)
        findViewById<View?>(R.id.slideGoal)?.visibility = View.GONE
        setLayoutDirection(LAYOUT_DIRECTION_LTR)
        hint.textDirection = TEXT_DIRECTION_RTL
        hint.text = context.getString(hintTextRes)
        handleIcon.setImageResource(iconRes)
        handle.translationX = 0f
        handle.setOnClickListener(null)
        handle.isClickable = false
    }

    fun setHint(resText: Int, resIcon: Int) {
        hintTextRes = resText
        iconRes = resIcon
        hint.setText(resText)
        handleIcon.setImageResource(resIcon)
    }

    fun setOnSlideCompleteListener(listener: () -> Unit) {
        onSlideComplete = listener
    }

    fun setParachuteDeniedListener(listener: Runnable) {
        onParachuteDenied = { listener.run() }
    }

    fun setWarmOnGrab(value: Boolean) = Unit

    fun setHintFadeFast(value: Boolean) = Unit

    private fun maxOffset(): Float =
        (width - paddingLeft - paddingRight - handle.width).toFloat().coerceAtLeast(0f)

    private fun progress(): Float {
        val max = maxOffset()
        if (max <= 0f) return 0f
        return 1f - (handle.translationX / max)
    }

    private fun applyProgress(p: Float) {
        hint.alpha = (1f - 0.55f * p).coerceAtLeast(0.45f)
        handle.setCardBackgroundColor(argb.evaluate(p, colorOnSurface, colorAccent) as Int)
    }

    fun reset() {
        dragging = false
        completed = false
        handle.animate().translationX(0f).setDuration(300L).start()
        hint.animate().alpha(1f).setDuration(200L).start()
        handle.setCardBackgroundColor(colorOnSurface)
        handleIcon.setImageResource(iconRes)
        hint.text = context.getString(hintTextRes)
    }

    fun armExit() {
        handleIcon.setImageResource(R.drawable.ic_parachute)
        hint.text = context.getString(R.string.slide_to_exit)
        hint.visibility = View.VISIBLE
        hint.alpha = 1f
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled) return false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val left = handle.x - 24
                if (event.x in left..(left + handle.width + 48)) {
                    dragging = true
                    completed = false
                    downX = event.x - handle.translationX
                    parent?.requestDisallowInterceptTouchEvent(true)
                    return true
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (dragging) {
                    val target = (event.x - downX).coerceIn(0f, maxOffset())
                    handle.translationX = target
                    applyProgress(progress())
                    if (progress() >= 0.92f && !completed) {
                        completed = true
                    }
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (dragging) {
                    dragging = false
                    parent?.requestDisallowInterceptTouchEvent(false)
                    if (completed) {
                        handle.animate().translationX(maxOffset()).setDuration(110L).withEndAction {
                            val cb = onSlideComplete
                            if (Prefs.parachuteCount >= 1) {
                                reset()
                                cb?.invoke()
                            } else {
                                reset()
                                onParachuteDenied?.invoke()
                            }
                        }.start()
                    } else {
                        handle.animate().translationX(0f).setDuration(300L).start()
                        hint.animate().alpha(1f).setDuration(200L).start()
                        handle.setCardBackgroundColor(colorOnSurface)
                    }
                    return true
                }
            }
        }
        return super.onTouchEvent(event)
    }
}
