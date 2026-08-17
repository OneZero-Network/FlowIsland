package com.flowisland.android.core.activity

import com.flowisland.android.core.activity.model.ActivityId
import com.flowisland.android.core.activity.model.ActivityState
import com.flowisland.android.core.activity.model.ActivityType
import com.flowisland.android.core.activity.model.ActivityUiState
import com.flowisland.android.core.database.ActiveActivityDao
import com.flowisland.android.core.di.ApplicationScope
import com.flowisland.android.core.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The single source of truth for every ongoing FlowIsland activity.
 *
 * The StateFlow is the hot UI/runtime representation; Room is the durable
 * recovery layer. This distinction matters: a process kill must not erase a
 * running timer, study session, trip, etc. Media is intentionally excluded from
 * durable storage because its source of truth is the current system media session.
 */
@Singleton
class ActivityEngine @Inject constructor(
    @ApplicationScope private val applicationScope: CoroutineScope,
    private val activeActivityDao: ActiveActivityDao,
    private val expiryScheduler: ActivityExpiryScheduler,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {

    private val _activities = MutableStateFlow<Map<ActivityId, ActivityUiState>>(emptyMap())
    private val restoreComplete = CompletableDeferred<Unit>()
    private val persistenceDispatcher = ioDispatcher.limitedParallelism(1)

    val activities: StateFlow<List<ActivityUiState>> = _activities
        .mapLatest { map -> PriorityEngine.sort(map.values.filter { !it.hidden }) }
        .stateIn(applicationScope, SharingStarted.Eagerly, emptyList())

    init {
        applicationScope.launch(ioDispatcher) {
            try {
                val restored = activeActivityDao.getAll()
                    .mapNotNull { ActivityStateCodec.fromEntity(it) }
                    .filter { it.type != ActivityType.MEDIA }
                    .map { state ->
                        if (state.state == ActivityState.ACTIVE &&
                            state.type != ActivityType.POMODORO &&
                            state.timer?.isExpired() == true
                        ) {
                            state.copy(state = ActivityState.EXPIRED)
                        } else {
                            state
                        }
                    }
                    .associateBy { it.id }

                if (restored.isNotEmpty()) {
                    restored.values.forEach(expiryScheduler::schedule)
                    _activities.update { current ->
                        // A user can create an activity immediately while restore is
                        // still running. Never overwrite that newer in-memory state.
                        restored + current
                    }
                }
            } finally {
                restoreComplete.complete(Unit)
            }
        }
    }

    suspend fun awaitRestored() = restoreComplete.await()

    fun snapshot(): List<ActivityUiState> = _activities.value.values.toList()

    fun get(id: ActivityId): ActivityUiState? = _activities.value[id]

    fun observe(id: ActivityId): StateFlow<ActivityUiState?> =
        _activities.map { it[id] }.stateIn(applicationScope, SharingStarted.Eagerly, _activities.value[id])

    fun upsert(state: ActivityUiState) {
        val next = state.copy(updatedAt = System.currentTimeMillis())
        _activities.update { it + (next.id to next) }
        persist(next)
        if (next.state.isTerminal) expiryScheduler.cancel(next.id.value) else expiryScheduler.schedule(next)
    }

    fun update(id: ActivityId, transform: (ActivityUiState) -> ActivityUiState) {
        var updated: ActivityUiState? = null
        _activities.update { map ->
            val current = map[id] ?: return@update map
            val next = transform(current).copy(updatedAt = System.currentTimeMillis())
            updated = next
            map + (id to next)
        }
        updated?.let { next ->
            persist(next)
            if (next.state.isTerminal) expiryScheduler.cancel(next.id.value) else expiryScheduler.schedule(next)
        }
    }

    fun markInteracted(id: ActivityId) = update(id) { it.copy(lastInteractedAt = System.currentTimeMillis()) }

    fun pause(id: ActivityId) = update(id) {
        it.copy(state = ActivityState.PAUSED, timer = it.timer?.pause())
    }

    fun resume(id: ActivityId) = update(id) {
        it.copy(state = ActivityState.ACTIVE, timer = it.timer?.resume())
    }

    fun complete(id: ActivityId) = update(id) { it.copy(state = ActivityState.COMPLETED) }
    fun fail(id: ActivityId) = update(id) { it.copy(state = ActivityState.FAILED) }
    fun cancel(id: ActivityId) = update(id) { it.copy(state = ActivityState.CANCELLED) }
    fun expire(id: ActivityId) = update(id) { it.copy(state = ActivityState.EXPIRED) }

    fun pin(id: ActivityId) = update(id) { it.copy(pinned = true) }
    fun unpin(id: ActivityId) = update(id) { it.copy(pinned = false) }
    fun hide(id: ActivityId) = update(id) { it.copy(hidden = true) }
    fun unhide(id: ActivityId) = update(id) { it.copy(hidden = false) }

    fun dismiss(id: ActivityId) {
        _activities.update { it - id }
        expiryScheduler.cancel(id.value)
        applicationScope.launch(persistenceDispatcher) { activeActivityDao.delete(id.value) }
    }

    fun setProgress(id: ActivityId, progress: Float) =
        update(id) { it.copy(explicitProgress = progress.coerceIn(0f, 1f)) }

    private fun persist(state: ActivityUiState) {
        if (state.type == ActivityType.MEDIA) return
        applicationScope.launch(persistenceDispatcher) {
            activeActivityDao.upsert(ActivityStateCodec.toEntity(state))
        }
    }
}
