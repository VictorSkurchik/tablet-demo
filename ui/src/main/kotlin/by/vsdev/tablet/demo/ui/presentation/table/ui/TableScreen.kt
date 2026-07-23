package by.vsdev.tablet.demo.ui.presentation.table.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.ReportDrawnWhen
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.PaneAdaptedValue
import androidx.compose.material3.adaptive.layout.SupportingPaneScaffold
import androidx.compose.material3.adaptive.layout.SupportingPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.ThreePaneScaffoldNavigator
import androidx.compose.material3.adaptive.navigation.rememberSupportingPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import by.vsdev.tablet.demo.domain.model.TableConfig
import by.vsdev.tablet.demo.ui.R
import by.vsdev.tablet.demo.ui.components.molecules.ErrorContent
import by.vsdev.tablet.demo.ui.components.molecules.LoadingContent
import by.vsdev.tablet.demo.ui.presentation.table.CellUiState
import by.vsdev.tablet.demo.ui.presentation.table.TableIntent
import by.vsdev.tablet.demo.ui.presentation.table.TableLoadState
import by.vsdev.tablet.demo.ui.presentation.table.TableUiState
import by.vsdev.tablet.demo.ui.presentation.table.TableViewModel
import by.vsdev.tablet.demo.ui.theme.AppTheme
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

private const val EDITOR_RESIZE_ANIMATION_DURATION_MILLIS = 350
private const val EXPANDED_EDITOR_HEIGHT_FRACTION = 0.5f

internal fun calculateEditorPaneTargetHeight(
    shouldExpand: Boolean,
    maxHeight: Dp,
    defaultHeight: Dp,
): Dp = if (shouldExpand) maxHeight * EXPANDED_EDITOR_HEIGHT_FRACTION else defaultHeight

@Composable
fun TableRoute(
    config: TableConfig,
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = koinViewModel<TableViewModel> { parametersOf(config) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    ReportDrawnWhen { !state.isLoading }
    TableScreen(
        state = state,
        onIntent = viewModel::onIntent,
        onNavigateUp = onNavigateUp,
        modifier = modifier,
    )
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3AdaptiveApi::class)
@Composable
internal fun TableScreen(
    state: TableUiState,
    onIntent: (TableIntent) -> Unit,
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val navigator = rememberSupportingPaneScaffoldNavigator()
    val imeBottom = WindowInsets.ime.getBottom(LocalDensity.current)
    val isOnScreenKeyboardVisible = WindowInsets.isImeVisible

    LaunchedEffect(state.editingIndex) {
        val supportingHidden =
            navigator.scaffoldValue[SupportingPaneScaffoldRole.Supporting] == PaneAdaptedValue.Hidden
        if (state.editingIndex != null && supportingHidden) {
            navigator.navigateTo(SupportingPaneScaffoldRole.Supporting)
        } else if (state.editingIndex == null && navigator.canNavigateBack()) {
            navigator.navigateBack()
        }
    }

    BackHandler(enabled = state.editingIndex != null) { onIntent(TableIntent.EditDismissed) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TableTopBar(
                rows = state.config.rows,
                columns = state.config.columns,
                isEditing = state.editingIndex != null,
                onNavigateUp = onNavigateUp,
                onDismissEditor = { onIntent(TableIntent.EditDismissed) },
            )
        },
    ) { innerPadding ->
        TableAdaptiveContent(
            state = state,
            onIntent = onIntent,
            navigator = navigator,
            isOnScreenKeyboardVisible = isOnScreenKeyboardVisible,
            imeBottom = imeBottom,
            modifier = Modifier.fillMaxSize().padding(innerPadding),
        )
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
private fun TableAdaptiveContent(
    state: TableUiState,
    onIntent: (TableIntent) -> Unit,
    navigator: ThreePaneScaffoldNavigator<Any>,
    isOnScreenKeyboardVisible: Boolean,
    imeBottom: Int,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier) {
        val shouldExpandEditor =
            state.editingIndex != null &&
                isOnScreenKeyboardVisible &&
                imeBottom > 0
        val editorPaneHeight by
            animateDpAsState(
                targetValue =
                    calculateEditorPaneTargetHeight(
                        shouldExpand = shouldExpandEditor,
                        maxHeight = maxHeight,
                        defaultHeight = navigator.scaffoldDirective.defaultPanePreferredHeight,
                    ),
                animationSpec =
                    tween(
                        durationMillis = EDITOR_RESIZE_ANIMATION_DURATION_MILLIS,
                        easing = FastOutSlowInEasing,
                    ),
                label = "editorPaneHeight",
            )

        SupportingPaneScaffold(
            modifier = Modifier.fillMaxSize(),
            directive = navigator.scaffoldDirective,
            value = navigator.scaffoldValue,
            mainPane = {
                AnimatedPane {
                    TableMainPane(
                        loadState = state.loadState,
                        columns = state.config.columns,
                        onIntent = onIntent,
                    )
                }
            },
            supportingPane = {
                AnimatedPane(modifier = Modifier.preferredHeight(editorPaneHeight)) {
                    val index = state.editingIndex
                    EditorPane(
                        index = index,
                        currentText = index?.let { state.cells.getOrNull(it)?.text },
                        onConfirm = { i, text -> onIntent(TableIntent.EditConfirmed(i, text)) },
                        onDismiss = { onIntent(TableIntent.EditDismissed) },
                    )
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TableTopBar(
    rows: Int,
    columns: Int,
    isEditing: Boolean,
    onNavigateUp: () -> Unit,
    onDismissEditor: () -> Unit,
) {
    TopAppBar(
        title = { Text(stringResource(R.string.table_title, rows, columns)) },
        navigationIcon = {
            IconButton(onClick = if (isEditing) onDismissEditor else onNavigateUp) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription =
                        stringResource(
                            if (isEditing) R.string.table_close_editor else R.string.table_back,
                        ),
                )
            }
        },
    )
}

@Composable
private fun TableMainPane(
    loadState: TableLoadState,
    columns: Int,
    onIntent: (TableIntent) -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
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
                    modifier = Modifier.align(Alignment.Center),
                )

            is TableLoadState.Content ->
                TableGrid(columns = columns, cells = loadState.cells, onIntent = onIntent)
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
