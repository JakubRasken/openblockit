package it.mirko.widget.display

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.openblockit.R

class TileDisplay @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var text: String = ""
    private var tileColor: Int = context.getColor(R.color.colorOnSurface)
    private var contentGravity: Int = 17

    init {
        paint.color = tileColor
    }

    fun setTileColor(color: Int) {
        tileColor = color
        paint.color = color
        invalidate()
    }

    fun setContentGravity(gravity: Int) {
        contentGravity = gravity
        invalidate()
    }

    fun f(value: String, animated: Boolean) {
        text = value
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val tile = 6f * resources.displayMetrics.density
        val gap = 2f * resources.displayMetrics.density
        val columns = 5
        val rows = 7
        val totalW = columns * tile + (columns - 1) * gap
        val totalH = rows * tile + (rows - 1) * gap
        val startX = (width - totalW) / 2f
        val startY = (height - totalH) / 2f
        for (row in 0 until rows) {
            for (col in 0 until columns) {
                val x = startX + col * (tile + gap)
                val y = startY + row * (tile + gap)
                canvas.drawRoundRect(x, y, x + tile, y + tile, tile * 0.2f, tile * 0.2f, paint)
            }
        }
    }
}
