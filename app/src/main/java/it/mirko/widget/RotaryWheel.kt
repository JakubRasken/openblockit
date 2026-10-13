package it.mirko.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.openblockit.R
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

class RotaryWheel @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.colorOnSurface)
        strokeCap = Paint.Cap.ROUND
        strokeWidth = resources.displayMetrics.density * 1.4f
    }
    private val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.colorPrimaryContainer)
    }

    private val baseColor = context.getColor(R.color.colorOnSurface)

    var barCount: Int = 1
        private set
    var selected: Int = 0
        private set
    var highlighted: Int = 0
        private set

    var onBarSelected: ((Int) -> Unit)? = null

    private val density = resources.displayMetrics.density

    fun setBars(count: Int, selectedIndex: Int, highlightedIndex: Int) {
        barCount = max(1, count)
        selected = max(0, min(barCount - 1, selectedIndex))
        highlighted = max(0, min(barCount - 1, highlightedIndex))
        invalidate()
    }

    fun setSelected(index: Int, notify: Boolean = true) {
        val next = max(0, min(barCount - 1, index))
        if (next == selected) return
        selected = next
        invalidate()
        if (notify) onBarSelected?.invoke(selected)
    }

    private fun fraction(index: Int): Float {
        if (barCount <= 1) return 0f
        return index.toFloat() / (barCount - 1).toFloat()
    }

    private fun angleFor(fraction: Float): Float =
        Math.toRadians((fraction.coerceIn(0f, 1f) * 240f + 150f).toDouble()).toFloat()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val radius = min(width, height) / 2f - 10f * density
        if (radius <= 0f) return

        for (i in 0 until barCount) {
            val angle = angleFor(fraction(i))
            tickPaint.alpha = if (i <= highlighted) 55 else 12
            canvas.drawLine(
                cx + cos(angle) * radius * 0.93f,
                cy + sin(angle) * radius * 0.93f,
                cx + cos(angle) * radius,
                cy + sin(angle) * radius,
                tickPaint,
            )
        }

        val selectedAngle = angleFor(fraction(selected))
        canvas.drawCircle(
            cx + cos(selectedAngle) * radius * 0.79f,
            cy + sin(selectedAngle) * radius * 0.79f,
            11f * density,
            accentPaint,
        )
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                val cx = width / 2f
                val cy = height / 2f
                if (cx <= 0f || cy <= 0f) return true
                var degrees = Math.toDegrees(kotlin.math.atan2((event.y - cy).toDouble(), (event.x - cx).toDouble())).toFloat()
                if (degrees < 0f) degrees += 360f
                var within = degrees - 150f
                if (within < 0f) within += 360f
                if (within > 240f) return true
                val index = if (barCount <= 1) 0 else (within / 240f * (barCount - 1)).roundToInt()
                setSelected(index)
                return true
            }
        }
        return super.onTouchEvent(event)
    }
}
