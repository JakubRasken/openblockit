package it.mirko.widget.navigation

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import com.google.android.material.card.MaterialCardView
import com.openblockit.R

class AltNavigationItem @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    var menuId: Int = 0
    val pill: MaterialCardView = MaterialCardView(context)
    private val iconView = NavIconView(context)

    init {
        addView(
            pill,
            LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply { gravity = Gravity.CENTER },
        )
        pill.addView(iconView, LayoutParams(dp(24), dp(24)).apply { gravity = Gravity.CENTER })
        pill.radius = dp(16).toFloat()
        pill.setCardBackgroundColor(context.getColor(R.color.colorPrimaryContainer))
    }

    /** Sets the icon shown inside the pill. */
    fun setIconIndex(index: Int) {
        iconView.index = index
        iconView.invalidate()
    }

    fun setMenuItemSelected(selected: Boolean) {
        pill.setCardBackgroundColor(
            if (selected) context.getColor(R.color.colorPrimaryContainer)
            else android.graphics.Color.TRANSPARENT
        )
        iconView.tint = if (selected) context.getColor(R.color.colorOnPrimaryContainer)
        else context.getColor(R.color.colorPrimaryContainer)
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}

class NavIconView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    var index: Int = 0
    var tint: Int = 0
        set(value) {
            field = value
            invalidate()
        }

    override fun onDraw(canvas: android.graphics.Canvas) {
        super.onDraw(canvas)
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = tint
            style = android.graphics.Paint.Style.STROKE
            strokeWidth = 2f * resources.displayMetrics.density
        }
        val w = width.toFloat()
        val h = height.toFloat()
        when (index) {
            0 -> {
                canvas.drawCircle(w / 2f, h / 2f, w * 0.32f, paint)
                canvas.drawLine(w / 2f, h * 0.18f, w / 2f, h * 0.34f, paint)
                canvas.drawLine(w * 0.34f, h * 0.2f, w * 0.66f, h * 0.2f, paint)
                canvas.drawLine(w / 2f, h / 2f, w / 2f, h * 0.32f, paint)
            }
            1 -> {
                val left = w * 0.14f
                canvas.drawRoundRect(left, h * 0.32f, w - left, h * 0.82f, 8f, 8f, paint)
                canvas.drawLine(w * 0.38f, h * 0.32f, w * 0.38f, h * 0.2f, paint)
                canvas.drawLine(w * 0.62f, h * 0.32f, w * 0.62f, h * 0.2f, paint)
                canvas.drawLine(w * 0.38f, h * 0.2f, w * 0.62f, h * 0.2f, paint)
            }
            2 -> {
                val fill = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { color = tint }
                val s = w * 0.32f
                val gap = w * 0.1f
                val start = (w - (s * 2 + gap)) / 2f
                canvas.drawRoundRect(start, start, start + s, start + s, 4f, 4f, fill)
                canvas.drawRoundRect(start + s + gap, start, start + s * 2 + gap, start + s, 4f, 4f, fill)
                canvas.drawRoundRect(start, start + s + gap, start + s, start + s * 2 + gap, 4f, 4f, fill)
                canvas.drawRoundRect(start + s + gap, start + s + gap, start + s * 2 + gap, start + s * 2 + gap, 4f, 4f, fill)
            }
            else -> {
                val fill = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { color = tint }
                val barW = w * 0.16f
                val heights = listOf(0.45f, 0.75f, 0.6f, 0.9f)
                heights.forEachIndexed { i, hf ->
                    val barH = h * hf
                    val left = w * 0.12f + i * (barW + w * 0.06f)
                    canvas.drawRoundRect(left, h - barH - h * 0.1f, left + barW, h - h * 0.1f, barW / 2f, barW / 2f, fill)
                }
            }
        }
    }
}
