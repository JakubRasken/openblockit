package it.mirko.widget

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.animation.PathInterpolator
import com.google.android.material.card.MaterialCardView
import com.openblockit.R

class HapticSlider @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : MaterialCardView(context, attrs, defStyleAttr) {

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.colorOnSurface)
        alpha = 100
        strokeWidth = 4f
    }
    private val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.colorPrimaryContainer)
    }

    private val spacing: Float = 9f * resources.displayMetrics.density
    private var downY = 0f
    private var accumulated = 0f
    private var active = false
    private var accentActive = false

    var minValue = 1
    var maxValue = 720
    var minutesCount = 30
        private set

    var onMinutesChanged: ((Int) -> Unit)? = null
    var onTimePicked: (() -> Unit)? = null

    private val emphasized = PathInterpolator(0.05f, 0.7f, 0.1f, 1f)

    fun setLimitMin(value: Int) {
        minValue = value
        if (minutesCount < minValue) setMinutes(minValue)
    }

    fun setLimitMax(value: Int) {
        maxValue = value
        if (minutesCount > maxValue) setMinutes(maxValue)
    }

    fun setMinutes(value: Int) {
        minutesCount = value.coerceIn(minValue, maxValue)
        onMinutesChanged?.invoke(minutesCount)
        invalidate()
    }

    fun setMinutesAnimate(target: Int) {
        val clamped = target.coerceIn(minValue, maxValue)
        val from = minutesCount
        val duration = (Math.abs(clamped - from).toLong() % maxValue.toLong()) * 10L
        val animator = ValueAnimator.ofInt(from, clamped)
        animator.setDuration(Math.max(duration, 1000L))
        animator.setInterpolator(emphasized)
        animator.addUpdateListener { value ->
            minutesCount = value.animatedValue as Int
            onMinutesChanged?.invoke(minutesCount)
            invalidate()
        }
        animator.start()
    }

    fun getHours(): String = "%02d".format(minutesCount / 60)

    fun getMinutes(): String = "%02d".format(minutesCount % 60)

    fun setAccentActive(value: Boolean) {
        accentActive = value
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val inset = paddingTop.coerceAtLeast(8)
        val lineW = width * 0.5f
        val startX = (width - lineW) / 2f
        val scroll = minutesCount * spacing / 6f
        var y = height / 2f - (scroll % spacing)
        while (y > inset) {
            if (y < height - inset) {
                canvas.drawLine(startX, y, startX + lineW, y, linePaint)
            }
            y -= spacing
        }
        accentPaint.color = if (accentActive) context.getColor(R.color.colorPrimaryContainer)
        else context.getColor(R.color.colorPrimaryContainer)
        val barH = 6f * resources.displayMetrics.density
        canvas.drawRoundRect(
            width * 0.25f,
            height / 2f - barH / 2f,
            width * 0.75f,
            height / 2f + barH / 2f,
            barH / 2f,
            barH / 2f,
            accentPaint,
        )
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                active = true
                downY = event.y
                accumulated = 0f
                parent?.requestDisallowInterceptTouchEvent(true)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (active) {
                    val delta = event.y - downY
                    val mapped = (delta / height.toFloat()) * 60f
                    val steps = mapped - accumulated
                    if (kotlin.math.abs(steps) >= 1f) {
                        val target = (minutesCount + steps.toInt()).coerceIn(minValue, maxValue)
                        if (target != minutesCount) {
                            minutesCount = target
                            accumulated += steps.toInt().toFloat()
                            onMinutesChanged?.invoke(minutesCount)
                            invalidate()
                        }
                    }
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (active) {
                    active = false
                    onTimePicked?.invoke()
                    return true
                }
            }
        }
        return super.onTouchEvent(event)
    }
}
