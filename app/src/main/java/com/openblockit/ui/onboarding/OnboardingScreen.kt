package com.openblockit.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.openblockit.core.AccessibilityUtils
import com.openblockit.core.BatteryUtils
import com.openblockit.core.OverlayPermission
import com.openblockit.core.Permissions
import com.openblockit.core.Prefs
import com.openblockit.core.supportsExactAlarms
import com.openblockit.ui.components.DotText
import com.openblockit.ui.components.MutedLabel
import com.openblockit.ui.components.SlideToStart
import com.openblockit.ui.theme.LocalPal

@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    val pal = LocalPal.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var tick by remember { mutableIntStateOf(0) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) tick++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val a11y = AccessibilityUtils.enabled(context)
    val overlay = OverlayPermission.granted(context)
    val notifications = Permissions.notificationsGranted(context)
    val alarms = supportsExactAlarms(context)
    val usage = Permissions.usageGranted(context)
    val battery = BatteryUtils.ignoreRequested(context)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(24.dp))
        DotText(
            text = "openblockit",
            tileSize = 4.dp,
            gap = 2.dp,
            color = pal.onSurface,
            letterGap = 4.dp,
        )
        Spacer(Modifier.height(24.dp))
        Text(
            "Welcome aboard",
            color = pal.onSurface,
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "This is your ticket to break free from smartphone addiction. openblockit locks your phone " +
                "for the time you choose: no shortcuts, no \"just five minutes\".",
            color = pal.onSurfaceVariant,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(24.dp))
        MutedLabel("BEFORE BOARDING")
        Spacer(Modifier.height(8.dp))

        Surface(
            color = pal.surfaceBright,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(16.dp)) {
                OnboardingRow("Session block", "Keeps you on the block screen during a session", a11y) { Permissions.openAccessibility(context) }
                OnboardingRow("Show over apps", "Brings the session back and shows the timer", overlay) { Permissions.openOverlay(context) }
                OnboardingRow("Notifications", "Countdown and reminders", notifications) { Permissions.openNotifications(context) }
                OnboardingRow("Alarms & reminders", "Routines start on time", alarms) { Permissions.openAlarms(context) }
                OnboardingRow("Usage access", "Backup lock if the block service stops", usage) { Permissions.openUsageAccess(context) }
                OnboardingRow("Battery unrestricted", "The system may otherwise stop a running block", battery) { Permissions.openBattery(context) }
            }
        }

        Spacer(Modifier.height(20.dp))
        MutedLabel("THE PARACHUTE")
        Spacer(Modifier.height(8.dp))
        Text(
            "The parachute is the only way to interrupt a session ahead of schedule. Everything in openblockit is free: " +
                "you start with three, and earn more as your tower grows.",
            color = pal.onSurfaceVariant,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(28.dp))
        SlideToStart(
            text = "Slide to start",
            onComplete = {
                Prefs.onboardingDone = true
                onDone()
            },
        )
        Spacer(Modifier.height(10.dp))
        TextButton(onClick = {
            Prefs.onboardingDone = true
            onDone()
        }) {
            Text("Continue as guest", color = pal.onSurfaceVariant, fontSize = 13.sp)
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun OnboardingRow(label: String, description: String, granted: Boolean, onGrant: () -> Unit) {
    val pal = LocalPal.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = 8.dp)) {
            Text(label, color = pal.onSurface, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(description, color = pal.tileMuted, fontSize = 11.sp)
        }
        if (granted) {
            Text("Granted", fontSize = 12.sp, color = pal.primaryContainer)
        } else {
            TextButton(onClick = onGrant) { Text("Grant", color = pal.primaryContainer) }
        }
    }
}
