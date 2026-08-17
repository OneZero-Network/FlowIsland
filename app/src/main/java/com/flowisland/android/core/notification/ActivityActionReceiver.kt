package com.flowisland.android.core.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.flowisland.android.core.activity.ActivityEngine
import com.flowisland.android.core.activity.model.ActivityAction
import com.flowisland.android.core.activity.model.ActivityId
import com.flowisland.android.core.reminder.ReminderScheduler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ActivityActionReceiver : BroadcastReceiver() {

    @Inject lateinit var activityEngine: ActivityEngine
    @Inject lateinit var notificationBridge: NotificationBridge
    @Inject lateinit var reminderScheduler: ReminderScheduler

    companion object {
        const val EXTRA_ACTIVITY_ID = "extra_activity_id"
        const val EXTRA_ACTION_KIND = "extra_action_kind"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val activityIdValue = intent.getStringExtra(EXTRA_ACTIVITY_ID) ?: return
        val kindName = intent.getStringExtra(EXTRA_ACTION_KIND) ?: return
        val kind = runCatching { ActivityAction.Kind.valueOf(kindName) }.getOrNull() ?: return
        val id = ActivityId(activityIdValue)

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                when (kind) {
                    ActivityAction.Kind.PAUSE -> {
                        val current = activityEngine.get(id)
                        if (current?.type == com.flowisland.android.core.activity.model.ActivityType.MEDIA) {
                            com.flowisland.android.feature.media.MediaSessionListenerService.activeInstance?.handlePlayPause(resume = false)
                        } else {
                            activityEngine.pause(id)
                        }
                    }
                    ActivityAction.Kind.RESUME -> {
                        val current = activityEngine.get(id)
                        if (current?.type == com.flowisland.android.core.activity.model.ActivityType.MEDIA) {
                            com.flowisland.android.feature.media.MediaSessionListenerService.activeInstance?.handlePlayPause(resume = true)
                        } else {
                            activityEngine.resume(id)
                        }
                    }
                    ActivityAction.Kind.CANCEL -> activityEngine.cancel(id)
                    ActivityAction.Kind.FINISH -> activityEngine.complete(id)
                    ActivityAction.Kind.ADD_1_MIN -> activityEngine.update(id) { it.copy(timer = it.timer?.addDuration(60_000)) }
                    ActivityAction.Kind.ADD_5_MIN -> activityEngine.update(id) { it.copy(timer = it.timer?.addDuration(300_000)) }
                    ActivityAction.Kind.DONE -> {
                        activityEngine.complete(id)
                        reminderScheduler.markDone(activityIdValue)
                    }
                    ActivityAction.Kind.SNOOZE -> reminderScheduler.snooze(activityIdValue, minutes = 10)
                    ActivityAction.Kind.LAP, ActivityAction.Kind.CUSTOM,
                    ActivityAction.Kind.VIEW_RESULT, ActivityAction.Kind.OPEN_NAVIGATION -> {
                        // These require app UI context (lap list, result screen, navigation
                        // app chooser) and are handled from the expanded island / notification
                        // tap into MainActivity rather than as a headless broadcast action.
                    }
                }
                activityEngine.get(id)?.let { notificationBridge.sync(it) }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
