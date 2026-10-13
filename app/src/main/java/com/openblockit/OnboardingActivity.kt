package com.openblockit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.openblockit.core.Prefs
import com.openblockit.ui.onboarding.OnboardingScreen
import com.openblockit.ui.theme.OpenBlockitTheme

class OnboardingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OpenBlockitTheme {
                var done by remember { mutableStateOf(Prefs.onboardingDone) }
                if (!done) {
                    OnboardingScreen {
                        done = true
                        finish()
                    }
                } else {
                    finish()
                }
            }
        }
    }
}
