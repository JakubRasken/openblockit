package it.mirko.widget.navigation

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import com.google.android.material.card.MaterialCardView

class AltNavigationBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : MaterialCardView(context, attrs, defStyleAttr) {

    private val row = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER
    }
    private val items = ArrayList<AltNavigationItem>()

    var selectedItemId = 0
        private set
    var onItemSelected: ((Int) -> Unit)? = null

    private var suppressCallback = false

    init {
        addView(row)
        for (i in 0 until 4) {
            val item = AltNavigationItem(context)
            item.menuId = i
            item.setIcon(i)
            item.setOnClickListener { select(i) }
            items.add(item)
            row.addView(item, LinearLayout.LayoutParams(dp(36), dp(48)))
        }
    }

    private fun select(index: Int) {
        val changed = index != selectedItemId
        selectedItemId = index
        items.forEachIndexed { i, item -> item.setMenuItemSelected(i == index) }
        if (changed && !suppressCallback) {
            suppressCallback = true
            try {
                onItemSelected?.invoke(index)
            } finally {
                suppressCallback = false
            }
        }
    }

    fun setSelectedItem(id: Int) = select(id)

    fun setSelectedItemAnim(id: Int) = select(id)

    fun setBadge(id: Int) = Unit

    fun setPillHeight(height: Int) = Unit

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
