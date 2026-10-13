package it.mirko.widget.graph

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.openblockit.R

class LevelGraph @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = context.getColor(R.color.colorPrimaryContainer) }
    private val mute = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = context.getColor(R.color.colorSurfaceBright) }

    var streak: Int = 0
        set(value) {
            field = value
            invalidate()
        }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val perRow = 7
        val cells = ((streak / perRow) + 1) * perRow
        val rows = cells / perRow
        if (rows == 0) return
        val gap = width * 0.02f
        val cellW = (width - gap * (perRow - 1)) / perRow
        val cellH = (height - gap * (rows - 1)) / rows
        for (i in 0 until cells) {
            val row = rows - 1 - i / perRow
            val col = i % perRow
            val x = col * (cellW + gap)
            val y = row * (cellH + gap)
            val paint = if (i < streak) fill else mute
            canvas.drawRoundRect(x, y, x + cellW, y + cellH, cellW * 0.18f, cellW * 0.18f, paint)
        }
    }
}
