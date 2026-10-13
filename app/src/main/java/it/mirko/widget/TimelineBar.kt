package it.mirko.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.openblockit.R

class TimelineBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.colorOnSurface)
        alpha = 90
        strokeWidth = 3f
    }
    private val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = context.getColor(R.color.colorPrimaryContainer) }

    var startMinutes: Int = 540
    var endMinutes: Int = 720

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val count = 48
        val step = width / count.toFloat()
        for (i in 0 until count) {
            val x = i * step + step / 2f
            canvas.drawLine(x, height * 0.2f, x, height * 0.8f, linePaint)
        }
        val startX = (startMinutes / 1440f) * width
        val endX = (endMinutes / 1440f) * width
        canvas.drawLine(startX, 0f, startX, height.toFloat(), accentPaint)
        canvas.drawLine(endX, 0f, endX, height.toFloat(), accentPaint)
    }
}
