package com.flowisland.android.core.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.flowisland.android.MainActivity
import com.flowisland.android.R
import com.flowisland.android.core.activity.ActivityEngine
import com.flowisland.android.core.activity.ActivityExpiryScheduler
import com.flowisland.android.core.activity.model.ActivityId
import com.flowisland.android.core.activity.model.ActivityState
import com.flowisland.android.core.database.ReminderDao
import com.flowisland.android.core.notification.ActivityActionReceiver
import com.flowisland.android.core.notification.NotificationChannels
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ReminderAlarmReceiver : BroadcastReceiver() {

    @Inject lateinit var reminderDao: ReminderDao
    @Inject lateinit var activityEngine: ActivityEngine

    companion object {
        const val EXTRA_REMINDER_ID = "extra_reminder_id"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getStringExtra(EXTRA_REMINDER_ID) ?: return
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.Default).launch {
            try {
                val reminder = reminderDao.getPendingOnce().find { it.id == reminderId } ?: return@launch

                // Reflect the firing reminder in the Activity Engine too, so it shows
                // up in the island/home screen at URGENT priority, not just as a
                // notification the user might not see immediately.
                activityEngine.update(ActivityId(reminderId)) { it.copy(state = ActivityState.ACTIVE) }

                val openIntent = android.app.PendingIntent.getActivity(
                    context, reminderId.hashCode(),
                    Intent(context, MainActivity::class.java).putExtra(MainActivity.EXTRA_OPEN_ACTIVITY_ID, reminderId),
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE,
                )
                val doneIntent = android.app.PendingIntent.getBroadcast(
                    context, reminderId.hashCode() + 2,
                    Intent(context, ActivityActionReceiver::class.java).apply {
                        setAction("com.flowisland.android.ACTION_DONE")
                        putExtra(ActivityActionReceiver.EXTRA_ACTIVITY_ID, reminderId)
                        putExtra(ActivityActionReceiver.EXTRA_ACTION_KIND, "DONE")
                    },
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE,
                )
                val snoozeIntent = android.app.PendingIntent.getBroadcast(
                    context, reminderId.hashCode() + 3,
                    Intent(context, ActivityActionReceiver::class.java).apply {
                        setAction("com.flowisland.android.ACTION_SNOOZE")
                        putExtra(ActivityActionReceiver.EXTRA_ACTIVITY_ID, reminderId)
                        putExtra(ActivityActionReceiver.EXTRA_ACTION_KIND, "SNOOZE")
                    },
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE,
                )

                val notification = NotificationCompat.Builder(context, NotificationChannels.REMINDERS)
                    .setSmallIcon(R.drawable.ic_notification_flowisland)
                    .setContentTitle(reminder.title)
                    .setContentText(context.getString(R.string.type_reminder))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setCategory(NotificationCompat.CATEGORY_REMINDER)
                    .setAutoCancel(true)
                    .setContentIntent(openIntent)
                    .addAction(R.drawable.ic_notification_flowisland, context.getString(R.string.action_done), doneIntent)
                    .addAction(R.drawable.ic_notification_flowisland, context.getString(R.string.action_snooze), snoozeIntent)
                    .build()

                NotificationManagerCompat.from(context).notify(reminderId.hashCode(), notification)
            } finally {
                pendingResult.finish()
            }
        }
    }
}

@AndroidEntryPoint
class BootRescheduleReceiver : BroadcastReceiver() {

    @Inject lateinit var reminderScheduler: ReminderScheduler
    @Inject lateinit var activityExpiryScheduler: ActivityExpiryScheduler

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                reminderScheduler.rescheduleAllPending()
                activityExpiryScheduler.rescheduleAllPersisted()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
