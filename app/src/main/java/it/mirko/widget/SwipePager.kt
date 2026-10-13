package it.mirko.widget

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout

class SwipePager @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    private val pages = ArrayList<View>()
    private var current = 0

    var onPageChanged: ((Int) -> Unit)? = null

    fun setPages(views: List<View>) {
        pages.clear()
        removeAllViews()
        pages.addAll(views)
        views.forEach { addView(it) }
        show(0)
    }

    fun getCurrentItem(): Int = current

    fun setCurrentItem(index: Int, smooth: Boolean) = show(index)

    fun a(index: Int, smooth: Boolean) = show(index)

    private fun show(index: Int) {
        if (pages.isEmpty()) return
        current = index.coerceIn(0, pages.size - 1)
        pages.forEachIndexed { i, view -> view.visibility = if (i == current) View.VISIBLE else View.GONE }
        onPageChanged?.invoke(current)
    }
}
