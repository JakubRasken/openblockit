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

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private var cellSize: Int = (2f * resources.displayMetrics.density).roundToInt()
    private var multiplier: Int = 2
    private var animateChanges = false
    private var isDecMin = false
    private var isHour = false
    private var currentValue = 0
    private var colon = false

    private val segments: Map<Int, List<FloatArray>> = mapOf(
        0 to listOf(TOP, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, BOTTOM),
        1 to listOf(TOP_RIGHT, BOTTOM_RIGHT),
        2 to listOf(TOP, TOP_RIGHT, MIDDLE, BOTTOM_LEFT, BOTTOM),
        3 to listOf(TOP, TOP_RIGHT, MIDDLE, BOTTOM_RIGHT, BOTTOM),
        4 to listOf(TOP_LEFT, TOP_RIGHT, MIDDLE, BOTTOM_RIGHT),
        5 to listOf(TOP, TOP_LEFT, MIDDLE, BOTTOM_RIGHT, BOTTOM),
        6 to listOf(TOP, TOP_LEFT, MIDDLE, BOTTOM_LEFT, BOTTOM_RIGHT, BOTTOM),
        7 to listOf(TOP, TOP_RIGHT, BOTTOM_RIGHT),
        8 to listOf(TOP, TOP_LEFT, TOP_RIGHT, MIDDLE, BOTTOM_LEFT, BOTTOM_RIGHT, BOTTOM),
        9 to listOf(TOP, TOP_LEFT, TOP_RIGHT, MIDDLE, BOTTOM_RIGHT),
    )

    private var glyph: List<FloatArray> = segments[0]!!

    private val holes: Map<Int, List<FloatArray>> = mapOf(
        0 to listOf(floatArrayOf(1f, 1f, 2f, 4f)),
        6 to listOf(floatArrayOf(1f, 1f, 2f, 2f)),
        8 to listOf(floatArrayOf(1f, 1f, 2f, 2f), floatArrayOf(1f, 3f, 2f, 4f)),
        9 to listOf(floatArrayOf(1f, 3f, 2f, 4f)),
    )

    fun setValue(value: Int) {
        currentValue = value % 10
        glyph = segments[currentValue] ?: segments[0]!!
        colon = false
        invalidate()
    }

    fun setColon() {
        colon = true
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
        val cell = cellSize.toFloat()
        if (cell <= 0f) return
        paint.style = Paint.Style.FILL

        val inkWidth = cell * 3f
        val inkHeight = cell * 5f
        val originX = (width - inkWidth) / 2f
        val originY = (height - inkHeight) / 2f

        if (colon) {
            val dot = cell * 0.94f
            canvas.drawRect(originX + cell, originY + cell * 1.15f, originX + cell + dot, originY + cell * 1.15f + dot, paint)
            canvas.drawRect(originX + cell, originY + cell * 3.05f, originX + cell + dot, originY + cell * 3.05f + dot, paint)
            return
        }

        val path = glyphPath(originX, originY, cell)
        canvas.drawPath(path, paint)
    }

    private fun glyphPath(originX: Float, originY: Float, cell: Float): android.graphics.Path {
        val path = android.graphics.Path()

        fun rect(x1: Int, y1: Int, x2: Int, y2: Int) {
            val piece = android.graphics.Path()
            piece.addRect(
                originX + x1 * cell,
                originY + y1 * cell,
                originX + x2 * cell,
                originY + y2 * cell,
                android.graphics.Path.Direction.CW,
            )
            path.op(piece, android.graphics.Path.Op.UNION)
        }

        glyph.forEach { segment ->
            val x1 = segment[0].toInt()
            val y1 = segment[1].toInt()
            val x2 = segment[2].toInt()
            val y2 = segment[3].toInt()
            if (y1 == y2) rect(0, y1, 3, y1 + 1) else rect(x1, y1, x1 + 1, y2)
        }

        holes[currentValue]?.forEach { hole ->
            val cut = android.graphics.Path()
            cut.addRect(
                originX + hole[0] * cell,
                originY + hole[1] * cell,
                originX + hole[2] * cell,
                originY + hole[3] * cell,
                android.graphics.Path.Direction.CW,
            )
            path.op(cut, android.graphics.Path.Op.DIFFERENCE)
        }
        return path
    }

    private companion object {
        const val CELL_ASPECT = 1.0f

        val TOP = floatArrayOf(0f, 0f, 2f, 0f)
        val TOP_LEFT = floatArrayOf(0f, 0f, 0f, 2f)
        val TOP_RIGHT = floatArrayOf(2f, 0f, 2f, 2f)
        val MIDDLE = floatArrayOf(0f, 2f, 2f, 2f)
        val BOTTOM_LEFT = floatArrayOf(0f, 2f, 0f, 4f)
        val BOTTOM_RIGHT = floatArrayOf(2f, 2f, 2f, 4f)
        val BOTTOM = floatArrayOf(0f, 4f, 2f, 4f)
    }
}
