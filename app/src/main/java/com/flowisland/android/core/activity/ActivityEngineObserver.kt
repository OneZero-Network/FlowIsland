package com.flowisland.android.core.activity

import com.flowisland.android.core.activity.model.ActivityState
import com.flowisland.android.core.activity.model.ActivityUiState
import com.flowisland.android.core.database.ActivityHistoryDao
import com.flowisland.android.core.database.ActivityHistoryEntity
import com.flowisland.android.core.di.ApplicationScope
import com.flowisland.android.core.di.IoDispatcher
import com.flowisland.android.core.notification.NotificationBridge
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The one place that reacts to every ActivityEngine change and fans it out to
 * the durable systems: the notification (via [NotificationBridge]) and the
 * append-only history ledger. The overlay service listens to the engine
 * independently while it's running, so this class stays notification/history-only.
 */
@Singleton
class ActivityEngineObserver @Inject constructor(
    private val activityEngine: ActivityEngine,
    private val notificationBridge: NotificationBridge,
    private val activityHistoryDao: ActivityHistoryDao,
    @ApplicationScope private val scope: CoroutineScope,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    private val recordedTerminalIds = mutableSetOf<String>()
    private val startedAtById = mutableMapOf<String, Long>()

    fun start() {
        scope.launch {
            activityEngine.activities.collect { list ->
                list.forEach { state -> handle(state) }
            }
        }
    }

    private suspend fun handle(state: ActivityUiState) {
        startedAtById.putIfAbsent(state.id.value, state.createdAt)
        notificationBridge.sync(state)

        if (state.state.isTerminal && state.id.value !in recordedTerminalIds) {
            recordedTerminalIds += state.id.value
            withContext(ioDispatcher) {
                activityHistoryDao.insert(
                    ActivityHistoryEntity(
                        id = state.id.value,
                        type = state.type.name,
                        title = state.title,
                        startedAt = startedAtById[state.id.value] ?: state.createdAt,
                        endedAt = System.currentTimeMillis(),
                        durationMillis = System.currentTimeMillis() - (startedAtById[state.id.value] ?: state.createdAt),
                        completed = state.state == ActivityState.COMPLETED,
                    )
                )
            }
            // Brief grace period so the UI can show the subtle "Completed" state
            // before the activity disappears from the engine entirely.
            scope.launch {
                delay(4_000)
                notificationBridge.cancel(state.id.value)
                activityEngine.dismiss(state.id)
                recordedTerminalIds -= state.id.value
                startedAtById -= state.id.value
            }
        }
    }
}
