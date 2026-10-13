package it.mirko.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.openblockit.R

class RotaryWheel @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.colorOnSurface)
        alpha = 90
        strokeWidth = 3f
    }
    private val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = context.getColor(R.color.colorPrimaryContainer) }

    var value: Int = 60
        private set
    var minValue = 15
    var maxValue = 720
    var onValueChanged: ((Int) -> Unit)? = null

    private var lastAngle = 0f

    fun setValue(newValue: Int) {
        value = newValue.coerceIn(minValue, maxValue)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val radius = minOf(cx, cy) * 0.9f
        val ticks = 60
        for (i in 0 until ticks) {
            val angle = (i.toFloat() / ticks) * 2f * Math.PI - Math.PI / 2
            val inner = if (i % 5 == 0) radius * 0.8f else radius * 0.88f
            val sx = cx + (inner * kotlin.math.cos(angle)).toFloat()
            val sy = cy + (inner * kotlin.math.sin(angle)).toFloat()
            val ex = cx + (radius * kotlin.math.cos(angle)).toFloat()
            val ey = cy + (radius * kotlin.math.sin(angle)).toFloat()
            canvas.drawLine(sx, sy, ex, ey, tickPaint)
        }
        val ratio = (value - minValue) / (maxValue - minValue).toFloat()
        val angle = ratio * 2f * Math.PI - Math.PI / 2
        val dotRadius = radius * 0.72f
        val dx = cx + (dotRadius * kotlin.math.cos(angle)).toFloat()
        val dy = cy + (dotRadius * kotlin.math.sin(angle)).toFloat()
        canvas.drawCircle(dx, dy, 10f * resources.displayMetrics.density, accentPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_MOVE -> {
                val cx = width / 2f
                val cy = height / 2f
                val angle = Math.toDegrees(kotlin.math.atan2((event.y - cy).toDouble(), (event.x - cx).toDouble())).toFloat()
                if (lastAngle != 0f) {
                    var delta = angle - lastAngle
                    if (delta > 180f) delta -= 360f
                    if (delta < -180f) delta += 360f
                    val step = ((maxValue - minValue) / 240).coerceAtLeast(1)
                    val next = (value + (delta / 3f).toInt() * step).coerceIn(minValue, maxValue)
                    if (next != value) {
                        value = next
                        onValueChanged?.invoke(value)
                        invalidate()
                    }
                }
                lastAngle = angle
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                lastAngle = 0f
                return true
            }
        }
        return super.onTouchEvent(event)
    }
}
