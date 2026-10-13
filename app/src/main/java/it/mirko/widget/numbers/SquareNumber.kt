package it.mirko.widget.numbers

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.math.roundToInt

class SquareNumber @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }

    private var cellSize: Int = (2f * resources.displayMetrics.density).roundToInt()
    private var multiplier: Int = 2
    private var animateChanges = false
    private var isDecMin = false
    private var isHour = false
    private var currentValue = 0

    private val masks: Map<Int, List<String>> = mapOf(
        0 to listOf("111", "101", "101", "101", "111"),
        1 to listOf("001", "001", "001", "001", "001"),
        2 to listOf("111", "001", "111", "100", "111"),
        3 to listOf("111", "001", "111", "001", "111"),
        4 to listOf("101", "101", "111", "001", "001"),
        5 to listOf("111", "100", "111", "001", "111"),
        6 to listOf("111", "100", "111", "101", "111"),
        7 to listOf("111", "001", "001", "001", "001"),
        8 to listOf("111", "101", "111", "101", "111"),
        9 to listOf("111", "101", "111", "001", "111"),
    )

    private var glyph: List<String> = masks[0]!!

    fun setValue(value: Int) {
        currentValue = value % 10
        glyph = masks[currentValue] ?: masks[0]!!
        invalidate()
    }

    fun setTextColor(color: Int) {
        paint.color = color
        invalidate()
    }

    fun setSizeMultiplier(value: Int) {
        multiplier = value
        cellSize = (value * resources.displayMetrics.density).roundToInt()
        
        requestLayout()
        invalidate()
    }

    fun setAnimate(value: Boolean) {
        animateChanges = value
    }

    fun setIsDecMin(value: Boolean) {
        isDecMin = value
    }

    fun setIsHour(value: Boolean) {
        isHour = value
    }

    fun getSize(): Int = cellSize

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = resolveSize(cellSize * 3, widthMeasureSpec)
        val height = resolveSize((cellSize * 5 * CELL_ASPECT).toInt(), heightMeasureSpec)
        setMeasuredDimension(width, height)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cellW = width / 3f - CELL_GAP
        val cellH = cellW * CELL_ASPECT
        val radius = cellW * 0.14f
        val offsetX = (width - cellW * 3 - CELL_GAP * 2) / 2f
        val offsetY = (height - cellH * glyph.size - CELL_GAP * (glyph.size - 1)) / 2f
        glyph.forEachIndexed { row, line ->
            line.forEachIndexed { col, ch ->
                if (ch == '1') {
                    val left = offsetX + col * (cellW + CELL_GAP)
                    val top = offsetY + row * (cellH + CELL_GAP)
                    canvas.drawRoundRect(left, top, left + cellW, top + cellH, radius, radius, paint)
                }
            }
        }
    }

    private companion object {
        const val CELL_ASPECT = 1.0f
        const val CELL_GAP = 2f
    }
}
