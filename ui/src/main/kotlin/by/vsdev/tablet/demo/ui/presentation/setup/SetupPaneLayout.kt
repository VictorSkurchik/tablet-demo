package by.vsdev.tablet.demo.ui.presentation.setup

import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.recalculateWindowInsets
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.PaneAdaptedValue
import androidx.compose.material3.adaptive.layout.PaneExpansionStateKey
import androidx.compose.material3.adaptive.layout.SupportingPaneScaffold
import androidx.compose.material3.adaptive.layout.SupportingPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.rememberPaneExpansionState
import androidx.compose.material3.adaptive.navigation.ThreePaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import by.vsdev.tablet.demo.ui.R
import by.vsdev.tablet.demo.ui.components.layout.AppPaneDragHandle
import by.vsdev.tablet.demo.ui.components.layout.BALANCED_PANE_ANCHOR_INDEX
import by.vsdev.tablet.demo.ui.components.layout.BALANCED_PANE_PROPORTION
import by.vsdev.tablet.demo.ui.components.layout.EQUAL_VERTICAL_PANE_HEIGHT_FRACTION
import by.vsdev.tablet.demo.ui.components.layout.HorizontalHingePaneScaffold
import by.vsdev.tablet.demo.ui.components.layout.ResponsiveFormContainer
import by.vsdev.tablet.demo.ui.components.layout.StandardPaneExpansionAnchors
import by.vsdev.tablet.demo.ui.components.layout.horizontalSeparatingHingeBounds
import by.vsdev.tablet.demo.ui.components.layout.supportsHorizontalPaneExpansion

private data class SetupFormContent(
    val state: SetupUiState,
    val rowsInput: TextFieldState,
    val columnsInput: TextFieldState,
    val onBuild: () -> Unit,
    val useCompactImeLayout: Boolean,
    val showHeader: Boolean,
    val rowsModifier: Modifier,
    val columnsModifier: Modifier,
    val onFocusRestorationDisabled: () -> Unit,
)

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
internal fun SetupPaneLayout(
    state: SetupUiState,
    rowsInput: TextFieldState,
    columnsInput: TextFieldState,
    onBuild: () -> Unit,
    useCompactImeLayout: Boolean,
    windowAdaptiveInfo: WindowAdaptiveInfo,
    navigator: ThreePaneScaffoldNavigator<Any>,
    rowsModifier: Modifier,
    columnsModifier: Modifier,
    onFocusRestorationDisabled: () -> Unit,
) {
    val supportingPaneVisible =
        navigator.scaffoldValue[SupportingPaneScaffoldRole.Supporting] != PaneAdaptedValue.Hidden
    val horizontalHingeBounds = windowAdaptiveInfo.horizontalSeparatingHingeBounds
    val hasSeparateIntroPane = supportingPaneVisible || horizontalHingeBounds.isNotEmpty()
    val movableFormContent = rememberMovableSetupFormContent()
    val formContent =
        SetupFormContent(
            state = state,
            rowsInput = rowsInput,
            columnsInput = columnsInput,
            onBuild = onBuild,
            useCompactImeLayout = useCompactImeLayout,
            showHeader = !hasSeparateIntroPane,
            rowsModifier = rowsModifier,
            columnsModifier = columnsModifier,
            onFocusRestorationDisabled = onFocusRestorationDisabled,
        )
    val formPaneContent: @Composable () -> Unit = {
        movableFormContent(formContent)
    }
    val introPaneContent: @Composable () -> Unit = { SetupSupportingPaneScaffold() }
    val mainPaneContent =
        if (hasSeparateIntroPane) {
            introPaneContent
        } else {
            formPaneContent
        }
    val supportingPaneContent =
        if (hasSeparateIntroPane) {
            formPaneContent
        } else {
            introPaneContent
        }

    if (horizontalHingeBounds.isNotEmpty()) {
        SetupHorizontalHingePaneLayout(
            hingeBounds = horizontalHingeBounds,
            excludedBounds = navigator.scaffoldDirective.excludedBounds,
            mainPaneContent = mainPaneContent,
            supportingPaneContent = supportingPaneContent,
        )
        return
    }

    SetupMaterialPaneLayout(
        navigator = navigator,
        useEqualHeightVerticalPanes = navigator.scaffoldDirective.maxVerticalPartitions > 1,
        mainPaneContent = mainPaneContent,
        supportingPaneContent = supportingPaneContent,
        onFocusRestorationDisabled = onFocusRestorationDisabled,
    )
}

@Composable
private fun rememberMovableSetupFormContent(): @Composable (SetupFormContent) -> Unit =
    remember {
        movableContentOf<SetupFormContent> { content ->
            SetupFormScaffold(
                state = content.state,
                rowsInput = content.rowsInput,
                columnsInput = content.columnsInput,
                onBuild = content.onBuild,
                useCompactImeLayout = content.useCompactImeLayout,
                showHeader = content.showHeader,
                rowsModifier = content.rowsModifier,
                columnsModifier = content.columnsModifier,
                onFocusRestorationDisabled = content.onFocusRestorationDisabled,
            )
        }
    }

@Composable
private fun SetupHorizontalHingePaneLayout(
    hingeBounds: List<Rect>,
    excludedBounds: List<Rect>,
    mainPaneContent: @Composable () -> Unit,
    supportingPaneContent: @Composable () -> Unit,
) {
    HorizontalHingePaneScaffold(
        hingeBounds = hingeBounds,
        excludedBounds = excludedBounds,
        mainPane = mainPaneContent,
        supportingPane = supportingPaneContent,
        mainPaneModifier = Modifier.recalculateWindowInsets(),
        supportingPaneModifier = Modifier.recalculateWindowInsets(),
    )
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
private fun SetupMaterialPaneLayout(
    navigator: ThreePaneScaffoldNavigator<Any>,
    useEqualHeightVerticalPanes: Boolean,
    mainPaneContent: @Composable () -> Unit,
    supportingPaneContent: @Composable () -> Unit,
    onFocusRestorationDisabled: () -> Unit,
) {
    val paneExpansionState =
        rememberPaneExpansionState(
            key = PaneExpansionStateKey.Default,
            anchors = StandardPaneExpansionAnchors,
            initialAnchoredIndex = BALANCED_PANE_ANCHOR_INDEX,
        )
    SideEffect {
        if (paneExpansionState.isUnspecified()) {
            paneExpansionState.setFirstPaneProportion(BALANCED_PANE_PROPORTION)
        }
    }
    val supportsHorizontalPaneExpansion = navigator.supportsHorizontalPaneExpansion
    SupportingPaneScaffold(
        directive = navigator.scaffoldDirective,
        value = navigator.scaffoldValue,
        mainPane = {
            AnimatedPane(modifier = Modifier.recalculateWindowInsets()) {
                mainPaneContent()
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
                    ).recalculateWindowInsets(),
            ) {
                supportingPaneContent()
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
                { state ->
                    AppPaneDragHandle(
                        state,
                        Modifier
                            .testTag(SETUP_PANE_DRAG_HANDLE_TAG)
                            .onFocusChanged {
                                if (it.isFocused) onFocusRestorationDisabled()
                            },
                    )
                }
            } else {
                null
            },
    )
}

@Composable
private fun SetupFormScaffold(
    state: SetupUiState,
    rowsInput: TextFieldState,
    columnsInput: TextFieldState,
    onBuild: () -> Unit,
    useCompactImeLayout: Boolean,
    showHeader: Boolean,
    rowsModifier: Modifier,
    columnsModifier: Modifier,
    onFocusRestorationDisabled: () -> Unit,
) {
    Scaffold(modifier = Modifier.testTag(SETUP_FORM_PANE_TAG)) { innerPadding ->
        SetupForm(
            state = state,
            rowsInput = rowsInput,
            columnsInput = columnsInput,
            onBuild = onBuild,
            useCompactImeLayout = useCompactImeLayout,
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding),
            showHeader = showHeader,
            rowsModifier = rowsModifier,
            columnsModifier = columnsModifier,
            onFocusRestorationDisabled = onFocusRestorationDisabled,
        )
    }
}

@Composable
private fun SetupSupportingPaneScaffold() {
    Scaffold(modifier = Modifier.testTag(SETUP_SUPPORTING_PANE_TAG)) { innerPadding ->
        SetupSupportingPane(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding),
        )
    }
}

@Composable
private fun SetupSupportingPane(modifier: Modifier = Modifier) {
    ResponsiveFormContainer(
        modifier = modifier,
        maxWidth = 360.dp,
        applyImePadding = false,
    ) {
        Text(
            text = stringResource(R.string.setup_title),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = stringResource(R.string.setup_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
