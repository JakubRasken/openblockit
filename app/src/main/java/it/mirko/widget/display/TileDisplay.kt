package it.mirko.widget.display

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.openblockit.R
import kotlin.math.roundToInt

class TileDisplay @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.colorOnSurface)
    }
    private var text: String = ""
    private var bars: FloatArray? = null
    private var tileColor: Int = context.getColor(R.color.colorOnSurface)
    private var contentGravity: Int = 17

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

    fun setText(value: String) {
        text = value
        invalidate()
    }

    fun setBars(values: FloatArray) {
        bars = values
        invalidate()
    }

    fun setTime(hours: Int, minutes: Int) {
        text = "%02d:%02d".format(hours, minutes)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val values = bars
        if (values != null) {
            drawBars(canvas, values)
            return
        }
        if (text.isEmpty()) return
        val rows = 6
        val glyphs = text.uppercase().map { GLYPHS[it] ?: GLYPHS[' ']!! }
        val columns = glyphs.map { glyph -> glyph.first.first().length }.sum() + (glyphs.size - 1)
        val tileByHeight = (height * 0.86f) / rows
        val tileByWidth = (width * 0.94f) / (columns * 1.16f)
        val tile = minOf(tileByHeight, tileByWidth)
        val gap = tile * 0.16f
        val totalWidth = columns * tile + (columns - 1) * gap
        var x = (width - totalWidth) / 2f
        val y0 = (height - (rows * tile + (rows - 1) * gap)) / 2f
        glyphs.forEachIndexed { index, glyph ->
            val rowsMask = glyph.first
            rowsMask.forEachIndexed { r, line ->
                line.forEachIndexed { c, ch ->
                    if (ch == '1') {
                        val left = x + c * (tile + gap)
                        val top = y0 + r * (tile + gap)
                        canvas.drawRoundRect(left, top, left + tile, top + tile, tile * 0.18f, tile * 0.18f, paint)
                    }
                }
            }
            val glyphWidth = rowsMask.first().length
            x += glyphWidth * tile + (glyphWidth - 1) * gap
            if (index != glyphs.lastIndex) x += tile + gap
        }
    }

    private fun drawBars(canvas: Canvas, values: FloatArray) {
        if (values.isEmpty() || width == 0 || height == 0) return
        val columns = values.size
        val columnWidth = width.toFloat() / columns
        val tile = columnWidth * 0.62f
        val gap = tile * 0.16f
        val rulerY = height - tile * 0.5f
        val baseline = rulerY - tile * 0.5f
        val maxValue = values.max().coerceAtLeast(1f)
        val usable = baseline - height * 0.08f
        val steps = (usable / (tile + gap)).toInt().coerceAtLeast(1)

        for (i in 0 until columns) {
            val centerX = columnWidth * i + columnWidth / 2f
            canvas.drawRoundRect(
                centerX - tile / 2f,
                rulerY - tile * 0.16f,
                centerX + tile / 2f,
                rulerY + tile * 0.16f,
                tile * 0.16f,
                tile * 0.16f,
                paint,
            )
        }

        for (i in 0 until columns) {
            val filled = ((values[i] / maxValue) * steps).roundToInt().coerceIn(0, steps)
            if (filled == 0) continue
            val centerX = columnWidth * i + columnWidth / 2f
            for (row in 0 until filled) {
                val bottom = baseline - row * (tile + gap)
                val top = bottom - tile
                if (top < 0f) break
                canvas.drawRoundRect(
                    centerX - tile / 2f,
                    top,
                    centerX + tile / 2f,
                    bottom,
                    tile * 0.18f,
                    tile * 0.18f,
                    paint,
                )
            }
        }
    }

    private companion object {
        val GLYPHS: Map<Char, Pair<List<String>, Boolean>> = mapOf(
            '0' to (listOf("0110", "1001", "1001", "1001", "1001", "0110") to false),
            '1' to (listOf("0000", "0010", "0110", "1010", "0010", "0010") to false),
            '2' to (listOf("0000", "1110", "0001", "0110", "1000", "1111") to false),
            '3' to (listOf("0000", "1110", "0001", "0111", "0001", "1110") to false),
            '4' to (listOf("0000", "1001", "1001", "0111", "0001", "0001") to false),
            '5' to (listOf("0000", "1111", "1000", "1110", "0001", "1110") to false),
            '6' to (listOf("0000", "0110", "1000", "1110", "1001", "0110") to false),
            '7' to (listOf("0000", "1110", "0001", "0001", "0001", "0001") to false),
            '8' to (listOf("0000", "0110", "1001", "0110", "1001", "0110") to false),
            '9' to (listOf("0000", "1110", "1001", "1111", "0001", "1110") to false),
            ':' to (listOf("0", "0", "1", "0", "1", "0") to true),
            ' ' to (listOf("00", "00", "00", "00", "00", "00") to false),
        )
    }
}
