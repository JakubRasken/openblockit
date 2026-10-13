package it.mirko.widget

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.view.animation.PathInterpolator
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
    private val colorOnAccent = context.getColor(R.color.colorOnPrimary)

    private val standard = PathInterpolator(0.2f, 0f, 0f, 1f)
    private val emphasized = PathInterpolator(0.05f, 0.7f, 0.1f, 1f)
    private val accelerate = PathInterpolator(0.3f, 0f, 0.8f, 0.15f)

    private var hintTextRes = R.string.slide_to_start
    private var iconRes = R.drawable.ic_play
    private var dragging = false
    private var downX = 0f
    private var completed = false
    private var exitMode = false

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
        if (hint.layoutParams is ViewGroup.MarginLayoutParams) {
            (hint.layoutParams as ViewGroup.MarginLayoutParams).apply {
                leftMargin = dp(92)
                rightMargin = dp(20)
            }
        }
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
        applyHintMargins()
    }

    private fun applyHintMargins() {
        if (hint.layoutParams is ViewGroup.MarginLayoutParams) {
            (hint.layoutParams as ViewGroup.MarginLayoutParams).apply {
                leftMargin = if (exitMode) dp(20) else dp(92)
                rightMargin = if (exitMode) dp(92) else dp(20)
                hint.layoutParams = this
            }
        }
    }

    fun setOnSlideCompleteListener(listener: () -> Unit) {
        onSlideComplete = listener
    }

    fun setParachuteDeniedListener(listener: Runnable) {
        onParachuteDenied = { listener.run() }
    }

    fun setWarmOnGrab(value: Boolean) = Unit

    fun setHintFadeFast(value: Boolean) = Unit

    fun armExit() {
        exitMode = true
        handleIcon.setImageResource(R.drawable.ic_parachute)
        hint.text = context.getString(R.string.slide_to_exit)
        hint.visibility = View.VISIBLE
        hint.alpha = 1f
        applyHintMargins()
        handle.alpha = 1f
        handle.setCardBackgroundColor(colorAccent)
    }

    fun disarm() {
        exitMode = false
    }

    private fun maxOffset(): Float =
        (width - paddingLeft - paddingRight - handle.width).toFloat().coerceAtLeast(0f)

    private fun progress(): Float {
        val max = maxOffset()
        if (max <= 0f) return 0f
        return (handle.translationX / max).coerceIn(0f, 1f)
    }

    private fun applyProgress(p: Float) {
        hint.alpha = (1f - 0.55f * p).coerceAtLeast(0.45f)
        handle.setCardBackgroundColor(colorAccent)
    }

    fun reset() {
        dragging = false
        completed = false
        exitMode = false
        handle.animate().cancel()
        handle.animate().translationX(0f).scaleX(1f).scaleY(1f)
            .setDuration(380L).setInterpolator(standard).start()
        hint.animate().alpha(1f).setDuration(200L).start()
        handle.setCardBackgroundColor(colorAccent)
        handleIcon.setImageResource(iconRes)
        hint.text = context.getString(hintTextRes)
        applyHintMargins()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled) return false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val left = handle.x - dp(8)
                if (event.x in left..(left + handle.width + dp(16))) {
                    dragging = true
                    completed = false
                    downX = event.x - handle.translationX
                    parent?.requestDisallowInterceptTouchEvent(true)
                    handle.animate().cancel()
                    handle.animate().scaleX(0.94f).scaleY(0.94f)
                        .setDuration(120L).setInterpolator(emphasized).start()
                    return true
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (dragging) {
                    val target = (event.x - downX).coerceIn(0f, maxOffset())
                    handle.translationX = target
                    applyProgress(progress())
                    if (progress() >= 0.85f && !completed) completed = true
                    if (progress() < 0.8f) completed = false
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (dragging) {
                    dragging = false
                    parent?.requestDisallowInterceptTouchEvent(false)
                    handle.animate().scaleX(1f).scaleY(1f).setDuration(180L)
                        .setInterpolator(emphasized).start()
                    if (completed) {
                        handle.animate().translationX(maxOffset()).setDuration(110L)
                            .setInterpolator(emphasized).withEndAction {
                                val callback = onSlideComplete
                                if (Prefs.parachuteCount >= 1 || !exitMode) {
                                    reset()
                                    callback?.invoke()
                                } else {
                                    reset()
                                    onParachuteDenied?.invoke()
                                }
                            }.start()
                    } else {
                        handle.animate().translationX(0f).setDuration(380L)
                            .setInterpolator(standard).start()
                        hint.animate().alpha(1f).setDuration(200L).start()
                        handle.setCardBackgroundColor(colorAccent)
                    }
                    return true
                }
            }
        }
        return super.onTouchEvent(event)
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
