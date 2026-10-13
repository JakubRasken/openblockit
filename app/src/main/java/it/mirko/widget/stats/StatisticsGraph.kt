package it.mirko.widget.stats

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.openblockit.R

class StatisticsGraph @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = context.getColor(R.color.colorPrimaryContainer) }
    private val scalePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.colorOnSurface)
        alpha = 60
        strokeWidth = 2f
    }

    var values: List<Float> = emptyList()
        set(value) {
            field = value
            invalidate()
        }

    var onValueSelected: ((Int) -> Unit)? = null

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (values.isEmpty()) return
        val max = (values.maxOrNull() ?: 1f).coerceAtLeast(1f)
        val gap = width * 0.02f
        val barW = ((width - gap * (values.size - 1)) / values.size).coerceAtLeast(2f)
        values.forEachIndexed { i, v ->
            val h = (height * (v / max)).coerceAtLeast(if (v > 0f) 4f else 2f)
            val left = i * (barW + gap)
            canvas.drawRoundRect(left, height - h, left + barW, height.toFloat(), barW * 0.35f, barW * 0.35f, barPaint)
        }
        canvas.drawLine(0f, height - 1f, width.toFloat(), height - 1f, scalePaint)
    }
}
