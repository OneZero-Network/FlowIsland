package com.flowisland.android.feature.delivery

import com.flowisland.android.core.activity.ActivityEngine
import com.flowisland.android.core.activity.model.ActivityId
import com.flowisland.android.core.database.DeliveryDao
import com.flowisland.android.core.di.ApplicationScope
import com.flowisland.android.core.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeliveryStatusManager @Inject constructor(
    private val activityEngine: ActivityEngine,
    private val deliveryDao: DeliveryDao,
    @ApplicationScope private val scope: CoroutineScope,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    /** Status is tracked by the exact enum-name-derived subtitle text set at each transition. */
    fun advance(activityId: String) {
        scope.launch {
            val current = activityEngine.get(ActivityId(activityId)) ?: return@launch
            val currentStatus = DeliveryStatus.entries.find {
                it.name == current.subtitle?.uppercase()?.replace(" ", "_")
            } ?: DeliveryStatus.ORDERED
            val next = currentStatus.next()
            if (next == null) {
                withContext(ioDispatcher) { deliveryDao.updateStatus(activityId, DeliveryStatus.DELIVERED.name, System.currentTimeMillis()) }
                activityEngine.complete(ActivityId(activityId))
                return@launch
            }
            withContext(ioDispatcher) { deliveryDao.updateStatus(activityId, next.name, System.currentTimeMillis()) }
            activityEngine.update(ActivityId(activityId)) {
                it.copy(subtitle = next.name.replace("_", " ").lowercase().replaceFirstChar(Char::uppercase))
            }
        }
    }
}
