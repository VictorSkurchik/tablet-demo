package by.vsdev.tablet.demo.ui.presentation.table

import androidx.compose.runtime.Immutable
import by.vsdev.tablet.demo.domain.model.TableConfig
import by.vsdev.tablet.demo.recovery.model.MAX_RECOVERED_CELL_VALUE_LENGTH

internal const val MAX_CELL_TEXT_LENGTH = MAX_RECOVERED_CELL_VALUE_LENGTH

@Immutable
internal data class CellUiState(
    val text: String,
    val isSelected: Boolean = false,
)

@Immutable
internal sealed interface TableLoadState {
    data object Loading : TableLoadState

    data class Content(
        val cells: List<CellUiState>,
    ) : TableLoadState

    data object Error : TableLoadState
}

@Immutable
internal data class TableUiState(
    val config: TableConfig,
    val loadState: TableLoadState = TableLoadState.Loading,
    val editingIndex: Int? = null,
    val editorDraft: String? = null,
) {
    val cells: List<CellUiState>
        get() = (loadState as? TableLoadState.Content)?.cells.orEmpty()

    val isLoading: Boolean
        get() = loadState == TableLoadState.Loading

    val hasLoadError: Boolean
        get() = loadState == TableLoadState.Error
}

internal sealed interface TableIntent {
    data class CellClicked(
        val index: Int,
    ) : TableIntent

    data class CellDoubleClicked(
        val index: Int,
    ) : TableIntent

    data class EditConfirmed(
        val index: Int,
        val text: String,
    ) : TableIntent

    data class EditorDraftChanged(
        val text: String,
    ) : TableIntent

    data object AppBackgrounded : TableIntent

    data object EditDismissed : TableIntent

    data object RetryLoad : TableIntent
}
