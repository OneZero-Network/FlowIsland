package com.flowisland.android.navigation

sealed class Screen(val route: String) {
    data object Onboarding : Screen("onboarding")
    data object Home : Screen("home")
    data object Settings : Screen("settings")
    data object PrivacyPolicy : Screen("privacy_policy")
    data object Switcher : Screen("switcher")
    data object History : Screen("history")

    data object NewTimer : Screen("new_timer")
    data object NewPomodoro : Screen("new_pomodoro")
    data object NewStopwatch : Screen("new_stopwatch")
    data object MediaSetup : Screen("media_setup")
    data object NewStudy : Screen("new_study")
    data object NewCooking : Screen("new_cooking")
    data object NewFitness : Screen("new_fitness")
    data object NewTrip : Screen("new_trip")
    data object NewExpense : Screen("new_expense")
    data object NewReminder : Screen("new_reminder")
    data object NewDelivery : Screen("new_delivery")
    data object NewFlight : Screen("new_flight")
    data object LocalTasks : Screen("local_tasks")

    data object Detail : Screen("detail/{activityId}") {
        fun createRoute(activityId: String) = "detail/$activityId"
        const val ARG_ACTIVITY_ID = "activityId"
    }

    data object ExpenseTripDetail : Screen("expense_trip/{tripId}") {
        fun createRoute(tripId: String) = "expense_trip/$tripId"
        const val ARG_TRIP_ID = "tripId"
    }
}
