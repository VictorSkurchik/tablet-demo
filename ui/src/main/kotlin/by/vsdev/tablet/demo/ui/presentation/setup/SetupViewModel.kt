package by.vsdev.tablet.demo.ui.presentation.setup

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import by.vsdev.tablet.demo.domain.model.TableConfig
import by.vsdev.tablet.demo.domain.usecase.TableConfigValidation
import by.vsdev.tablet.demo.domain.usecase.ValidateTableConfigUseCase
import by.vsdev.tablet.demo.ui.mvi.MviViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

private const val KEY_ROWS = "setup_rows"
private const val KEY_COLUMNS = "setup_columns"

internal class SetupViewModel(
    private val validateTableConfig: ValidateTableConfigUseCase,
    private val savedStateHandle: SavedStateHandle,
) : MviViewModel<SetupUiState, SetupIntent>(
        restoredSetupState(savedStateHandle, validateTableConfig),
    ) {
    val rowsInput = TextFieldState(savedStateHandle.get<String>(KEY_ROWS).orEmpty())
    val columnsInput = TextFieldState(savedStateHandle.get<String>(KEY_COLUMNS).orEmpty())

    private val navigationEvents = Channel<TableConfig>(Channel.CONFLATED)
    val navigation: Flow<TableConfig> = navigationEvents.receiveAsFlow()

    init {
        viewModelScope.launch {
            snapshotFlow { rowsInput.text.toString() to columnsInput.text.toString() }
                .collect { (rows, columns) -> revalidate(rows, columns) }
        }
    }

    override fun onIntent(intent: SetupIntent) {
        when (intent) {
            SetupIntent.BuildClicked -> build()
        }
    }

    private fun revalidate(
        rows: String,
        columns: String,
    ) {
        val validation = validateTableConfig(rows, columns)
        savedStateHandle[KEY_ROWS] = rows
        savedStateHandle[KEY_COLUMNS] = columns
        setState {
            copy(
                rowsError = validation.rowsError.takeIf { rows.isNotEmpty() },
                columnsError = validation.columnsError.takeIf { columns.isNotEmpty() },
                canBuild = validation.isValid,
            )
        }
    }

    private fun build() {
        val validation: TableConfigValidation =
            validateTableConfig(rowsInput.text.toString(), columnsInput.text.toString())
        val config = validation.config
        if (config != null) {
            navigationEvents.trySend(config)
        } else {
            setState {
                copy(
                    rowsError = validation.rowsError,
                    columnsError = validation.columnsError,
                    canBuild = false,
                )
            }
        }
    }
}

private fun restoredSetupState(
    handle: SavedStateHandle,
    validate: ValidateTableConfigUseCase,
): SetupUiState {
    val rows = handle.get<String>(KEY_ROWS).orEmpty()
    val columns = handle.get<String>(KEY_COLUMNS).orEmpty()
    val validation = validate(rows, columns)
    return SetupUiState(
        rowsError = validation.rowsError.takeIf { rows.isNotEmpty() },
        columnsError = validation.columnsError.takeIf { columns.isNotEmpty() },
        canBuild = validation.isValid,
    )
}
