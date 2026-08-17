package com.flowisland.android.core.notification

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.flowisland.android.MainActivity
import com.flowisland.android.R
import com.flowisland.android.core.activity.model.ActivityAction
import com.flowisland.android.core.activity.model.ActivityState
import com.flowisland.android.core.activity.model.ActivityType
import com.flowisland.android.core.activity.model.ActivityUiState
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Converts the ActivityEngine's single source-of-truth snapshot into a real
 * platform Notification. This is the ONLY place in the app that constructs a
 * Notification -- feature code never builds its own.
 *
 * Two code paths, chosen per-device, per-notification:
 *  - Android 16+ with promoted-notification access granted: Notification.ProgressStyle
 *    ("Live Update"), a real system-native progress-centric notification.
 *  - Everything else (older Android, or the user denied promoted-notification
 *    access): a standard ongoing NotificationCompat notification using
 *    setUsesChronometer, which the system auto-updates from the timestamp with
 *    zero app-side ticking -- battery-neutral and correct after screen off/on.
 */
@Singleton
class NotificationBridge @Inject constructor(@ApplicationContext private val context: Context) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun notificationIdFor(activityId: String): Int = activityId.hashCode()

    fun channelFor(type: ActivityType): String = when (type) {
        ActivityType.TIMER, ActivityType.POMODORO, ActivityType.STOPWATCH -> NotificationChannels.TIMERS
        ActivityType.REMINDER -> NotificationChannels.REMINDERS
        ActivityType.FITNESS -> NotificationChannels.FITNESS
        ActivityType.TRIP -> NotificationChannels.TRIPS
        else -> NotificationChannels.ACTIVE_ACTIVITIES
    }

    fun canUseLiveUpdate(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA &&
            runCatching { notificationManager.canPostPromotedNotifications() }.getOrDefault(false)

    fun sync(state: ActivityUiState) {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return
        if (state.state.isTerminal) {
            postTerminalNotification(state)
            return
        }
        val notification = if (canUseLiveUpdate()) buildLiveUpdateNotification(state) else buildStandardNotification(state)
        NotificationManagerCompat.from(context).notify(notificationIdFor(state.id.value), notification)
    }

    fun cancel(activityId: String) {
        NotificationManagerCompat.from(context).cancel(notificationIdFor(activityId))
    }

    private fun contentIntent(state: ActivityUiState): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(MainActivity.EXTRA_OPEN_ACTIVITY_ID, state.id.value)
        }
        return PendingIntent.getActivity(context, state.id.value.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun actionPendingIntent(state: ActivityUiState, action: ActivityAction): PendingIntent {
        val intent = Intent(context, ActivityActionReceiver::class.java).apply {
            setAction("com.flowisland.android.ACTION_${action.kind.name}")
            putExtra(ActivityActionReceiver.EXTRA_ACTIVITY_ID, state.id.value)
            putExtra(ActivityActionReceiver.EXTRA_ACTION_KIND, action.kind.name)
        }
        val requestCode = (state.id.value + action.kind.name).hashCode()
        return PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun compatActionsFor(state: ActivityUiState): List<NotificationCompat.Action> =
        state.actions.take(3).map { action ->
            NotificationCompat.Action.Builder(
                R.drawable.ic_notification_flowisland,
                context.getString(action.labelResId),
                actionPendingIntent(state, action),
            ).build()
        }

    private fun buildStandardNotification(state: ActivityUiState): Notification {
        val builder = NotificationCompat.Builder(context, channelFor(state.type))
            .setSmallIcon(R.drawable.ic_notification_flowisland)
            .setContentTitle(state.title)
            .setContentText(state.subtitle)
            .setOngoing(state.state == ActivityState.ACTIVE || state.state == ActivityState.PAUSED)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentIntent(state))
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)

        state.timer?.let { timer ->
            if (!timer.isPaused) {
                builder.setUsesChronometer(true)
                builder.setChronometerCountDown(!timer.countUp)
                builder.setWhen(timer.wallClockTargetOrStartMillis())
            } else {
                builder.setUsesChronometer(false)
            }
        }

        state.explicitProgress?.let { progress ->
            builder.setProgress(100, (progress * 100).toInt(), false)
        }

        compatActionsFor(state).forEach { builder.addAction(it) }
        return builder.build()
    }

    private fun buildLiveUpdateNotification(state: ActivityUiState): Notification {
        // Platform Notification.Builder is required directly for ProgressStyle
        // (AndroidX NotificationCompat's promoted-notification wrapper is still
        // rolling out); this branch only ever runs on API 36+ (checked above).
        val progressPercent = ((state.explicitProgress ?: state.timer?.progress() ?: 0f) * 100).toInt().coerceIn(0, 100)

        val progressStyle = Notification.ProgressStyle()
            .setStyledByProgress(false)
            .setProgress(progressPercent)
            .setProgressSegments(
                listOf(Notification.ProgressStyle.Segment(100).setColor(Color.parseColor("#5B5BD6")))
            )

        val builder = Notification.Builder(context, channelFor(state.type))
            .setSmallIcon(R.drawable.ic_notification_flowisland)
            .setContentTitle(state.title)
            .setContentText(state.subtitle)
            .setOngoing(state.state == ActivityState.ACTIVE || state.state == ActivityState.PAUSED)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentIntent(state))
            .setStyle(progressStyle)
            // Android 16.1 exposes this through Notification.Builder. Writing the
            // documented extra keeps the source compatible with API 36 SDKs while
            // still requesting promoted ongoing treatment on devices that support it.
            .setExtras(android.os.Bundle().apply {
                putBoolean("android.requestPromotedOngoing", true)
            })

        state.timer?.takeIf { !it.isPaused }?.let { timer ->
            builder.setUsesChronometer(true)
            builder.setChronometerCountDown(!timer.countUp)
            builder.setWhen(timer.wallClockTargetOrStartMillis())
        }

        state.actions.take(3).forEach { action ->
            builder.addAction(
                Notification.Action.Builder(
                    androidx.core.graphics.drawable.IconCompat.createWithResource(context, R.drawable.ic_notification_flowisland).toIcon(context),
                    context.getString(action.labelResId),
                    actionPendingIntent(state, action),
                ).build()
            )
        }

        return builder.build()
    }

    private fun postTerminalNotification(state: ActivityUiState) {
        val builder = NotificationCompat.Builder(context, channelFor(state.type))
            .setSmallIcon(R.drawable.ic_notification_flowisland)
            .setContentTitle(state.title)
            .setContentText(when (state.state) {
                ActivityState.COMPLETED -> context.getString(R.string.completion_label)
                ActivityState.CANCELLED -> context.getString(R.string.cancelled_label)
                ActivityState.FAILED -> context.getString(R.string.failed_label)
                ActivityState.EXPIRED -> context.getString(R.string.expired_label)
                else -> context.getString(R.string.completion_label)
            })
            .setOngoing(false)
            .setAutoCancel(true)
            .setContentIntent(contentIntent(state))
        NotificationManagerCompat.from(context).notify(notificationIdFor(state.id.value), builder.build())
    }
}
