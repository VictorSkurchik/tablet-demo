package by.vsdev.tablet.demo.ui.presentation.table.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.ReportDrawnWhen
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.SupportingPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.BackNavigationBehavior
import androidx.compose.material3.adaptive.navigation.rememberSupportingPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import by.vsdev.tablet.demo.domain.model.TableConfig
import by.vsdev.tablet.demo.ui.R
import by.vsdev.tablet.demo.ui.components.layout.calculateAppPaneScaffoldDirective
import by.vsdev.tablet.demo.ui.components.molecules.ErrorContent
import by.vsdev.tablet.demo.ui.components.molecules.LoadingContent
import by.vsdev.tablet.demo.ui.presentation.table.CellUiState
import by.vsdev.tablet.demo.ui.presentation.table.TableIntent
import by.vsdev.tablet.demo.ui.presentation.table.TableLoadState
import by.vsdev.tablet.demo.ui.presentation.table.TableUiState
import by.vsdev.tablet.demo.ui.presentation.table.TableViewModel
import by.vsdev.tablet.demo.ui.theme.AppSpacing
import by.vsdev.tablet.demo.ui.theme.AppTheme
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun TableRoute(
    config: TableConfig,
    recoverySessionId: String,
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier,
    windowAdaptiveInfo: WindowAdaptiveInfo =
        currentWindowAdaptiveInfo(supportLargeAndXLargeWidth = true),
) {
    val viewModel =
        koinViewModel<TableViewModel> {
            parametersOf(config, recoverySessionId)
        }
    val state by viewModel.state.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        viewModel.onIntent(TableIntent.AppBackgrounded)
    }
    ReportDrawnWhen { state.loadState != TableLoadState.Loading }
    TableScreen(
        state = state,
        onIntent = viewModel::onIntent,
        onNavigateUp = { viewModel.closeSession(onNavigateUp) },
        modifier = modifier,
        windowAdaptiveInfo = windowAdaptiveInfo,
    )
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
internal fun TableScreen(
    state: TableUiState,
    onIntent: (TableIntent) -> Unit,
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier,
    windowAdaptiveInfo: WindowAdaptiveInfo =
        currentWindowAdaptiveInfo(supportLargeAndXLargeWidth = true),
) {
    val density = LocalDensity.current
    val paneDirective =
        remember(windowAdaptiveInfo, density) {
            calculateAppPaneScaffoldDirective(windowAdaptiveInfo, density)
        }
    val navigator =
        rememberSupportingPaneScaffoldNavigator<Int>(
            scaffoldDirective = paneDirective,
        )
    val screenTitle = stringResource(R.string.table_title, state.config.rows, state.config.columns)
    var lastEditingIndex by remember { mutableStateOf<Int?>(null) }
    var restoreFocusIndex by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(state.editingIndex) {
        if (state.editingIndex != null) {
            lastEditingIndex = state.editingIndex
            restoreFocusIndex = null
        } else if (lastEditingIndex != null) {
            restoreFocusIndex = lastEditingIndex
            lastEditingIndex = null
        }
        val supportingIsCurrentDestination =
            navigator.currentDestination?.pane == SupportingPaneScaffoldRole.Supporting
        if (state.editingIndex != null && !supportingIsCurrentDestination) {
            navigator.navigateTo(
                pane = SupportingPaneScaffoldRole.Supporting,
                contentKey = state.editingIndex,
            )
        } else if (
            state.editingIndex == null &&
            supportingIsCurrentDestination
        ) {
            navigator.navigateBack(BackNavigationBehavior.PopUntilCurrentDestinationChange)
        }
    }

    BackHandler {
        if (state.editingIndex != null) {
            onIntent(TableIntent.EditDismissed)
        } else {
            onNavigateUp()
        }
    }

    TablePaneLayout(
        state = state,
        navigator = navigator,
        mainPaneTitle = screenTitle,
        restoreFocusIndex = restoreFocusIndex,
        windowAdaptiveInfo = windowAdaptiveInfo,
        onIntent = onIntent,
        onNavigateUp = onNavigateUp,
        modifier = modifier.fillMaxSize(),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TableTopBar(
    rows: Int,
    columns: Int,
    onNavigateUp: () -> Unit,
) {
    TopAppBar(
        title = {
            Text(
                stringResource(R.string.table_title, rows, columns),
                modifier = Modifier.semantics { heading() },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        navigationIcon = {
            IconButton(onClick = onNavigateUp) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.table_back),
                )
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditorTopBar(
    canDismiss: Boolean,
    onDismiss: () -> Unit,
) {
    TopAppBar(
        title = {
            Text(
                text = stringResource(R.string.editor_title),
                modifier = Modifier.semantics { heading() },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        navigationIcon = {
            if (canDismiss) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.table_close_editor),
                    )
                }
            }
        },
    )
}

@Composable
internal fun TableMainScaffold(
    state: TableUiState,
    restoreFocusIndex: Int?,
    onIntent: (TableIntent) -> Unit,
    onNavigateUp: () -> Unit,
) {
    Scaffold(
        topBar = {
            TableTopBar(
                rows = state.config.rows,
                columns = state.config.columns,
                onNavigateUp = onNavigateUp,
            )
        },
    ) { innerPadding ->
        TableMainPane(
            loadState = state.loadState,
            columns = state.config.columns,
            restoreFocusIndex = restoreFocusIndex,
            onIntent = onIntent,
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding),
        )
    }
}

@Composable
internal fun EditorScaffold(
    index: Int?,
    currentText: String?,
    draftState: TextFieldState,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    Scaffold(
        topBar = {
            EditorTopBar(
                canDismiss = index != null,
                onDismiss = onDismiss,
            )
        },
    ) { innerPadding ->
        EditorPane(
            index = index,
            currentText = currentText,
            draftState = draftState,
            onConfirm = onConfirm,
            onDismiss = onDismiss,
            showTitle = false,
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding),
        )
    }
}

@Composable
private fun TableMainPane(
    loadState: TableLoadState,
    columns: Int,
    restoreFocusIndex: Int?,
    onIntent: (TableIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        when (loadState) {
            TableLoadState.Loading ->
                LoadingContent(
                    message = stringResource(R.string.table_loading),
                    modifier = Modifier.align(Alignment.Center),
                )

            TableLoadState.Error ->
                ErrorContent(
                    message = stringResource(R.string.table_load_error),
                    retryLabel = stringResource(R.string.table_retry),
                    onRetry = { onIntent(TableIntent.RetryLoad) },
                    modifier = Modifier.align(Alignment.Center).padding(AppSpacing.large),
                )

            is TableLoadState.Content ->
                TableGrid(
                    columns = columns,
                    cells = loadState.cells,
                    restoreFocusIndex = restoreFocusIndex,
                    onIntent = onIntent,
                )
        }
    }
}

private fun tableScreenPreviewCells(count: Int): List<CellUiState> =
    List(count) { CellUiState(text = "Cell $it", isSelected = it % 3 == 0) }

@Preview(name = "Table – populated", widthDp = 900, heightDp = 600)
@Composable
private fun TableScreenPopulatedPreview() {
    AppTheme {
        TableScreen(
            state =
                TableUiState(
                    config = TableConfig(rows = 4, columns = 3),
                    loadState = TableLoadState.Content(tableScreenPreviewCells(12)),
                ),
            onIntent = {},
            onNavigateUp = {},
        )
    }
}

@Preview(name = "Table – editor open", widthDp = 900, heightDp = 600)
@Composable
private fun TableScreenEditorPreview() {
    AppTheme {
        TableScreen(
            state =
                TableUiState(
                    config = TableConfig(rows = 4, columns = 3),
                    loadState = TableLoadState.Content(tableScreenPreviewCells(12)),
                    editingIndex = 0,
                    editorDraft = "Cell 0",
                ),
            onIntent = {},
            onNavigateUp = {},
        )
    }
}

@Preview(name = "Table – split screen", widthDp = 600, heightDp = 720, fontScale = 1.3f)
@Composable
private fun TableScreenSplitPreview() {
    AppTheme {
        TableScreen(
            state =
                TableUiState(
                    config = TableConfig(rows = 8, columns = 6),
                    loadState = TableLoadState.Content(tableScreenPreviewCells(48)),
                ),
            onIntent = {},
            onNavigateUp = {},
        )
    }
}

@Preview(name = "Table – loading")
@Composable
private fun TableScreenLoadingPreview() {
    AppTheme {
        TableScreen(
            state = TableUiState(config = TableConfig(rows = 4, columns = 3)),
            onIntent = {},
            onNavigateUp = {},
        )
    }
}
