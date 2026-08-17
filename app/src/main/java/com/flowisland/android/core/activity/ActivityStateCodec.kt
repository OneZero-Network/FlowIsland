package com.flowisland.android.core.activity

import com.flowisland.android.core.activity.model.ActivityAction
import com.flowisland.android.core.activity.model.ActivityIconId
import com.flowisland.android.core.activity.model.ActivityId
import com.flowisland.android.core.activity.model.ActivityState
import com.flowisland.android.core.activity.model.ActivityType
import com.flowisland.android.core.activity.model.ActivityUiState
import com.flowisland.android.core.database.ActiveActivityEntity
import com.flowisland.android.core.time.TimerSpec
import org.json.JSONArray
import org.json.JSONObject

/** Converts the immutable engine snapshot to/from the durable Room representation. */
object ActivityStateCodec {

    fun toEntity(state: ActivityUiState): ActiveActivityEntity = ActiveActivityEntity(
        id = state.id.value,
        type = state.type.name,
        title = state.title,
        subtitle = state.subtitle,
        icon = state.icon.name,
        state = state.state.name,
        timerDurationMillis = state.timer?.durationMillis,
        timerStartedAtElapsedRealtime = state.timer?.startedAtElapsedRealtime,
        timerStartedAtWallClockMillis = state.timer?.startedAtWallClockMillis,
        timerAccumulatedPausedMillis = state.timer?.accumulatedPausedMillis,
        timerPausedAtElapsedRealtime = state.timer?.pausedAtElapsedRealtime,
        timerPausedAtWallClockMillis = state.timer?.pausedAtWallClockMillis,
        timerCountUp = state.timer?.countUp,
        explicitProgress = state.explicitProgress,
        pinned = state.pinned,
        hidden = state.hidden,
        createdAt = state.createdAt,
        updatedAt = state.updatedAt,
        lastInteractedAt = state.lastInteractedAt,
        payloadId = state.payloadId,
        expirationTime = state.expirationTime,
        actionsJson = JSONArray().apply {
            state.actions.forEach { action ->
                put(JSONObject().apply {
                    put("id", action.id)
                    put("labelResId", action.labelResId)
                    put("kind", action.kind.name)
                })
            }
        }.toString(),
    )

    fun fromEntity(entity: ActiveActivityEntity): ActivityUiState? = runCatching {
        val timer = if (entity.timerDurationMillis != null && entity.timerStartedAtElapsedRealtime != null && entity.timerStartedAtWallClockMillis != null) {
            TimerSpec(
                durationMillis = entity.timerDurationMillis,
                startedAtElapsedRealtime = entity.timerStartedAtElapsedRealtime,
                startedAtWallClockMillis = entity.timerStartedAtWallClockMillis,
                accumulatedPausedMillis = entity.timerAccumulatedPausedMillis ?: 0L,
                pausedAtElapsedRealtime = entity.timerPausedAtElapsedRealtime,
                pausedAtWallClockMillis = entity.timerPausedAtWallClockMillis,
                countUp = entity.timerCountUp ?: false,
            )
        } else null

        val actionsJson = JSONArray(entity.actionsJson)
        val actions = buildList {
            for (index in 0 until actionsJson.length()) {
                val item = actionsJson.getJSONObject(index)
                add(
                    ActivityAction(
                        id = item.getString("id"),
                        labelResId = item.getInt("labelResId"),
                        kind = ActivityAction.Kind.valueOf(item.getString("kind")),
                    )
                )
            }
        }

        ActivityUiState(
            id = ActivityId(entity.id),
            type = ActivityType.valueOf(entity.type),
            title = entity.title,
            subtitle = entity.subtitle,
            icon = ActivityIconId.valueOf(entity.icon),
            state = ActivityState.valueOf(entity.state),
            timer = timer,
            explicitProgress = entity.explicitProgress,
            actions = actions,
            pinned = entity.pinned,
            hidden = entity.hidden,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            lastInteractedAt = entity.lastInteractedAt,
            payloadId = entity.payloadId,
            expirationTime = entity.expirationTime,
        )
    }.getOrNull()
}
