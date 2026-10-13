package com.openblockit.widget

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openblockit.MainActivity
import com.openblockit.core.Prefs
import com.openblockit.core.SessionManager
import com.openblockit.core.StartResult
import com.openblockit.ui.theme.OpenBlockitTheme

class WidgetLaunchActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OpenBlockitTheme {
                WidgetStartScreen(onDone = { finish() })
            }
        }
    }
}

@Composable
private fun WidgetStartScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Start a session", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(15, 30, 60, 90).forEach { minutes ->
                AssistChip(
                    onClick = {
                        Prefs.lastMinutes = minutes
                        when (SessionManager.start(minutes)) {
                            StartResult.Started, StartResult.AlreadyRunning -> {
                                SessionManager.showBlockScreen()
                                onDone()
                            }
                            StartResult.NeedsParachute ->
                                Toast.makeText(context, "Sessions longer than 3 hours require a parachute", Toast.LENGTH_LONG).show()
                        }
                    },
                    label = { Text("$minutes m") },
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        TextButton(onClick = {
            context.startActivity(Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            onDone()
        }) {
            Text("Open openblockit")
        }
        TextButton(onClick = onDone) {
            Text("Cancel")
        }
    }
}
