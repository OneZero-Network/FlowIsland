package com.flowisland.android.core.activity

import com.flowisland.android.core.activity.model.ActivityId
import com.flowisland.android.core.activity.model.ActivityState
import com.flowisland.android.core.activity.model.ActivityUiState
import com.flowisland.android.core.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The single source of truth for every ongoing FlowIsland activity, regardless of
 * type. Nothing outside this class ever mutates activity state directly -- the
 * home screen, the notification bridge, the overlay service and the widget all
 * subscribe to [activities] and render the same snapshot.
 *
 * Deliberately free of any Android framework / UI dependency so it is trivially
 * unit-testable and so the "one source of truth" rule can't be quietly broken by
 * a feature reaching into NotificationManager or WindowManager state directly.
 */
@Singleton
class ActivityEngine @Inject constructor(
    @ApplicationScope private val applicationScope: CoroutineScope,
) {

    private val _activities = MutableStateFlow<Map<ActivityId, ActivityUiState>>(emptyMap())

    /** Ranked, visible (non-hidden) activities -- what everything should render. */
    val activities: StateFlow<List<ActivityUiState>> = _activities
        .mapLatest { map -> PriorityEngine.sort(map.values.filter { !it.hidden }) }
        .stateIn(applicationScope, SharingStarted.Eagerly, emptyList())

    fun snapshot(): List<ActivityUiState> = _activities.value.values.toList()

    fun get(id: ActivityId): ActivityUiState? = _activities.value[id]

    /** Live view of a single activity by id, for the few call sites (widget, overlay row) that want it directly. */
    fun observe(id: ActivityId): StateFlow<ActivityUiState?> =
        _activities.map { it[id] }.stateIn(applicationScope, SharingStarted.Eagerly, _activities.value[id])

    fun upsert(state: ActivityUiState) {
        _activities.update { it + (state.id to state.copy(updatedAt = System.currentTimeMillis())) }
    }

    fun update(id: ActivityId, transform: (ActivityUiState) -> ActivityUiState) {
        _activities.update { map ->
            val current = map[id] ?: return@update map
            map + (id to transform(current).copy(updatedAt = System.currentTimeMillis()))
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

    /** Fully removes the activity from the engine (used after a terminal state has been acknowledged). */
    fun dismiss(id: ActivityId) {
        _activities.update { it - id }
    }

    fun setProgress(id: ActivityId, progress: Float) = update(id) { it.copy(explicitProgress = progress.coerceIn(0f, 1f)) }
}
