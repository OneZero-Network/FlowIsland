package com.flowisland.android

import android.app.Application
import com.flowisland.android.core.activity.ActivityEngineObserver
import com.flowisland.android.core.notification.NotificationChannelInstaller
import com.flowisland.android.core.overlay.OverlayController
import com.flowisland.android.feature.cooking.CookingStepAdvancer
import com.flowisland.android.feature.fitness.FitnessSessionTracker
import com.flowisland.android.feature.trip.TripSessionTracker
import com.flowisland.android.feature.pomodoro.PomodoroCycleManager
import com.flowisland.android.feature.study.StudySessionRecorder
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class FlowIslandApp : Application() {

    @Inject lateinit var notificationChannelInstaller: NotificationChannelInstaller
    @Inject lateinit var activityEngineObserver: ActivityEngineObserver
    @Inject lateinit var pomodoroCycleManager: PomodoroCycleManager
    @Inject lateinit var cookingStepAdvancer: CookingStepAdvancer
    @Inject lateinit var studySessionRecorder: StudySessionRecorder
    @Inject lateinit var fitnessSessionTracker: FitnessSessionTracker
    @Inject lateinit var tripSessionTracker: TripSessionTracker
    @Inject lateinit var overlayController: OverlayController

    override fun onCreate() {
        super.onCreate()
        notificationChannelInstaller.installAll()
        activityEngineObserver.start()
        pomodoroCycleManager.start()
        cookingStepAdvancer.start()
        studySessionRecorder.start()
        fitnessSessionTracker.recover()
        tripSessionTracker.recover()
        overlayController.start()
    }
}
