package it.mirko.widget

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.AttributeSet
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.openblockit.core.Prefs

class HapticFAB @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FloatingActionButton(context, attrs, defStyleAttr) {

    private val vibrator: Vibrator? = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator

    override fun performClick(): Boolean {
        if (Prefs.hapticsEnabled) {
            vibrator?.vibrate(VibrationEffect.createOneShot(12L, VibrationEffect.DEFAULT_AMPLITUDE))
        }
        return super.performClick()
    }
}
