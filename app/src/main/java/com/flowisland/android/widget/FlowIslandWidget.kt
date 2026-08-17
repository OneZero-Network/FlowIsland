package com.flowisland.android.widget

import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.flowisland.android.MainActivity
import com.flowisland.android.R
import com.flowisland.android.core.activity.ActivityEngine
import com.flowisland.android.core.time.TimeFormat
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun activityEngine(): ActivityEngine
}

/**
 * Kept intentionally simple (no custom colors/backgrounds) since RemoteViews-
 * backed Glance surfaces have the narrowest, most version-sensitive API surface
 * in the whole app; correctness here matters more than visual polish, and the
 * home-screen launcher already tints/frames widgets consistently per OEM.
 */
class FlowIslandWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val activityEngine = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java).activityEngine()
        val topActivity = activityEngine.activities.value.firstOrNull()

        val subtitle = topActivity?.timer?.let {
            if (it.countUp) TimeFormat.stopwatch(it.elapsedMillis()) else TimeFormat.countdown(it.remainingMillis())
        } ?: topActivity?.subtitle

        provideContent {
            Column(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    .clickable(actionStartActivity<MainActivity>()),
            ) {
                Text(
                    text = topActivity?.title ?: context.getString(R.string.widget_nothing_active),
                    style = TextStyle(fontSize = 14.sp),
                )
                if (subtitle != null) {
                    Text(text = subtitle, style = TextStyle(fontSize = 12.sp))
                }
            }
        }
    }
}

class FlowIslandWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = FlowIslandWidget()
}
