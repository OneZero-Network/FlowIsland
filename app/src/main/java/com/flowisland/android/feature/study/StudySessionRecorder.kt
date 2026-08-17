package com.flowisland.android.feature.study

import com.flowisland.android.core.activity.ActivityEngine
import com.flowisland.android.core.activity.model.ActivityId
import com.flowisland.android.core.activity.model.ActivityState
import com.flowisland.android.core.activity.model.ActivityType
import com.flowisland.android.core.activity.model.ActivityUiState
import com.flowisland.android.core.database.StudySessionDao
import com.flowisland.android.core.database.StudySessionEntity
import com.flowisland.android.core.di.ApplicationScope
import com.flowisland.android.core.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StudySessionRecorder @Inject constructor(
    private val activityEngine: ActivityEngine,
    private val studySessionDao: StudySessionDao,
    @ApplicationScope private val scope: CoroutineScope,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    private val recorded = mutableSetOf<String>()

    fun start() {
        scope.launch {
            activityEngine.activities.collect { list ->
                list.filter { it.type == ActivityType.STUDY && it.state.isTerminal }
                    .forEach { record(it, completed = it.state == ActivityState.COMPLETED) }
            }
        }
    }

    private fun record(state: ActivityUiState, completed: Boolean) {
        val activityId = state.id.value
        if (activityId in recorded) return
        recorded += activityId
        val startedAt = state.createdAt
        val plannedDuration = state.timer?.durationMillis ?: 0L
        val endedAt = System.currentTimeMillis()
        scope.launch {
            withContext(ioDispatcher) {
                studySessionDao.insert(
                    StudySessionEntity(
                        id = activityId,
                        subject = state.title,
                        plannedDurationMillis = plannedDuration,
                        actualDurationMillis = (endedAt - startedAt).coerceAtLeast(0L),
                        startedAt = startedAt,
                        endedAt = endedAt,
                        completed = completed,
                    )
                )
            }
        }
    }
}
