package by.vsdev.tablet.demo.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import by.vsdev.tablet.demo.recovery.RecoverySessionIdFactory
import by.vsdev.tablet.demo.recovery.restoreOrCreate
import by.vsdev.tablet.demo.ui.presentation.setup.SetupRoute
import by.vsdev.tablet.demo.ui.presentation.table.ui.TableRoute

@Composable
internal fun AppRoot(recoverySessionIdFactory: RecoverySessionIdFactory) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = SetupDestination) {
        composable<SetupDestination> {
            SetupRoute(
                onNavigateToTable = { config ->
                    navController.navigate(
                        TableDestination(config, recoverySessionIdFactory.newSessionId()),
                    ) {
                        launchSingleTop = true
                    }
                },
            )
        }
        composable<TableDestination> { entry ->
            val destination = entry.toRoute<TableDestination>()
            val recoverySessionId =
                rememberSaveable(destination.recoverySessionId) {
                    recoverySessionIdFactory.restoreOrCreate(destination.recoverySessionId)
                }
            TableRoute(
                config = destination.toConfig(),
                recoverySessionId = recoverySessionId,
                onNavigateUp = navController::navigateUp,
            )
        }
    }
}
