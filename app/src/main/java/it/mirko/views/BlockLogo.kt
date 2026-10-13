package it.mirko.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.SystemClock
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import java.util.Random

open class BlockLogo @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private val inkPaint = Paint().apply { isAntiAlias = true }
    private val accentPaint = Paint().apply { isAntiAlias = true }
    private val random = Random()
    private val rect = RectF()

    private var widthPx = 0
    private var heightPx = 0
    private var lastBlink = 0L
    private var blinkCol = 0
    private var blinkRow = 0
    private var inkColor: Int

    private var drawCellW = 0
    private var drawCellH = 0

    private val cellW: Int
    private val cellH: Int
    private val cellGap: Int
    private val corner: Int

    init {
        inkColor = context.getColor(com.openblockit.R.color.colorOnSurface)
        inkPaint.color = inkColor
        accentPaint.color = context.getColor(com.openblockit.R.color.colorPrimaryContainer)
        val dm = resources.displayMetrics
        cellGap = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 0f, dm).toInt()
        cellW = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5.75f, dm).toInt()
        cellH = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5f, dm).toInt()
        corner = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 0f, dm).toInt()
        drawCellW = cellW
        drawCellH = cellH
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        widthPx = w
        heightPx = h
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (isShown) postInvalidateDelayed(33L)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!isShown || widthPx == 0 || heightPx == 0) return
        val now = SystemClock.elapsedRealtime()
        if (now > lastBlink + 2600) {
            lastBlink = now
            var guard = 0
            while (guard++ < 60) {
                val c = random.nextInt(COLUMNS)
                val r = random.nextInt(ROWS)
                if (MASKS[r][c] == '1' && !(c == blinkCol && r == blinkRow)) {
                    blinkCol = c
                    blinkRow = r
                    break
                }
            }
        }
        val elapsed = (now - lastBlink).toInt()
        for (row in 0 until ROWS) {
            for (col in 0 until COLUMNS) {
                if (MASKS[row][col] != '1') continue
                val x = (drawCellW + cellGap) * col.toFloat()
                val y = (drawCellH + cellGap) * row.toFloat()
                rect.set(x, y, x + drawCellW, y + drawCellH)
                canvas.drawRoundRect(rect, corner.toFloat(), corner.toFloat(), inkPaint)
                if (now <= lastBlink + 2300 && blinkCol == col && blinkRow == row) {
                    accentPaint.alpha = when {
                        elapsed < 1000 -> elapsed * 255 / 1000
                        elapsed < 1300 -> 255
                        else -> 255 - ((elapsed - 1300) * 255 / 1000)
                    }
                    canvas.drawRoundRect(rect, corner.toFloat(), corner.toFloat(), accentPaint)
                }
            }
        }
        postInvalidateDelayed(33L)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val natural = COLUMNS * cellW + (COLUMNS - 1) * cellGap
        var cw = cellW
        var ch = cellH
        val available = MeasureSpec.getSize(widthMeasureSpec)
        if (MeasureSpec.getMode(widthMeasureSpec) != MeasureSpec.UNSPECIFIED && natural > available && natural > 0) {
            val scale = available.toFloat() / natural
            cw = (cellW * scale).toInt().coerceAtLeast(1)
            ch = (cellH * scale).toInt().coerceAtLeast(1)
        }
        drawCellW = cw
        drawCellH = ch
        val width = resolveSize(COLUMNS * cw + (COLUMNS - 1) * cellGap, widthMeasureSpec)
        val height = resolveSize(ROWS * ch + (ROWS - 1) * cellGap, heightMeasureSpec)
        setMeasuredDimension(width, height)
    }

    fun setInkColor(color: Int) {
        inkColor = color
        inkPaint.color = color
        invalidate()
    }

    private companion object {
        const val COLUMNS = 39
        const val ROWS = 4

        val MASKS = arrayOf(
            "0000110000000000" + "10001000000000101010010",
            "1110101011101110" + "11101011101110110000111",
            "1010111011001010" + "10101010101000101010010",
            "1110100111101010" + "11101011101110101010010",
        )
    }
}
