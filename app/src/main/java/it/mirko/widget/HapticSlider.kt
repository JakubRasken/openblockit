package it.mirko.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
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

    private val spacing: Float = 14f * resources.displayMetrics.density
    private var accumulated = 0f
    private var lastY = 0f
    private var active = false

    var minValue = 5
    var maxValue = 720
    var step = 5
    var value = 30
        private set
    var onValueChanged: ((Int) -> Unit)? = null

    fun setValue(newValue: Int) {
        value = newValue.coerceIn(minValue, maxValue)
        invalidate()
    }

    fun setRange(min: Int, max: Int) {
        minValue = min
        maxValue = max
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val inset = paddingTop.coerceAtLeast(8)
        val lineW = width * 0.5f
        val startX = (width - lineW) / 2f
        val scroll = ((value - minValue) / step.toFloat()) * spacing
        var y = height / 2f - (scroll % spacing)
        while (y > inset) {
            if (y < height - inset) {
                canvas.drawLine(startX, y, startX + lineW, y, linePaint)
            }
            y -= spacing
        }
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
                accumulated = 0f
                lastY = event.y
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (active) {
                    accumulated += lastY - event.y
                    lastY = event.y
                    var changed = false
                    while (accumulated >= spacing) {
                        accumulated -= spacing
                        if (value + step <= maxValue) {
                            value += step
                            changed = true
                        }
                    }
                    while (accumulated <= -spacing) {
                        accumulated += spacing
                        if (value - step >= minValue) {
                            value -= step
                            changed = true
                        }
                    }
                    if (changed) {
                        onValueChanged?.invoke(value)
                        invalidate()
                    }
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                active = false
                return true
            }
        }
        return super.onTouchEvent(event)
    }
}
