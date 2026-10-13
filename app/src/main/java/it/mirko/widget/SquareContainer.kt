package it.mirko.widget

import android.content.Context
import android.content.res.Resources
import android.util.AttributeSet
import android.view.ViewGroup
import androidx.appcompat.widget.LinearLayoutCompat

class SquareContainer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayoutCompat(context, attrs, defStyleAttr) {

    private var ratio: Float = 1f

    init {
        if (attrs != null) {
            val a = context.obtainStyledAttributes(attrs, intArrayOf(com.openblockit.R.attr.squareRatio))
            ratio = a.getFloat(0, 1f)
            a.recycle()
        }
    }

    fun setRatio(value: Float) {
        ratio = value
        requestLayout()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val metrics = Resources.getSystem().displayMetrics
        val minSide = minOf(metrics.widthPixels, metrics.heightPixels)
        val lp: ViewGroup.LayoutParams = layoutParams ?: return
        val targetWidth = minSide
        val targetHeight = (minSide * ratio).toInt()
        if (lp.width != targetWidth || lp.height != targetHeight) {
            lp.width = targetWidth
            lp.height = targetHeight
            post { requestLayout() }
        }
    }
}
