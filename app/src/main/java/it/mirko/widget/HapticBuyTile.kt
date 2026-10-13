package it.mirko.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import com.openblockit.R

class HapticBuyTile @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : HapticCard(context, attrs, defStyleAttr) {

    private val number: TextView
    private val label: TextView

    init {
        val view = LayoutInflater.from(context).inflate(R.layout._view_preset_layout, this, false)
        number = view.findViewById(R.id.presetTextNumber)
        label = view.findViewById(R.id.presetTextLabel)
        addView(view)
        setCardBackgroundColor(context.getColor(R.color.colorSurfaceVariant))
        val color = context.getColor(R.color.colorOnSurfaceVariant)
        number.setTextColor(color)
        label.setTextColor(color)
    }

    fun setActive(active: Boolean) {
        setCardBackgroundColor(context.getColor(if (active) R.color.colorOnSurface else R.color.colorSurfaceVariant))
        val color = context.getColor(if (active) R.color.colorOnSecondaryContainer else R.color.colorOnSurfaceVariant)
        number.setTextColor(color)
        label.setTextColor(color)
    }

    fun setLabel(text: String) {
        label.text = text
    }

    fun setNumber(text: String) {
        number.text = text
    }

    fun setSingleText(text: String) {
        label.visibility = ViewGroup.GONE
        number.isAllCaps = false
        number.setAutoSizeTextTypeWithDefaults(android.widget.TextView.AUTO_SIZE_TEXT_TYPE_NONE)
        number.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 16f)
        number.text = text
    }
}
