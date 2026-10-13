package it.mirko.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.TextView
import com.openblockit.R

class HapticPreset @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : HapticCard(context, attrs, defStyleAttr) {

    private val numberView: TextView
    private val labelView: TextView

    var minutes: Int = 5
        private set

    init {
        val view = LayoutInflater.from(context).inflate(R.layout._view_preset_layout, this, false)
        numberView = view.findViewById(R.id.presetTextNumber)
        labelView = view.findViewById(R.id.presetTextLabel)
        numberView.setTextColor(context.getColor(R.color.colorSecondaryContainer))
        addView(view)
    }

    private fun labelFor(minutes: Int): String = when {
        minutes < 60 || minutes == 90 -> context.getString(R.string.time_minute_plural)
        minutes == 60 -> context.getString(R.string.time_hour_single)
        else -> context.getString(R.string.time_hour_plural)
    }

    private fun numberFor(minutes: Int): String = when {
        minutes == 15 -> context.getString(R.string.fifteen)
        minutes == 30 -> context.getString(R.string.thirty)
        minutes == 45 -> "45"
        minutes == 60 -> context.getString(R.string.one)
        minutes == 90 -> "90"
        minutes == 120 -> context.getString(R.string.two)
        minutes == 180 -> context.getString(R.string.three)
        minutes < 60 -> minutes.toString()
        minutes % 60 == 0 -> (minutes / 60).toString()
        else -> "${minutes / 60}:${String.format(java.util.Locale.US, "%02d", minutes % 60)}"
    }

    fun setMinutes(value: Int) {
        minutes = value
        numberView.text = numberFor(value)
        labelView.text = labelFor(value)
    }
}
