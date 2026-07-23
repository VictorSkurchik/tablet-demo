package by.vsdev.tablet.demo.ui.presentation.table

import androidx.compose.runtime.Immutable
import by.vsdev.tablet.demo.domain.model.TableConfig

internal const val MAX_CELL_TEXT_LENGTH = 100

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
        val text: String,
    ) : TableIntent

    data object EditDismissed : TableIntent

    data object RetryLoad : TableIntent
}
