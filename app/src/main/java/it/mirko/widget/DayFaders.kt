package it.mirko.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.openblockit.R

class DayFaders @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private val on = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = context.getColor(R.color.colorPrimaryContainer) }
    private val off = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = context.getColor(R.color.colorSurfaceVariant) }

    var activeDays: Set<Int> = setOf(1, 2, 3, 4, 5, 6, 7)
        set(value) {
            field = value
            invalidate()
        }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cols = 7
        val gap = width * 0.02f
        val cellW = (width - gap * (cols - 1)) / cols
        for (i in 0 until cols) {
            val left = i * (cellW + gap)
            canvas.drawRoundRect(left, 0f, left + cellW, height.toFloat(), cellW * 0.24f, cellW * 0.24f, if (activeDays.contains(i + 1)) on else off)
        }
    }
}
