package it.mirko.widget.navigation

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.widget.LinearLayout
import com.google.android.material.card.MaterialCardView
import com.openblockit.R

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

    init {
        addView(row)
        for (i in 0 until 4) {
            val item = AltNavigationItem(context)
            item.menuId = i
            item.setIconIndex(i)
            item.setOnClickListener { select(i) }
            items.add(item)
            row.addView(item, LinearLayout.LayoutParams(dp(52), dp(52)).apply { marginEnd = dp(4) })
        }
    }

    private var suppressCallback = false

    private fun select(index: Int) {
        val changed = index != selectedItemId
        selectedItemId = index
        items.forEachIndexed { i, item ->
            item.setMenuItemSelected(i == index)
            (item.pill.getChildAt(0) as? NavIconView)?.tint = item.context.getColor(
                if (i == index) R.color.colorOnPrimaryContainer else R.color.colorPrimaryContainer
            )
        }
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
