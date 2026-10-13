package it.mirko.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.openblockit.R
import java.text.DateFormatSymbols

class DayFaders @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.colorOnSurfaceVariant)
        textAlign = Paint.Align.CENTER
        textSize = 13f * resources.displayMetrics.density
        typeface = resources.getFont(R.font.google_sans_regular)
    }
    private val onPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.colorPrimaryContainer)
    }
    private val offPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.colorSurface)
    }
    private val track = RectF()

    var activeDays: Set<Int> = setOf(1, 2, 3, 4, 5, 6, 7)
        set(value) {
            field = value
            invalidate()
        }
    var onDayToggled: ((Int) -> Unit)? = null

    private val letters: List<String> by lazy {
        val symbols = DateFormatSymbols()
        val order = listOf(2, 3, 4, 5, 6, 7, 1)
        order.map { index ->
            val name = symbols.shortWeekdays[index].trim()
            if (name.isEmpty()) "" else name.substring(0, 1).uppercase()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = resolveSize(suggestedMinimumWidth, widthMeasureSpec)
        val height = resolveSize((150 * resources.displayMetrics.density).toInt(), heightMeasureSpec)
        setMeasuredDimension(width, height)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val colWidth = width / 7f
        val letterY = 26f * resources.displayMetrics.density
        val trackTop = 44f * resources.displayMetrics.density
        val trackBottom = height - 8f * resources.displayMetrics.density
        val trackWidth = colWidth * 0.56f
        for (i in 0 until 7) {
            val centerX = colWidth * i + colWidth / 2f
            canvas.drawText(letters[i], centerX, letterY, labelPaint)
            val left = centerX - trackWidth / 2f
            track.set(left, trackTop, left + trackWidth, trackBottom)
            val radius = trackWidth * 0.28f
            canvas.drawRoundRect(track, radius, radius, offPaint)
            if (activeDays.contains(i + 1)) {
                val fillBottom = trackTop + (trackBottom - trackTop) * 0.62f
                track.set(left, trackTop, left + trackWidth, fillBottom)
                canvas.drawRoundRect(track, radius, radius, onPaint)
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_UP) {
            val index = (event.x / (width / 7f)).toInt().coerceIn(0, 6) + 1
            onDayToggled?.invoke(index)
            return true
        }
        return true
    }
}
