package by.vsdev.tablet.demo.ui.presentation.setup

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import by.vsdev.tablet.demo.domain.model.TableConfig
import by.vsdev.tablet.demo.domain.model.TableLimits
import by.vsdev.tablet.demo.domain.usecase.FieldError
import by.vsdev.tablet.demo.ui.R
import by.vsdev.tablet.demo.ui.components.layout.ResponsiveFormContainer
import by.vsdev.tablet.demo.ui.components.molecules.NumberField
import by.vsdev.tablet.demo.ui.mvi.CollectEffect
import by.vsdev.tablet.demo.ui.theme.AppTheme
import org.koin.androidx.compose.koinViewModel

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

@Composable
internal fun SetupScreen(
    state: SetupUiState,
    rowsInput: TextFieldState,
    columnsInput: TextFieldState,
    onIntent: (SetupIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    Scaffold(modifier = modifier) { innerPadding ->
        ResponsiveFormContainer(modifier = Modifier.padding(innerPadding)) {
            Text(
                text = stringResource(R.string.setup_title),
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = stringResource(R.string.setup_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            NumberField(
                state = rowsInput,
                label = stringResource(R.string.setup_rows_label),
                supportingText = state.rowsError.supportingText(TableLimits.rowRange),
                isError = state.rowsError != null,
                maxLength = TableLimits.MAX_ROWS.toString().length,
                imeAction = ImeAction.Next,
                onImeAction = { focusManager.moveFocus(FocusDirection.Down) },
            )
            NumberField(
                state = columnsInput,
                label = stringResource(R.string.setup_columns_label),
                supportingText = state.columnsError.supportingText(TableLimits.columnRange),
                isError = state.columnsError != null,
                maxLength = TableLimits.MAX_COLUMNS.toString().length,
                imeAction = ImeAction.Done,
                onImeAction = {
                    if (state.canBuild) {
                        onIntent(SetupIntent.BuildClicked)
                    } else {
                        focusManager.clearFocus()
                    }
                },
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
