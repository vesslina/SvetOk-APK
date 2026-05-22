package ru.svetok.app.ui.nav

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import ru.svetok.app.data.onboarding.OnboardingPrefs
import ru.svetok.app.ui.admin.AdminViewModel
import ru.svetok.app.ui.admin.CreateOutageScreen
import ru.svetok.app.ui.complaint.ComplaintScreen
import ru.svetok.app.ui.map.MapScreen
import ru.svetok.app.ui.onboarding.OnboardingScreen
import ru.svetok.app.ui.outages.OutagesListScreen
import ru.svetok.app.ui.settings.SettingsScreen

private const val ONBOARDING_ROUTE    = "onboarding"
private const val MAP_ROUTE           = "map"
private const val COMPLAINT_ROUTE     = "complaint?street={street}"
private const val SETTINGS_ROUTE      = "settings"
private const val OUTAGES_LIST_ROUTE  = "outages_list"
private const val CREATE_OUTAGE_ROUTE = "create_outage"

@Composable
fun SvetOkNavGraph() {
    val navController    = rememberNavController()
    val adminViewModel: AdminViewModel = koinViewModel()
    val onboardingPrefs: OnboardingPrefs = koinInject()

    val startDestination = if (onboardingPrefs.isCompleted) MAP_ROUTE else ONBOARDING_ROUTE

    NavHost(navController = navController, startDestination = startDestination) {

        composable(ONBOARDING_ROUTE) {
            OnboardingScreen(
                onFinish = {
                    navController.navigate(MAP_ROUTE) {
                        popUpTo(ONBOARDING_ROUTE) { inclusive = true }
                    }
                },
            )
        }

        composable(MAP_ROUTE) {
            MapScreen(
                onReportStreet = { street ->
                    navController.navigate("complaint?street=${Uri.encode(street)}")
                },
                onOpenSettings = { navController.navigate(SETTINGS_ROUTE) },
                onOpenAdminPanel = { navController.navigate(OUTAGES_LIST_ROUTE) },
                onOpenOutagesList = { navController.navigate(OUTAGES_LIST_ROUTE) },
                adminViewModel = adminViewModel,
            )
        }

        composable(
            route = COMPLAINT_ROUTE,
            arguments = listOf(
                navArgument("street") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = ""
                }
            ),
        ) { backStackEntry ->
            ComplaintScreen(
                streetName = backStackEntry.arguments?.getString("street").orEmpty(),
                onBack = { navController.popBackStack() },
            )
        }

        composable(SETTINGS_ROUTE) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }

        composable(OUTAGES_LIST_ROUTE) {
            OutagesListScreen(
                onBack = { navController.popBackStack() },
                onCreateOutage = { navController.navigate(CREATE_OUTAGE_ROUTE) },
            )
        }

        composable(CREATE_OUTAGE_ROUTE) {
            CreateOutageScreen(
                viewModel = adminViewModel,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
