package by.vsdev.tablet.demo.ui.presentation.table

import androidx.compose.runtime.Immutable
import by.vsdev.tablet.demo.domain.model.TableConfig
import by.vsdev.tablet.demo.recovery.model.MAX_RECOVERED_CELL_VALUE_LENGTH
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf

internal const val MAX_CELL_TEXT_LENGTH = MAX_RECOVERED_CELL_VALUE_LENGTH

@Immutable
internal data class CellUiState(
    val text: String,
    val isSelected: Boolean = false,
)

@Immutable
internal sealed interface TableLoadState {
    data object Loading : TableLoadState

    @Immutable
    class Content(
        val cells: PersistentList<CellUiState>,
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
    val cells: PersistentList<CellUiState>
        get() = (loadState as? TableLoadState.Content)?.cells ?: persistentListOf()
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

    data class EditorDraftChanged(
        val text: String,
    ) : TableIntent

    data object AppBackgrounded : TableIntent

    data object EditDismissed : TableIntent

    data object RetryLoad : TableIntent
}
