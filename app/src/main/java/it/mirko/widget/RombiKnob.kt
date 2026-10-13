package it.mirko.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.openblockit.R

class RombiKnob @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.colorOnSurface)
        alpha = 90
        strokeWidth = 3f
    }
    private val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.colorPrimaryContainer)
        strokeWidth = 4f
    }

    var value: Int = 0
        private set
    var maxValue: Int = 1440
    var onValueChanged: ((Int) -> Unit)? = null

    fun setValue(newValue: Int) {
        value = newValue.coerceIn(0, maxValue)
        invalidate()
    }

    fun setMax(newMax: Int) {
        maxValue = newMax
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val count = 24
        val step = width / count.toFloat()
        val top = height * 0.22f
        val bottom = height * 0.78f
        for (i in 0 until count) {
            val x = i * step + step / 2f
            canvas.drawLine(x, top, x, bottom, tickPaint)
        }
        val ratio = if (maxValue > 0) value / maxValue.toFloat() else 0f
        val x = (width * ratio).coerceIn(2f, width - 2f)
        canvas.drawLine(x, height * 0.08f, x, height * 0.92f, accentPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                val ratio = (event.x / width.toFloat()).coerceIn(0f, 1f)
                val next = (ratio * maxValue).toInt()
                if (next != value) {
                    value = next
                    onValueChanged?.invoke(value)
                    invalidate()
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }
}
