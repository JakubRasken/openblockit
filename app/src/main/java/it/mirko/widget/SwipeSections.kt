package it.mirko.widget

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import kotlin.math.abs

class SwipeSections @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    private val sections = ArrayList<View>()
    private var currentIndex = 0
    private var downX = 0f
    private var downY = 0f
    private var suppress = false

    var onSectionChanged: ((Int) -> Unit)? = null

    fun attachSections(views: List<View>) {
        sections.clear()
        sections.addAll(views)
        currentIndex = 0
        applyVisibility()
    }

    fun getCurrentSection(): Int = currentIndex

    fun setCurrentSection(index: Int, animate: Boolean) {
        if (sections.isEmpty()) return
        val clamped = index.coerceIn(0, sections.size - 1)
        val changed = clamped != currentIndex
        currentIndex = clamped
        applyVisibility()
        if (changed && !suppress) {
            suppress = true
            try {
                onSectionChanged?.invoke(currentIndex)
            } finally {
                suppress = false
            }
        }
    }

    fun setCurrentSectionQuiet(index: Int) {
        if (sections.isEmpty()) return
        currentIndex = index.coerceIn(0, sections.size - 1)
        applyVisibility()
    }

    private fun applyVisibility() {
        sections.forEachIndexed { i, view ->
            view.visibility = if (i == currentIndex) View.VISIBLE else View.INVISIBLE
        }
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = ev.x
                downY = ev.y
                return false
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = ev.x - downX
                val dy = ev.y - downY
                if (abs(dx) > abs(dy) * 1.8f && abs(dx) > 140f) {
                    return true
                }
            }
        }
        return false
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
            }
            MotionEvent.ACTION_UP -> {
                val dx = event.x - downX
                if (dx < -140f) setCurrentSection(currentIndex + 1, true)
                else if (dx > 140f) setCurrentSection(currentIndex - 1, true)
            }
        }
        return true
    }
}
