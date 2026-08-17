package com.flowisland.android.core.activity

import com.flowisland.android.R
import com.flowisland.android.core.activity.model.ActivityAction

/** Shared action-list builders so every feature doesn't hand-roll the same PAUSE/FINISH pairs. */
object ActivityActions {

    fun countdownRunning(includeAddMinute: Boolean = true) = buildList {
        if (includeAddMinute) add(ActivityAction("add1", R.string.action_add_1_min, ActivityAction.Kind.ADD_1_MIN))
        add(ActivityAction("pause", R.string.action_pause, ActivityAction.Kind.PAUSE))
        add(ActivityAction("finish", R.string.action_finish, ActivityAction.Kind.FINISH))
    }

    fun countdownPaused() = listOf(
        ActivityAction("resume", R.string.action_resume, ActivityAction.Kind.RESUME),
        ActivityAction("cancel", R.string.action_cancel, ActivityAction.Kind.CANCEL),
    )

    fun stopwatchRunning() = listOf(
        ActivityAction("lap", R.string.action_lap, ActivityAction.Kind.LAP),
        ActivityAction("pause", R.string.action_pause, ActivityAction.Kind.PAUSE),
        ActivityAction("finish", R.string.action_finish, ActivityAction.Kind.FINISH),
    )

    fun stopwatchPaused() = listOf(
        ActivityAction("resume", R.string.action_resume, ActivityAction.Kind.RESUME),
        ActivityAction("finish", R.string.action_finish, ActivityAction.Kind.FINISH),
    )

    fun simpleFinishCancel() = listOf(
        ActivityAction("finish", R.string.action_finish, ActivityAction.Kind.FINISH),
        ActivityAction("cancel", R.string.action_cancel, ActivityAction.Kind.CANCEL),
    )

    fun reminderActions() = listOf(
        ActivityAction("done", R.string.action_done, ActivityAction.Kind.DONE),
        ActivityAction("snooze", R.string.action_snooze, ActivityAction.Kind.SNOOZE),
    )

    fun cancelOnly() = listOf(ActivityAction("cancel", R.string.action_cancel, ActivityAction.Kind.CANCEL))

    fun viewResult() = listOf(ActivityAction("view_result", R.string.action_view_result, ActivityAction.Kind.VIEW_RESULT))
}
