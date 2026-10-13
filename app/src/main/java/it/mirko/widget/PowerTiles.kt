package it.mirko.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.openblockit.R

class PowerTiles @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = context.getColor(R.color.colorPrimaryContainer) }

    var litCount: Int = 4
        set(value) {
            field = value
            invalidate()
        }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val tiles = 4
        val gap = width * 0.06f
        val tile = (width - gap * (tiles - 1)) / tiles
        for (i in 0 until tiles) {
            val left = i * (tile + gap)
            paint.alpha = if (i < litCount) 255 else 60
            canvas.drawRoundRect(left, 0f, left + tile, height.toFloat(), tile * 0.22f, tile * 0.22f, paint)
        }
    }
}
