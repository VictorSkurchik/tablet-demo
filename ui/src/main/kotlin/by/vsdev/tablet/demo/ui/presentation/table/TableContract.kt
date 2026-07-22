package by.vsdev.tablet.demo.ui.presentation.table

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import by.vsdev.tablet.demo.domain.model.TableConfig

internal const val MAX_CELL_TEXT_LENGTH = 100

/** Snapshot-backed state that can update without replacing its entry in the cell list. */
@Stable
internal class CellState(
    text: String,
    isSelected: Boolean = false,
) {
    var text: String by mutableStateOf(text)
        private set
    var isSelected: Boolean by mutableStateOf(isSelected)
        private set

    fun toggleSelection() {
        isSelected = !isSelected
    }

    fun updateText(value: String) {
        text = value.take(MAX_CELL_TEXT_LENGTH)
    }
}

internal data class TableUiState(
    val config: TableConfig,
    val isLoading: Boolean = true,
    val editingIndex: Int? = null,
)

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

    data object EditDismissed : TableIntent
}
