package com.flowisland.android.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.flowisland.android.core.permissions.PermissionsManager
import com.flowisland.android.feature.ai.LocalTasksScreen
import com.flowisland.android.feature.cooking.CookingCreateScreen
import com.flowisland.android.feature.delivery.DeliveryCreateScreen
import com.flowisland.android.feature.detail.ActivityDetailScreen
import com.flowisland.android.feature.download.LocalExportsScreen
import com.flowisland.android.feature.expense.AddExpenseScreen
import com.flowisland.android.feature.expense.ExpenseCreateScreen
import com.flowisland.android.feature.fitness.FitnessCreateScreen
import com.flowisland.android.feature.flight.FlightCreateScreen
import com.flowisland.android.feature.history.HistoryScreen
import com.flowisland.android.feature.home.HomeScreen
import com.flowisland.android.feature.media.MediaSetupScreen
import com.flowisland.android.feature.onboarding.OnboardingScreen
import com.flowisland.android.feature.pomodoro.PomodoroCreateScreen
import com.flowisland.android.feature.privacy.PrivacyPolicyScreen
import com.flowisland.android.feature.reminders.ReminderCreateScreen
import com.flowisland.android.feature.settings.SettingsScreen
import com.flowisland.android.feature.stopwatch.StopwatchCreateScreen
import com.flowisland.android.feature.study.StudyCreateScreen
import com.flowisland.android.feature.switcher.SwitcherScreen
import com.flowisland.android.feature.timer.TimerCreateScreen
import com.flowisland.android.feature.trip.TripCreateScreen

@Composable
fun FlowIslandNavGraph(navController: NavHostController, startDestination: String, permissionsManager: PermissionsManager) {
    NavHost(navController = navController, startDestination = startDestination) {

        composable(Screen.Onboarding.route) {
            OnboardingScreen(onDone = { navController.navigate(Screen.Home.route) { popUpTo(Screen.Onboarding.route) { inclusive = true } } })
        }

        composable(Screen.Home.route) {
            HomeScreen(
                onOpenActivity = { id -> navController.navigate(Screen.Detail.createRoute(id)) },
                onQuickStart = { route -> navController.navigate(route) },
                onSettings = { navController.navigate(Screen.Settings.route) },
                onHistory = { navController.navigate(Screen.History.route) },
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(onBack = { navController.popBackStack() }, onPrivacyPolicy = { navController.navigate(Screen.PrivacyPolicy.route) })
        }
        composable(Screen.PrivacyPolicy.route) { PrivacyPolicyScreen(onBack = { navController.popBackStack() }) }
        composable(Screen.History.route) { HistoryScreen(onBack = { navController.popBackStack() }) }
        composable(Screen.Switcher.route) { SwitcherScreen(onOpenActivity = { id -> navController.navigate(Screen.Detail.createRoute(id)) }, onBack = { navController.popBackStack() }) }

        composable(Screen.NewTimer.route) { TimerCreateScreen(onStarted = { id -> navController.navigateToDetail(id) }, onBack = { navController.popBackStack() }) }
        composable(Screen.NewPomodoro.route) { PomodoroCreateScreen(onStarted = { id -> navController.navigateToDetail(id) }, onBack = { navController.popBackStack() }) }
        composable(Screen.NewStopwatch.route) { StopwatchCreateScreen(onStarted = { id -> navController.navigateToDetail(id) }, onBack = { navController.popBackStack() }) }
        composable(Screen.NewStudy.route) { StudyCreateScreen(onStarted = { id -> navController.navigateToDetail(id) }, onBack = { navController.popBackStack() }) }
        composable(Screen.NewCooking.route) { CookingCreateScreen(onStarted = { id -> navController.navigateToDetail(id) }, onBack = { navController.popBackStack() }) }
        composable(Screen.NewFitness.route) { FitnessCreateScreen(onStarted = { id -> navController.navigateToDetail(id) }, onBack = { navController.popBackStack() }) }
        composable(Screen.NewTrip.route) { TripCreateScreen(onStarted = { id -> navController.navigateToDetail(id) }, onBack = { navController.popBackStack() }) }
        composable(Screen.NewReminder.route) { ReminderCreateScreen(onStarted = { id -> navController.navigateToDetail(id) }, onBack = { navController.popBackStack() }) }
        composable(Screen.NewDelivery.route) { DeliveryCreateScreen(onStarted = { id -> navController.navigateToDetail(id) }, onBack = { navController.popBackStack() }) }
        composable(Screen.NewFlight.route) { FlightCreateScreen(onStarted = { id -> navController.navigateToDetail(id) }, onBack = { navController.popBackStack() }) }
        composable("local_exports") { LocalExportsScreen(onStarted = { id -> navController.navigateToDetail(id) }, onBack = { navController.popBackStack() }) }
        composable(Screen.LocalTasks.route) { LocalTasksScreen(onStarted = { id -> navController.navigateToDetail(id) }, onBack = { navController.popBackStack() }) }
        composable(Screen.MediaSetup.route) { MediaSetupScreen(permissionsManager = permissionsManager, onDone = { navController.popBackStack() }) }

        composable(Screen.NewExpense.route) { ExpenseCreateScreen(onStarted = { id -> navController.navigateToDetail(id) }, onBack = { navController.popBackStack() }) }
        composable(
            Screen.ExpenseTripDetail.route,
            arguments = listOf(navArgument(Screen.ExpenseTripDetail.ARG_TRIP_ID) { type = NavType.StringType }),
        ) { backStackEntry ->
            val tripId = backStackEntry.arguments?.getString(Screen.ExpenseTripDetail.ARG_TRIP_ID).orEmpty()
            AddExpenseScreen(activityId = "", tripId = tripId, currencyCode = "INR", onBack = { navController.popBackStack() })
        }

        composable(
            Screen.Detail.route,
            arguments = listOf(navArgument(Screen.Detail.ARG_ACTIVITY_ID) { type = NavType.StringType }),
        ) { backStackEntry ->
            val activityId = backStackEntry.arguments?.getString(Screen.Detail.ARG_ACTIVITY_ID).orEmpty()
            ActivityDetailScreen(
                activityId = activityId,
                onBack = { navController.popBackStack() },
                onAddExpense = { actId, tripId -> navController.navigate("add_expense/$actId/$tripId") },
            )
        }

        composable(
            "add_expense/{activityId}/{tripId}",
            arguments = listOf(navArgument("activityId") { type = NavType.StringType }, navArgument("tripId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val activityId = backStackEntry.arguments?.getString("activityId").orEmpty()
            val tripId = backStackEntry.arguments?.getString("tripId").orEmpty()
            AddExpenseScreen(activityId = activityId, tripId = tripId, currencyCode = "INR", onBack = { navController.popBackStack() })
        }
    }
}

private fun NavHostController.navigateToDetail(activityId: String) {
    navigate(Screen.Detail.createRoute(activityId)) {
        popUpTo(Screen.Home.route)
    }
}
