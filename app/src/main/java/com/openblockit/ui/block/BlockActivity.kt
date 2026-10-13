package com.openblockit.ui.block

import android.content.Intent
import com.openblockit.MainActivity
import com.openblockit.core.SessionManager

class BlockActivity : MainActivity() {

    override fun hostsBlockScreen(): Boolean = true

    override fun onResume() {
        super.onResume()
        if (!SessionManager.started()) finish()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.getBooleanExtra("completed", false)) return
        if (!SessionManager.started()) finish()
    }
}
