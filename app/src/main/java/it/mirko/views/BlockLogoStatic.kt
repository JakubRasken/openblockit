package it.mirko.views

import android.content.Context
import android.util.AttributeSet

class BlockLogoStatic @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : BlockLogo(context, attrs, defStyleAttr) {

    fun setCellSizeDip(value: Float) = Unit
}
