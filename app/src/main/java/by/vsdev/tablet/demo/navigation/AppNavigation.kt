package by.vsdev.tablet.demo.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import by.vsdev.tablet.demo.R
import by.vsdev.tablet.demo.ui.presentation.setup.SetupRoute
import by.vsdev.tablet.demo.ui.presentation.table.ui.TableRoute

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
internal fun AppRoot(
    windowAdaptiveInfo: WindowAdaptiveInfo =
        currentWindowAdaptiveInfo(supportLargeAndXLargeWidth = true),
) {
    val navController = rememberNavController()

    Surface(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            NavHost(navController = navController, startDestination = SetupDestination) {
                composable<SetupDestination> {
                    SetupRoute(
                        onNavigateToTable = { config ->
                            navController.navigate(TableDestination(config)) {
                                launchSingleTop = true
                            }
                        },
                        windowAdaptiveInfo = windowAdaptiveInfo,
                    )
                }
                composable<TableDestination> { entry ->
                    val destination = entry.toRoute<TableDestination>()
                    TableRoute(
                        config = destination.toConfig(),
                        onNavigateUp = navController::navigateUp,
                        windowAdaptiveInfo = windowAdaptiveInfo,
                    )
                }
            }
            Text(
                text = stringResource(R.string.developer_credit),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier =
                    Modifier
                        .align(Alignment.BottomEnd)
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                        .padding(8.dp),
            )
        }
    }
}
