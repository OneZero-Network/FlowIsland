package com.flowisland.android.core.activity

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.flowisland.android.core.activity.model.ActivityId
import com.flowisland.android.core.activity.model.ActivityState
import com.flowisland.android.core.database.ActiveActivityDao
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ActivityExpiryReceiver : BroadcastReceiver() {
    @Inject lateinit var activityEngine: ActivityEngine
    @Inject lateinit var activeActivityDao: ActiveActivityDao

    override fun onReceive(context: Context, intent: Intent) {
        val activityId = intent.getStringExtra(EXTRA_ACTIVITY_ID) ?: return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // The engine may not have finished its asynchronous Room restore yet.
                // Read the durable row directly and re-upsert the terminal state so
                // the expiry is never lost during a cold-start race.
                val persisted = activeActivityDao.getAll().firstOrNull { it.id == activityId }
                val state = persisted?.let(ActivityStateCodec::fromEntity)
                if (state != null && state.state == ActivityState.ACTIVE) {
                    activityEngine.upsert(state.copy(state = ActivityState.EXPIRED))
                } else {
                    activityEngine.expire(ActivityId(activityId))
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val EXTRA_ACTIVITY_ID = "extra_activity_id"
    }
}
