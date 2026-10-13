package it.mirko.widget

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.PathInterpolator
import com.openblockit.R
import java.text.DateFormatSymbols
import java.util.Calendar
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class DayFaders @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private val letterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = runCatching { resources.getFont(R.font.google_sans_bold) }
            .getOrDefault(Typeface.DEFAULT_BOLD)
        textAlign = Paint.Align.CENTER
    }
    private val faderPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()

    private val activeColor = context.getColor(R.color.colorPrimaryContainer)
    private val idleColor = context.getColor(R.color.colorOnSurfaceVariant)
    private val textColor = context.getColor(R.color.colorOnSurface)
    private val trackColor = context.getColor(R.color.colorSurface)

    private val progress = FloatArray(7)
    private val animators = arrayOfNulls<ValueAnimator>(7)
    private val letterArray = Array(7) { "" }
    private val dayArray = IntArray(7)

    private var trackWidth = 0f
    private var trackHeight = 0f
    private var textHeight = 0f
    private var topOffset = 0f
    private var faderTop = 0f

    private val standard = PathInterpolator(0.2f, 0f, 0f, 1f)

    var activeDays: Set<Int> = setOf(1, 2, 3, 4, 5, 6, 7)
        set(value) {
            field = value
            for (i in 0 until 7) {
                progress[i] = if (value.contains(dayArray[i])) 1f else 0f
            }
            invalidate()
        }
    var onDayToggled: ((Int) -> Unit)? = null

    init {
        val symbols = DateFormatSymbols()
        val firstDay = Calendar.getInstance().firstDayOfWeek
        val calendar = Calendar.getInstance()
        for (i in 0 until 7) {
            val day = (((firstDay - 1) + i) % 7) + 1
            dayArray[i] = day
            calendar.set(Calendar.DAY_OF_WEEK, day)
            val name = symbols.shortWeekdays[day]?.trim().orEmpty()
            letterArray[i] = if (name.isEmpty()) "" else name.substring(0, 1).uppercase()
        }
        isClickable = true
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val column = ((width - paddingLeft - paddingRight) / 7f) * 0.74f
        var nominal = column * 2.05f + column * 0.28f + column * 0.52f + paddingTop + paddingBottom
        nominal = min(nominal, resources.displayMetrics.heightPixels * 0.135f)
        setMeasuredDimension(width, resolveSize(nominal.roundToInt(), heightMeasureSpec))
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width == 0 || height == 0) return

        val column = (width - paddingLeft - paddingRight) / 7f
        trackWidth = column * 0.74f
        val nominalTrack = trackWidth * 2.05f
        textHeight = trackWidth * 0.52f
        var gap = trackWidth * 0.28f
        var total = textHeight + gap + nominalTrack

        val available = (height - paddingTop - paddingBottom).toFloat()
        if (total > available && total > 0f) {
            val scale = available / total
            textHeight *= scale
            trackWidth *= scale
            gap *= scale
            total = available
        } else if (available > total) {
            val grown = min((available - total) + nominalTrack, trackWidth * 3.5f)
            if (grown > nominalTrack) {
                trackHeight = grown
                total = textHeight + gap + grown
            } else {
                trackHeight = nominalTrack
            }
        } else {
            trackHeight = nominalTrack
        }
        if (trackHeight <= 0f) trackHeight = nominalTrack

        topOffset = max(0f, (available - total) * 0.5f) + paddingTop
        faderTop = topOffset + textHeight + gap

        letterPaint.textSize = textHeight * 0.86f
        for (i in 0 until 7) {
            val active = activeDays.contains(dayArray[i])
            letterPaint.color = tint(textColor, if (active) 165 else 95)
            val centerX = (i + 0.5f) * column + paddingLeft
            canvas.drawText(letterArray[i], centerX, textHeight * 0.86f + topOffset, letterPaint)

            val left = centerX - trackWidth / 2f
            val right = centerX + trackWidth / 2f
            val bottom = faderTop + trackHeight
            faderPaint.color = trackColor
            rect.set(left, faderTop, right, bottom)
            canvas.drawRoundRect(rect, trackWidth * 0.24f, trackWidth * 0.24f, faderPaint)

            val inset = max(1.5f, trackWidth * 0.11f)
            val half = (bottom - faderTop) / 2f
            val fillTop = half + trackHeight * 0f + lerp(half, inset, 1f - progress[i])
            faderPaint.color = if (active) tint(activeColor, if (isEnabled) 110 else 130)
            else tint(idleColor, if (isEnabled) 150 else 90)
            rect.set(left + inset, fillTop, right - inset, half + fillTop - inset)
            canvas.drawRoundRect(rect, trackWidth * 0.16f, trackWidth * 0.16f, faderPaint)
        }
    }

    fun setDayActive(day: Int, active: Boolean) {
        val index = dayArray.indexOf(day)
        if (index < 0) return
        animators[index]?.cancel()
        animators[index] = ValueAnimator.ofFloat(progress[index], if (active) 1f else 0f).apply {
            duration = 190L
            interpolator = standard
            addUpdateListener { anim ->
                progress[index] = anim.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_UP) {
            val index = (event.x / (width / 7f)).toInt().coerceIn(0, 6)
            onDayToggled?.invoke(dayArray[index])
            return true
        }
        return true
    }

    private fun lerp(start: Float, stop: Float, fraction: Float) = start + (stop - start) * fraction

    private fun tint(color: Int, alpha: Int): Int =
        (color and 0x00FFFFFF) or (alpha.coerceIn(0, 255) shl 24)
}
