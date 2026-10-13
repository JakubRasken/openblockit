package it.mirko.widget.navigation

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup.LayoutParams
import android.view.animation.PathInterpolator
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatImageView
import com.google.android.material.card.MaterialCardView
import com.openblockit.R

class AltNavigationItem @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    var menuId: Int = 0
    private val container: MaterialCardView
    private val icon: AppCompatImageView
    private val mask: MaterialCardView

    private val standard = PathInterpolator(0.2f, 0f, 0f, 1f)

    init {
        LayoutInflater.from(context).inflate(R.layout._alt_navigation_item, this, true)
        container = findViewById(R.id.itemSelectedContainer)
        icon = findViewById(R.id.navIcon)
        mask = findViewById(R.id.maskStats)
        container.visibility = View.INVISIBLE
        container.layoutParams = LayoutParams(dp(55), dp(31), Gravity.CENTER)
        container.radius = dp(16).toFloat()
        mask.translationX = -dp(24).toFloat()
        findViewById<View?>(R.id.badge)?.visibility = View.GONE
    }

    fun setIcon(index: Int) {
        icon.setImageResource(
            when (index) {
                0 -> R.drawable.ic_navigation_session
                1 -> R.drawable.ic_navigation_bag
                2 -> R.drawable.ic_navigation_control
                else -> R.drawable.ic_navigation_stats
            }
        )
    }

    fun setMenuItemSelected(selected: Boolean) {
        container.animate().cancel()
        mask.animate().cancel()
        if (selected) {
            container.visibility = View.VISIBLE
            container.alpha = 0f
            container.animate().alpha(1f).setDuration(160L).setInterpolator(standard).start()
            icon.imageTintList = android.content.res.ColorStateList.valueOf(
                context.getColor(R.color.colorOnSecondaryContainer)
            )
            mask.translationX = -dp(24).toFloat()
            mask.animate().translationX(0f).setDuration(180L).setInterpolator(standard)
                .withEndAction {
                    mask.translationX = dp(24).toFloat()
                }.start()
        } else {
            container.visibility = View.INVISIBLE
            icon.imageTintList = android.content.res.ColorStateList.valueOf(
                context.getColor(R.color.colorSecondaryContainer)
            )
            mask.translationX = -dp(24).toFloat()
        }
    }

    fun setBadge(visible: Boolean) {
        findViewById<View?>(R.id.badge)?.visibility = if (visible) View.VISIBLE else View.GONE
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
