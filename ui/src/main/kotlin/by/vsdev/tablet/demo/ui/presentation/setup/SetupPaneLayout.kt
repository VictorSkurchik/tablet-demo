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
import androidx.compose.material3.adaptive.layout.SupportingPaneScaffold
import androidx.compose.material3.adaptive.layout.SupportingPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.ThreePaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import by.vsdev.tablet.demo.ui.R
import by.vsdev.tablet.demo.ui.components.layout.EQUAL_VERTICAL_PANE_HEIGHT_FRACTION
import by.vsdev.tablet.demo.ui.components.layout.HorizontalHingePaneScaffold
import by.vsdev.tablet.demo.ui.components.layout.ResponsiveFormContainer
import by.vsdev.tablet.demo.ui.components.layout.horizontalSeparatingHingeBounds

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
internal fun SetupPaneLayout(
    state: SetupUiState,
    rowsInput: TextFieldState,
    columnsInput: TextFieldState,
    onIntent: (SetupIntent) -> Unit,
    useCompactImeLayout: Boolean,
    windowAdaptiveInfo: WindowAdaptiveInfo,
    navigator: ThreePaneScaffoldNavigator<Any>,
    rowsModifier: Modifier,
    columnsModifier: Modifier,
) {
    val supportingPaneVisible =
        navigator.scaffoldValue[SupportingPaneScaffoldRole.Supporting] != PaneAdaptedValue.Hidden
    val horizontalHingeBounds = windowAdaptiveInfo.horizontalSeparatingHingeBounds
    val hasSeparateIntroPane = supportingPaneVisible || horizontalHingeBounds.isNotEmpty()
    val formPaneContent: @Composable () -> Unit = {
        SetupFormScaffold(
            state = state,
            rowsInput = rowsInput,
            columnsInput = columnsInput,
            onIntent = onIntent,
            useCompactImeLayout = useCompactImeLayout,
            showHeader = !hasSeparateIntroPane,
            rowsModifier = rowsModifier,
            columnsModifier = columnsModifier,
        )
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
        useEqualHeightTabletopPanes = windowAdaptiveInfo.windowPosture.isTabletop,
        mainPaneContent = mainPaneContent,
        supportingPaneContent = supportingPaneContent,
    )
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
    useEqualHeightTabletopPanes: Boolean,
    mainPaneContent: @Composable () -> Unit,
    supportingPaneContent: @Composable () -> Unit,
) {
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
                        if (useEqualHeightTabletopPanes) {
                            Modifier.preferredHeight(EQUAL_VERTICAL_PANE_HEIGHT_FRACTION)
                        } else {
                            Modifier
                        }
                    ).recalculateWindowInsets(),
            ) {
                supportingPaneContent()
            }
        },
    )
}

@Composable
private fun SetupFormScaffold(
    state: SetupUiState,
    rowsInput: TextFieldState,
    columnsInput: TextFieldState,
    onIntent: (SetupIntent) -> Unit,
    useCompactImeLayout: Boolean,
    showHeader: Boolean,
    rowsModifier: Modifier,
    columnsModifier: Modifier,
) {
    Scaffold(modifier = Modifier.testTag(SETUP_FORM_PANE_TAG)) { innerPadding ->
        SetupForm(
            state = state,
            rowsInput = rowsInput,
            columnsInput = columnsInput,
            onIntent = onIntent,
            useCompactImeLayout = useCompactImeLayout,
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding),
            showHeader = showHeader,
            rowsModifier = rowsModifier,
            columnsModifier = columnsModifier,
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
