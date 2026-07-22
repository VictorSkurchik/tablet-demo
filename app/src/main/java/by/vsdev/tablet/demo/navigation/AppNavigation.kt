package by.vsdev.tablet.demo.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import by.vsdev.tablet.demo.ui.presentation.setup.SetupRoute
import by.vsdev.tablet.demo.ui.presentation.table.ui.TableRoute

@Composable
internal fun AppRoot() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = SetupDestination) {
        composable<SetupDestination> {
            SetupRoute(
                onNavigateToTable = { config ->
                    navController.navigate(TableDestination(config)) {
                        launchSingleTop = true
                    }
                },
            )
        }
        composable<TableDestination> { entry ->
            val destination = entry.toRoute<TableDestination>()
            TableRoute(
                config = destination.toConfig(),
                onNavigateUp = navController::navigateUp,
            )
        }
    }
}
