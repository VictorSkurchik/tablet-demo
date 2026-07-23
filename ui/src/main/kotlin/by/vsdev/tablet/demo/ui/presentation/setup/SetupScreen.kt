package by.vsdev.tablet.demo.ui.presentation.setup

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import by.vsdev.tablet.demo.domain.model.TableConfig
import by.vsdev.tablet.demo.domain.model.TableLimits
import by.vsdev.tablet.demo.domain.usecase.FieldError
import by.vsdev.tablet.demo.ui.R
import by.vsdev.tablet.demo.ui.components.layout.ResponsiveFormContainer
import by.vsdev.tablet.demo.ui.components.molecules.NumberField
import by.vsdev.tablet.demo.ui.mvi.CollectEffect
import by.vsdev.tablet.demo.ui.theme.AppSpacing
import by.vsdev.tablet.demo.ui.theme.AppTheme
import org.koin.androidx.compose.koinViewModel

private val ComfortableSetupHeight = 320.dp

private enum class SetupField {
    Rows,
    Columns,
}

@Composable
fun SetupRoute(
    onNavigateToTable: (TableConfig) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = koinViewModel<SetupViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()
    CollectEffect(viewModel.navigation, onNavigateToTable)
    SetupScreen(
        state = state,
        rowsInput = viewModel.rowsInput,
        columnsInput = viewModel.columnsInput,
        onIntent = viewModel::onIntent,
        modifier = modifier,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SetupScreen(
    state: SetupUiState,
    rowsInput: TextFieldState,
    columnsInput: TextFieldState,
    onIntent: (SetupIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val imeBottom = WindowInsets.ime.getBottom(density)
    val isOnScreenKeyboardVisible = WindowInsets.isImeVisible
    val rowsRequester = remember { BringIntoViewRequester() }
    val columnsRequester = remember { BringIntoViewRequester() }
    var focusedField by remember { mutableStateOf<SetupField?>(null) }

    LaunchedEffect(isOnScreenKeyboardVisible, focusedField) {
        if (isOnScreenKeyboardVisible) {
            val requester =
                when (focusedField) {
                    SetupField.Rows -> rowsRequester
                    SetupField.Columns -> columnsRequester
                    null -> return@LaunchedEffect
                }
            withFrameNanos { }
            requester.bringIntoView()
        }
    }

    val screenTitle = stringResource(R.string.setup_title)
    Scaffold(
        modifier =
            modifier.semantics {
                paneTitle = screenTitle
                isTraversalGroup = true
            },
    ) { innerPadding ->
        BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            val imeHeight = with(density) { imeBottom.toDp() }
            val useCompactImeLayout =
                isOnScreenKeyboardVisible &&
                    imeBottom > 0 &&
                    maxHeight - imeHeight < ComfortableSetupHeight

            SetupForm(
                state = state,
                rowsInput = rowsInput,
                columnsInput = columnsInput,
                onIntent = onIntent,
                useCompactImeLayout = useCompactImeLayout,
                rowsModifier =
                    Modifier
                        .bringIntoViewRequester(rowsRequester)
                        .trackSetupFieldFocus(SetupField.Rows, focusedField) { focusedField = it },
                columnsModifier =
                    Modifier
                        .bringIntoViewRequester(columnsRequester)
                        .trackSetupFieldFocus(SetupField.Columns, focusedField) { focusedField = it },
            )
        }
    }
}

private fun Modifier.trackSetupFieldFocus(
    field: SetupField,
    focusedField: SetupField?,
    onFocusedFieldChanged: (SetupField?) -> Unit,
): Modifier =
    onFocusChanged {
        when {
            it.isFocused -> onFocusedFieldChanged(field)
            focusedField == field -> onFocusedFieldChanged(null)
        }
    }

@Composable
internal fun SetupForm(
    state: SetupUiState,
    rowsInput: TextFieldState,
    columnsInput: TextFieldState,
    onIntent: (SetupIntent) -> Unit,
    useCompactImeLayout: Boolean,
    modifier: Modifier = Modifier,
    rowsModifier: Modifier = Modifier,
    columnsModifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current

    ResponsiveFormContainer(
        modifier = modifier,
        contentPadding = if (useCompactImeLayout) AppSpacing.small else AppSpacing.large,
        verticalSpacing = if (useCompactImeLayout) AppSpacing.small else AppSpacing.medium,
    ) {
        if (!useCompactImeLayout) {
            Text(
                text = stringResource(R.string.setup_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = stringResource(R.string.setup_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        NumberField(
            state = rowsInput,
            label = stringResource(R.string.setup_rows_label),
            supportingText = state.rowsError.supportingText(TableLimits.rowRange),
            isError = state.rowsError != null,
            maxLength = TableLimits.MAX_ROWS.toString().length,
            imeAction = ImeAction.Next,
            showSupportingText = !useCompactImeLayout || state.rowsError != null,
            onImeAction = { focusManager.moveFocus(FocusDirection.Down) },
            modifier = rowsModifier,
        )
        NumberField(
            state = columnsInput,
            label = stringResource(R.string.setup_columns_label),
            supportingText = state.columnsError.supportingText(TableLimits.columnRange),
            isError = state.columnsError != null,
            maxLength = TableLimits.MAX_COLUMNS.toString().length,
            imeAction = ImeAction.Done,
            showSupportingText = !useCompactImeLayout || state.columnsError != null,
            onImeAction = {
                if (state.canBuild) {
                    onIntent(SetupIntent.BuildClicked)
                } else {
                    focusManager.clearFocus()
                }
            },
            modifier = columnsModifier,
        )

        Button(
            onClick = { onIntent(SetupIntent.BuildClicked) },
            enabled = state.canBuild,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.setup_build))
        }
    }
}

@Composable
private fun FieldError?.supportingText(range: IntRange): String =
    when (this) {
        null -> stringResource(R.string.setup_allowed_range, range.first, range.last)
        FieldError.EMPTY -> stringResource(R.string.setup_error_required)
        FieldError.NOT_A_NUMBER -> stringResource(R.string.setup_error_not_a_number)
        FieldError.BELOW_MIN -> stringResource(R.string.setup_error_min, range.first)
        FieldError.ABOVE_MAX -> stringResource(R.string.setup_error_max, range.last)
    }

@Preview(name = "Setup – empty")
@Composable
private fun SetupScreenEmptyPreview() {
    AppTheme {
        SetupScreen(
            state = SetupUiState(),
            rowsInput = TextFieldState(),
            columnsInput = TextFieldState(),
            onIntent = {},
        )
    }
}

@Preview(name = "Setup – valid")
@Composable
private fun SetupScreenValidPreview() {
    AppTheme {
        SetupScreen(
            state = SetupUiState(canBuild = true),
            rowsInput = TextFieldState("100"),
            columnsInput = TextFieldState("4"),
            onIntent = {},
        )
    }
}

@Preview(name = "Setup – error")
@Composable
private fun SetupScreenErrorPreview() {
    AppTheme {
        SetupScreen(
            state =
                SetupUiState(
                    rowsError = FieldError.ABOVE_MAX,
                    columnsError = FieldError.ABOVE_MAX,
                ),
            rowsInput = TextFieldState("9999"),
            columnsInput = TextFieldState("9"),
            onIntent = {},
        )
    }
}
