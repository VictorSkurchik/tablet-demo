package by.vsdev.tablet.demo.ui.presentation.table.ui

import androidx.compose.foundation.layout.recalculateWindowInsets
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.SupportingPaneScaffold
import androidx.compose.material3.adaptive.layout.rememberPaneExpansionState
import androidx.compose.material3.adaptive.navigation.ThreePaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import by.vsdev.tablet.demo.ui.R
import by.vsdev.tablet.demo.ui.components.layout.AppPaneDragHandle
import by.vsdev.tablet.demo.ui.components.layout.BALANCED_PANE_ANCHOR_INDEX
import by.vsdev.tablet.demo.ui.components.layout.EQUAL_VERTICAL_PANE_HEIGHT_FRACTION
import by.vsdev.tablet.demo.ui.components.layout.HorizontalHingePaneScaffold
import by.vsdev.tablet.demo.ui.components.layout.StandardPaneExpansionAnchors
import by.vsdev.tablet.demo.ui.components.layout.horizontalSeparatingHingeBounds
import by.vsdev.tablet.demo.ui.components.layout.supportsHorizontalPaneExpansion
import by.vsdev.tablet.demo.ui.presentation.table.MAX_CELL_TEXT_LENGTH
import by.vsdev.tablet.demo.ui.presentation.table.TableIntent
import by.vsdev.tablet.demo.ui.presentation.table.TableUiState

internal const val TABLE_MAIN_PANE_TAG = "tableMainPane"
internal const val TABLE_EDITOR_PANE_TAG = "tableEditorPane"
internal const val TABLE_PANE_DRAG_HANDLE_TAG = "tablePaneDragHandle"

private const val MAIN_PANE_STATE_KEY = "mainPane"
private const val EDITOR_PANE_STATE_KEY = "editorPane"

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
internal fun TablePaneLayout(
    state: TableUiState,
    navigator: ThreePaneScaffoldNavigator<Int>,
    mainPaneTitle: String,
    restoreFocusIndex: Int?,
    windowAdaptiveInfo: WindowAdaptiveInfo,
    onIntent: (TableIntent) -> Unit,
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val paneStateHolder = rememberSaveableStateHolder()
    val editingIndex = state.editingIndex
    val editorDraft =
        rememberSaveable(editingIndex, saver = TextFieldState.Saver) {
            val currentText = editingIndex?.let { state.cells.getOrNull(it)?.text }.orEmpty()
            TextFieldState((state.editorDraft ?: currentText).take(MAX_CELL_TEXT_LENGTH))
        }
    LaunchedEffect(editingIndex, editorDraft) {
        if (editingIndex != null) {
            snapshotFlow { editorDraft.text.toString() }
                .collect { onIntent(TableIntent.EditorDraftChanged(it)) }
        }
    }
    val horizontalHingeBounds = windowAdaptiveInfo.horizontalSeparatingHingeBounds
    if (horizontalHingeBounds.isNotEmpty()) {
        TableHorizontalHingePaneLayout(
            hingeBounds = horizontalHingeBounds,
            excludedBounds = navigator.scaffoldDirective.excludedBounds,
            state = state,
            mainPaneTitle = mainPaneTitle,
            restoreFocusIndex = restoreFocusIndex,
            paneStateHolder = paneStateHolder,
            editorDraft = editorDraft,
            onIntent = onIntent,
            onNavigateUp = onNavigateUp,
            modifier = modifier,
        )
        return
    }

    TableMaterialPaneLayout(
        state = state,
        navigator = navigator,
        mainPaneTitle = mainPaneTitle,
        restoreFocusIndex = restoreFocusIndex,
        useEqualHeightVerticalPanes =
            windowAdaptiveInfo.windowPosture.isTabletop ||
                (
                    state.editingIndex != null &&
                        navigator.scaffoldDirective.maxVerticalPartitions > 1
                ),
        paneStateHolder = paneStateHolder,
        editorDraft = editorDraft,
        onIntent = onIntent,
        onNavigateUp = onNavigateUp,
        modifier = modifier,
    )
}

@Composable
private fun TableHorizontalHingePaneLayout(
    hingeBounds: List<Rect>,
    excludedBounds: List<Rect>,
    state: TableUiState,
    mainPaneTitle: String,
    restoreFocusIndex: Int?,
    paneStateHolder: SaveableStateHolder,
    editorDraft: TextFieldState,
    onIntent: (TableIntent) -> Unit,
    onNavigateUp: () -> Unit,
    modifier: Modifier,
) {
    val editorTitle = stringResource(R.string.editor_title)
    HorizontalHingePaneScaffold(
        hingeBounds = hingeBounds,
        excludedBounds = excludedBounds,
        modifier = modifier,
        mainPaneModifier = Modifier.tablePaneSemantics(TABLE_MAIN_PANE_TAG, mainPaneTitle),
        supportingPaneModifier =
            Modifier.tablePaneSemantics(TABLE_EDITOR_PANE_TAG, editorTitle),
        mainPane = {
            TableMainPaneWithSavedState(
                stateHolder = paneStateHolder,
                state = state,
                restoreFocusIndex = restoreFocusIndex,
                onIntent = onIntent,
                onNavigateUp = onNavigateUp,
            )
        },
        supportingPane = {
            TableEditorPaneWithSavedState(
                stateHolder = paneStateHolder,
                state = state,
                editorDraft = editorDraft,
                onIntent = onIntent,
            )
        },
    )
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
private fun TableMaterialPaneLayout(
    state: TableUiState,
    navigator: ThreePaneScaffoldNavigator<Int>,
    mainPaneTitle: String,
    restoreFocusIndex: Int?,
    useEqualHeightVerticalPanes: Boolean,
    paneStateHolder: SaveableStateHolder,
    editorDraft: TextFieldState,
    onIntent: (TableIntent) -> Unit,
    onNavigateUp: () -> Unit,
    modifier: Modifier,
) {
    val editorTitle = stringResource(R.string.editor_title)
    val paneExpansionState =
        rememberPaneExpansionState(
            keyProvider = navigator.scaffoldValue,
            anchors = StandardPaneExpansionAnchors,
            initialAnchoredIndex = BALANCED_PANE_ANCHOR_INDEX,
        )
    val supportsHorizontalPaneExpansion = navigator.supportsHorizontalPaneExpansion
    SupportingPaneScaffold(
        modifier = modifier,
        directive = navigator.scaffoldDirective,
        value = navigator.scaffoldValue,
        mainPane = {
            AnimatedPane(
                modifier = Modifier.tablePaneSemantics(TABLE_MAIN_PANE_TAG, mainPaneTitle),
            ) {
                TableMainPaneWithSavedState(
                    stateHolder = paneStateHolder,
                    state = state,
                    restoreFocusIndex = restoreFocusIndex,
                    onIntent = onIntent,
                    onNavigateUp = onNavigateUp,
                )
            }
        },
        supportingPane = {
            AnimatedPane(
                modifier =
                    (
                        if (useEqualHeightVerticalPanes) {
                            Modifier.preferredHeight(EQUAL_VERTICAL_PANE_HEIGHT_FRACTION)
                        } else {
                            Modifier
                        }
                    ).tablePaneSemantics(TABLE_EDITOR_PANE_TAG, editorTitle),
            ) {
                TableEditorPaneWithSavedState(
                    stateHolder = paneStateHolder,
                    state = state,
                    editorDraft = editorDraft,
                    onIntent = onIntent,
                )
            }
        },
        paneExpansionState =
            if (supportsHorizontalPaneExpansion) {
                paneExpansionState
            } else {
                null
            },
        paneExpansionDragHandle =
            if (supportsHorizontalPaneExpansion) {
                { state -> AppPaneDragHandle(state, Modifier.testTag(TABLE_PANE_DRAG_HANDLE_TAG)) }
            } else {
                null
            },
    )
}

@Composable
private fun TableMainPaneWithSavedState(
    stateHolder: SaveableStateHolder,
    state: TableUiState,
    restoreFocusIndex: Int?,
    onIntent: (TableIntent) -> Unit,
    onNavigateUp: () -> Unit,
) {
    stateHolder.SaveableStateProvider(MAIN_PANE_STATE_KEY) {
        TableMainScaffold(
            state = state,
            restoreFocusIndex = restoreFocusIndex,
            onIntent = onIntent,
            onNavigateUp = onNavigateUp,
        )
    }
}

@Composable
private fun TableEditorPaneWithSavedState(
    stateHolder: SaveableStateHolder,
    state: TableUiState,
    editorDraft: TextFieldState,
    onIntent: (TableIntent) -> Unit,
) {
    stateHolder.SaveableStateProvider(EDITOR_PANE_STATE_KEY) {
        val index = state.editingIndex
        EditorScaffold(
            index = index,
            currentText = index?.let { state.cells.getOrNull(it)?.text },
            draftState = editorDraft,
            onConfirm = { text -> onIntent(TableIntent.EditConfirmed(text)) },
            onDismiss = { onIntent(TableIntent.EditDismissed) },
        )
    }
}

private fun Modifier.tablePaneSemantics(
    tag: String,
    title: String,
): Modifier =
    recalculateWindowInsets()
        .testTag(tag)
        .semantics {
            paneTitle = title
            isTraversalGroup = true
        }
