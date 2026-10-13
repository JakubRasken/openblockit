package it.mirko.widget.numbers

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import kotlin.math.abs
import kotlin.math.roundToInt

class SquareNumber @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    private var cellSize: Int = 0
    private var multiplier: Int = 2
    private var animateChanges = false
    private var isDecMin = false
    private var isHour = false
    private var currentValue = 0

    init {
        setLayoutDirection(LAYOUT_DIRECTION_LTR)
        cellSize = (multiplier * resources.displayMetrics.density).roundToInt()
        rebuild()
    }

    private fun rebuild() {
        removeAllViews()
        val lp = LayoutParams(cellSize, cellSize).apply { gravity = Gravity.START or Gravity.TOP }
        repeat(13) {
            val v = View(context)
            v.layoutParams = lp
            v.setBackgroundColor(Color.WHITE)
            addView(v)
        }
        for (i in 0 until childCount) {
            getChildAt(i).translationX = cellSize.toFloat()
            getChildAt(i).translationY = cellSize * 2f
        }
    }

    private fun digitCells(digit: Int): List<Pair<Int, Int>> {
        val grid = when (digit) {
            0 -> listOf(
                "111", "101", "101", "101", "111",
            )
            1 -> listOf(
                "001", "001", "001", "001", "001",
            )
            2 -> listOf(
                "111", "001", "111", "100", "111",
            )
            3 -> listOf(
                "111", "001", "111", "001", "111",
            )
            4 -> listOf(
                "101", "101", "111", "001", "001",
            )
            5 -> listOf(
                "111", "100", "111", "001", "111",
            )
            6 -> listOf(
                "111", "100", "111", "101", "111",
            )
            7 -> listOf(
                "111", "001", "001", "001", "001",
            )
            8 -> listOf(
                "111", "101", "111", "101", "111",
            )
            else -> listOf(
                "111", "101", "111", "001", "111",
            )
        }
        val cells = ArrayList<Pair<Int, Int>>()
        grid.forEachIndexed { row, line ->
            line.forEachIndexed { col, ch ->
                if (ch == '1') cells.add(col to row)
            }
        }
        return cells
    }

    private fun place(digit: Int) {
        val cells = digitCells(digit)
        for (i in 0 until childCount) {
            val view = getChildAt(i)
            view.animate().cancel()
            view.scaleX = 1f
            view.scaleY = 1f
            view.alpha = 1f
            if (i < cells.size) {
                view.translationX = (cells[i].first * cellSize).toFloat()
                view.translationY = (cells[i].second * cellSize).toFloat()
            } else {
                view.alpha = 0f
                view.translationX = 0f
                view.translationY = -cellSize.toFloat()
            }
        }
    }

    private fun placeAnimated(digit: Int) {
        val cells = digitCells(digit)
        for (i in 0 until childCount) {
            val view = getChildAt(i)
            val delayBase = if (isDecMin) 200L else 0L
            val delay = if (isHour) 500L else delayBase
            view.animate().setDuration(300L).setStartDelay(delay)
            if (i < cells.size) {
                view.alpha = 1f
                view.translationX = (cells[i].first * cellSize).toFloat()
                view.translationY = (cells[i].second * cellSize).toFloat()
            } else {
                view.alpha = 0f
                view.translationX = 0f
                view.translationY = -cellSize.toFloat()
            }
        }
    }

    fun setValue(value: Int) {
        currentValue = value % 10
        if (animateChanges) placeAnimated(currentValue) else place(currentValue)
    }

    fun setTextColor(color: Int) {
        for (i in 0 until childCount) getChildAt(i).setBackgroundColor(color)
    }

    fun setSizeMultiplier(value: Int) {
        multiplier = value
        cellSize = (value * resources.displayMetrics.density).roundToInt()
        val lp = LayoutParams(cellSize, cellSize).apply { gravity = Gravity.START or Gravity.TOP }
        for (i in 0 until childCount) getChildAt(i).layoutParams = lp
        layoutParams?.let {
            it.width = cellSize * 3
            it.height = cellSize * 5
            layoutParams = it
        }
        requestLayout()
        place(currentValue)
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
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        layoutParams?.let {
            it.width = cellSize * 3
            it.height = cellSize * 5
        }
    }
}
