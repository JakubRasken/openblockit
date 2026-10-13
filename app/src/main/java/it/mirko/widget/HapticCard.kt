package it.mirko.widget

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.AttributeSet
import android.view.MotionEvent
import com.google.android.material.card.MaterialCardView
import com.openblockit.core.Prefs

open class HapticCard @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : MaterialCardView(context, attrs, defStyleAttr) {

    private val vibrator: Vibrator? = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN && hasOnClickListeners() && isEnabled && Prefs.hapticsEnabled) {
            vibrator?.vibrate(VibrationEffect.createOneShot(1L, VibrationEffect.DEFAULT_AMPLITUDE))
        }
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean {
        if (Prefs.hapticsEnabled) {
            vibrator?.vibrate(VibrationEffect.createOneShot(10L, VibrationEffect.DEFAULT_AMPLITUDE))
        }
        return super.performClick()
    }
}
