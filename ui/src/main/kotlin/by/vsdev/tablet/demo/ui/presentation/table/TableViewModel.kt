package by.vsdev.tablet.demo.ui.presentation.table

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import by.vsdev.tablet.demo.domain.model.TableConfig
import by.vsdev.tablet.demo.domain.usecase.GenerateTableDataUseCase
import by.vsdev.tablet.demo.domain.util.BackgroundDispatcher
import by.vsdev.tablet.demo.ui.mvi.MviViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class TableViewModel(
    private val config: TableConfig,
    private val generateTableData: GenerateTableDataUseCase,
    private val backgroundDispatcher: BackgroundDispatcher,
) : MviViewModel<TableUiState, TableIntent>(TableUiState(config = config)) {
    var cells: List<CellState> by mutableStateOf(emptyList())
        private set

    init {
        viewModelScope.launch {
            val data = generateTableData(config)
            cells =
                withContext(backgroundDispatcher.value) {
                    data.cells.map { CellState(text = it) }
                }
            setState { copy(isLoading = false) }
        }
    }

    override fun onIntent(intent: TableIntent) {
        when (intent) {
            is TableIntent.CellClicked -> toggleColor(intent.index)
            is TableIntent.CellDoubleClicked -> openEditor(intent.index)
            is TableIntent.EditConfirmed -> confirmEdit(intent.index, intent.text)
            TableIntent.EditDismissed -> dismissEditor()
        }
    }

    private fun toggleColor(index: Int) {
        val cell = cells.getOrNull(index) ?: return
        cell.toggleSelection()
    }

    private fun openEditor(index: Int) {
        if (index !in cells.indices) return
        setState { copy(editingIndex = index) }
    }

    private fun confirmEdit(
        index: Int,
        text: String,
    ) {
        val cell = cells.getOrNull(index) ?: return
        cell.updateText(text)
        dismissEditor()
    }

    private fun dismissEditor() {
        setState { copy(editingIndex = null) }
    }
}
