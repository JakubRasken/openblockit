package it.mirko.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.openblockit.R

class BagTiles @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private val primary = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = context.getColor(R.color.colorPrimaryContainer) }
    private val plain = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = context.getColor(R.color.colorOnSurface) }
    private var selected: Int = -1

    fun select(index: Int) {
        selected = index
        invalidate()
    }

    fun setDemoLoop(value: Boolean) = Unit

    fun d(colorA: Int, colorB: Int) = Unit

    fun setAddonDemoLoop(value: Boolean) = Unit

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val bags = 3
        val bagW = width / bags.toFloat()
        for (bag in 0 until bags) {
            val ox = bag * bagW + bagW * 0.14f
            val w = bagW * 0.72f
            val paint = if (bag == selected) primary else plain
            val bars = listOf(0.44f, 0.62f, 0.4f, 0.2f)
            val barW = w * 0.2f
            val gap = w * 0.06f
            val baseY = height * 0.72f
            var x = ox
            bars.forEach { hf ->
                val h = height * hf
                canvas.drawRoundRect(x, baseY - h, x + barW, baseY, barW * 0.35f, barW * 0.35f, paint)
                x += barW + gap
            }
            val sq = w * 0.16f
            var sx = ox + w * 0.06f
            repeat(3) {
                canvas.drawRoundRect(sx, baseY + height * 0.06f, sx + sq, baseY + height * 0.06f + sq, sq * 0.25f, sq * 0.25f, paint)
                sx += sq + w * 0.1f
            }
        }
    }
}
