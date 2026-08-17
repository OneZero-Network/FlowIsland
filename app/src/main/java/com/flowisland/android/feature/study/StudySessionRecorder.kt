package com.flowisland.android.feature.study

import com.flowisland.android.core.activity.ActivityEngine
import com.flowisland.android.core.activity.model.ActivityId
import com.flowisland.android.core.activity.model.ActivityState
import com.flowisland.android.core.activity.model.ActivityType
import com.flowisland.android.core.database.StudySessionDao
import com.flowisland.android.core.database.StudySessionEntity
import com.flowisland.android.core.di.ApplicationScope
import com.flowisland.android.core.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
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
            activityEngine.activities.distinctUntilChanged().collect { list ->
                list.filter { it.type == ActivityType.STUDY && it.state.isTerminal }
                    .forEach { record(it.id.value, completed = it.state == ActivityState.COMPLETED) }
            }
        }
    }

    private fun record(activityId: String, completed: Boolean) {
        if (activityId in recorded) return
        recorded += activityId
        val (subject, startedAt, plannedDuration) = StudySessionRegistry.consume(activityId) ?: return
        scope.launch {
            withContext(ioDispatcher) {
                studySessionDao.insert(
                    StudySessionEntity(
                        id = activityId,
                        subject = subject,
                        plannedDurationMillis = plannedDuration,
                        actualDurationMillis = System.currentTimeMillis() - startedAt,
                        startedAt = startedAt,
                        endedAt = System.currentTimeMillis(),
                        completed = completed,
                    )
                )
            }
        }
    }
}
