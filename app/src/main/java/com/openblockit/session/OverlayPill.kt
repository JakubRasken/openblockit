package com.openblockit.session

import android.content.Context
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import com.openblockit.core.SessionManager

object OverlayPill {
    private var appContext: Context? = null
    private var view: View? = null
    private var textView: TextView? = null
    private var windowManager: WindowManager? = null
    private var params: WindowManager.LayoutParams? = null
    private var currentLabel: String = ""

    var isVisible: Boolean = false
        private set

    private val mainHandler = Handler(Looper.getMainLooper())

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun show(label: String, secondsLeft: Int) {
        currentLabel = label
        mainHandler.post {
            try {
                val context = appContext ?: return@post
                if (!Settings.canDrawOverlays(context)) return@post
                if (view == null) createView(context)
                textView?.text = "$label · ${com.openblockit.core.formatClock(secondsLeft)}"
                if (!isVisible) {
                    view?.visibility = View.VISIBLE
                    isVisible = true
                }
            } catch (_: Exception) {
            }
        }
    }

    fun refresh(secondsLeft: Int) {
        if (!isVisible || currentLabel.isEmpty()) return
        mainHandler.post {
            try {
                textView?.text = "$currentLabel · ${com.openblockit.core.formatClock(secondsLeft)}"
            } catch (_: Exception) {
            }
        }
    }

    fun hide() {
        mainHandler.post {
            try {
                val v = view
                val wm = windowManager
                if (v != null && wm != null) wm.removeViewImmediate(v)
            } catch (_: Exception) {
            }
            view = null
            textView = null
            windowManager = null
            params = null
            currentLabel = ""
            isVisible = false
        }
    }

    private fun createView(context: Context) {
        val tv = TextView(context).apply {
            setPadding(dp(context, 18), dp(context, 8), dp(context, 18), dp(context, 8))
            setBackgroundColor(0xE61C1A17.toInt())
            setTextColor(0xFFF2EDE6.toInt())
            textSize = 13f
        }
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = dp(context, 56)
        }
        wm.addView(tv, layoutParams)
        view = tv
        textView = tv
        windowManager = wm
        params = layoutParams
    }

    private fun dp(context: Context, value: Int): Int =
        (value * context.resources.displayMetrics.density).toInt()
}
