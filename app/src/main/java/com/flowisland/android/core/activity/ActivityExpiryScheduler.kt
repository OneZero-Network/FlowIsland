package com.flowisland.android.core.activity

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.flowisland.android.MainActivity
import com.flowisland.android.core.activity.model.ActivityState
import com.flowisland.android.core.activity.model.ActivityUiState
import com.flowisland.android.core.database.ActiveActivityDao
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Uses AlarmManager.setAlarmClock for user-visible countdown deadlines. This is
 * more reliable than a coroutine delay during Doze and survives process death.
 */
@Singleton
class ActivityExpiryScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val activeActivityDao: ActiveActivityDao,
) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun schedule(state: ActivityUiState) {
        val timer = state.timer ?: return
        if (timer.countUp || state.state != ActivityState.ACTIVE) {
            cancel(state.id.value)
            return
        }
        val triggerAt = timer.wallClockTargetOrStartMillis()
        if (triggerAt <= System.currentTimeMillis()) {
            cancel(state.id.value)
            return
        }

        val showIntent = PendingIntent.getActivity(
            context,
            requestCode(state.id.value, 0),
            Intent(context, MainActivity::class.java).putExtra(MainActivity.EXTRA_OPEN_ACTIVITY_ID, state.id.value),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val fireIntent = PendingIntent.getBroadcast(
            context,
            requestCode(state.id.value, 1),
            Intent(context, ActivityExpiryReceiver::class.java).putExtra(ActivityExpiryReceiver.EXTRA_ACTIVITY_ID, state.id.value),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(triggerAt, showIntent), fireIntent)
    }

    fun cancel(activityId: String) {
        val fireIntent = PendingIntent.getBroadcast(
            context,
            requestCode(activityId, 1),
            Intent(context, ActivityExpiryReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        alarmManager.cancel(fireIntent)
    }

    /** Rebuild alarms after reboot because AlarmManager entries do not persist across reboot. */
    suspend fun rescheduleAllPersisted() = withContext(Dispatchers.IO) {
        activeActivityDao.getAll()
            .mapNotNull { ActivityStateCodec.fromEntity(it) }
            .forEach(::schedule)
    }

    private fun requestCode(id: String, salt: Int): Int = (id.hashCode() * 31) + salt
}
