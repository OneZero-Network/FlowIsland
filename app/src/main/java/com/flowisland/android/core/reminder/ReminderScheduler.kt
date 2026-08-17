package com.flowisland.android.core.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.flowisland.android.MainActivity
import com.flowisland.android.core.database.ReminderDao
import com.flowisland.android.core.database.ReminderEntity
import com.flowisland.android.core.di.ApplicationScope
import com.flowisland.android.core.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Uses AlarmManager#setAlarmClock(), the API Android reserves for user-visible
 * "alarm clock"-style events. Unlike setExactAndAllowWhileIdle(), it does not
 * require the SCHEDULE_EXACT_ALARM special permission and is exempt from Doze/
 * App Standby deferral -- which is exactly the reliability a "remind me at
 * 18:30" feature needs, without over-requesting permissions.
 */
@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val reminderDao: ReminderDao,
    @ApplicationScope private val scope: CoroutineScope,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun schedule(reminder: ReminderEntity) {
        val showIntent = PendingIntent.getActivity(
            context, reminder.id.hashCode(),
            Intent(context, MainActivity::class.java).apply {
                putExtra(MainActivity.EXTRA_OPEN_ACTIVITY_ID, reminder.id)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val fireIntent = PendingIntent.getBroadcast(
            context, reminder.id.hashCode() + 1,
            Intent(context, ReminderAlarmReceiver::class.java).putExtra(ReminderAlarmReceiver.EXTRA_REMINDER_ID, reminder.id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(reminder.triggerAtMillis, showIntent), fireIntent)
    }

    fun cancel(reminderId: String) {
        val fireIntent = PendingIntent.getBroadcast(
            context, reminderId.hashCode() + 1,
            Intent(context, ReminderAlarmReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        alarmManager.cancel(fireIntent)
    }

    fun markDone(reminderId: String) {
        scope.launch(ioDispatcher) {
            reminderDao.markDone(reminderId)
            cancel(reminderId)
        }
    }

    fun snooze(reminderId: String, minutes: Int) {
        scope.launch(ioDispatcher) {
            val newTrigger = System.currentTimeMillis() + minutes * 60_000L
            reminderDao.snooze(reminderId, newTrigger)
            reminderDao.getPendingOnce().find { it.id == reminderId }?.let { schedule(it) }
        }
    }

    /** Re-arms every pending reminder's AlarmManager entry -- called once after boot. */
    suspend fun rescheduleAllPending() = withContext(ioDispatcher) {
        reminderDao.getPendingOnce().forEach { schedule(it) }
    }
}
