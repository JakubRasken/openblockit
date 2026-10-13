package com.openblockit.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.Button
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.openblockit.core.Clock
import com.openblockit.core.Prefs
import com.openblockit.core.StatsRepo
import com.openblockit.core.TowerRepo
import com.openblockit.session.RoutineScheduler

class PresetWidgetContent : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val minutes = Prefs.lastMinutes
        provideContent {
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(ColorProvider(Color(0xFF1C1A17)))
                    .padding(12.dp),
                verticalAlignment = Alignment.Vertical.CenterVertically,
            ) {
                Text(
                    text = "openblockit",
                    style = TextStyle(
                        color = ColorProvider(Color(0xFFF2EDE6)),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
                Spacer(GlanceModifier.height(2.dp))
                Text(
                    text = "Preset: $minutes min",
                    style = TextStyle(color = ColorProvider(Color(0xFFC9C0B4)), fontSize = 11.sp),
                )
                Spacer(GlanceModifier.height(6.dp))
                Button(
                    text = "Start session",
                    onClick = actionStartActivity(android.content.Intent(context, WidgetLaunchActivity::class.java)),
                )
            }
        }
    }
}

class PresetWidget : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PresetWidgetContent()
}

class StreakWidgetContent : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val streak = try {
            TowerRepo.currentStreak(StatsRepo.allDays())
        } catch (_: Exception) {
            0
        }
        val todayMinutes = try {
            StatsRepo.minutesOn(Clock.todayKey())
        } catch (_: Exception) {
            0
        }
        provideContent {
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(ColorProvider(Color(0xFF1C1A17)))
                    .padding(12.dp),
                verticalAlignment = Alignment.Vertical.CenterVertically,
            ) {
                Text(
                    text = "$streak days",
                    style = TextStyle(
                        color = ColorProvider(Color(0xFFF2B33D)),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
                Text(
                    text = "Today: $todayMinutes min · tap to refresh",
                    style = TextStyle(color = ColorProvider(Color(0xFFC9C0B4)), fontSize = 11.sp),
                )
                Spacer(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .clickable(actionRunCallback<RefreshStreakAction>()),
                )
            }
        }
    }
}

class RefreshStreakAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        StreakWidgetContent().update(context, glanceId)
    }
}

class StreakWidget : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = StreakWidgetContent()
}
