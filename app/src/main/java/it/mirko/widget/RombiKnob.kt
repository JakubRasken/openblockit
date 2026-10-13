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

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = context.getColor(R.color.colorPrimaryContainer) }
    private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.colorOnSurface)
        style = Paint.Style.STROKE
        strokeWidth = 3f * resources.displayMetrics.density
    }

    var value: Int = 0
        private set
    var maxValue: Int = 100
    var onValueChanged: ((Int) -> Unit)? = null

    fun setValue(newValue: Int) {
        value = newValue.coerceIn(0, maxValue)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val radius = minOf(cx, cy) * 0.86f
        canvas.drawCircle(cx, cy, radius, ring)
        val angle = (value / maxValue.toFloat()) * 2f * Math.PI - Math.PI / 2
        val dx = cx + (radius * 0.72f * kotlin.math.cos(angle)).toFloat()
        val dy = cy + (radius * 0.72f * kotlin.math.sin(angle)).toFloat()
        canvas.drawCircle(dx, dy, radius * 0.2f, paint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_MOVE || event.actionMasked == MotionEvent.ACTION_DOWN) {
            val delta = event.y - height / 2f
            val next = ((1f - (delta / (height / 2f))) * maxValue / 2f + value).toInt().coerceIn(0, maxValue)
            if (next != value) {
                value = next
                onValueChanged?.invoke(value)
                invalidate()
            }
            return true
        }
        return super.onTouchEvent(event)
    }
}
