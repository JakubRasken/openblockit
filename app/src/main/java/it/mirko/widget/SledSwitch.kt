package it.mirko.widget

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import com.google.android.material.card.MaterialCardView
import com.openblockit.R
import com.openblockit.core.Prefs

class SledSwitch @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : MaterialCardView(context, attrs, defStyleAttr) {

    private val knob = View(context)
    private var checkedState = false

    var onCheckedChange: ((Boolean) -> Unit)? = null

    init {
        val knobWidth = dp(30)
        val knobHeight = dp(44)
        knob.background = GradientDrawable().apply {
            cornerRadius = dp(10).toFloat()
            setColor(context.getColor(R.color.colorOnSurface))
        }
        addView(
            knob,
            FrameLayout.LayoutParams(knobWidth, knobHeight).apply {
                gravity = Gravity.CENTER_VERTICAL or Gravity.START
                marginStart = dp(7)
            },
        )
        setOnClickListener {
            toggle()
            if (Prefs.hapticsEnabled) {
                try {
                    performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
                } catch (_: Exception) {
                }
            }
            onCheckedChange?.invoke(checkedState)
        }
        applyState(false)
    }

    override fun isChecked(): Boolean = checkedState

    override fun setChecked(checked: Boolean) {
        checkedState = checked
        applyState(true)
    }

    override fun toggle() {
        setChecked(!checkedState)
    }

    fun setCheckedAnimated(value: Boolean, animate: Boolean) {
        checkedState = value
        applyState(animate)
    }

    private fun applyState(animate: Boolean) {
        val target = if (checkedState) width - dp(37) else dp(7)
        val alpha = if (checkedState) 1f else 0.35f
        knob.animate().cancel()
        if (animate && width > 0) {
            knob.animate().translationX(target.toFloat()).alpha(alpha).setDuration(180L).start()
        } else {
            knob.post {
                knob.translationX = target.toFloat()
            }
            knob.alpha = alpha
        }
        setCardBackgroundColor(
            context.getColor(if (checkedState) R.color.colorPrimaryContainer else R.color.colorSurface)
        )
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        applyState(false)
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
