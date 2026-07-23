package by.vsdev.tablet.demo.ui.presentation.table

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import by.vsdev.tablet.demo.domain.model.TableConfig
import by.vsdev.tablet.demo.domain.model.TableDataResult
import by.vsdev.tablet.demo.domain.usecase.GenerateTableDataUseCase
import by.vsdev.tablet.demo.domain.util.BackgroundDispatcher
import by.vsdev.tablet.demo.ui.mvi.MviViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class TableViewModel(
    private val config: TableConfig,
    private val generateTableData: GenerateTableDataUseCase,
    private val backgroundDispatcher: BackgroundDispatcher,
) : MviViewModel<TableUiState, TableIntent>(TableUiState(config = config)) {
    var cells: List<CellState> by mutableStateOf(emptyList())
        private set
    private var loadJob: Job? = null

    init {
        loadTable()
    }

    private fun loadTable() {
        if (loadJob?.isActive == true) return
        loadJob =
            viewModelScope.launch {
                setState { copy(loadState = TableLoadState.Loading) }
                when (val result = generateTableData(config)) {
                    is TableDataResult.Success -> {
                        cells =
                            withContext(backgroundDispatcher.value) {
                                result.data.cells.map { CellState(text = it) }
                            }
                        setState { copy(loadState = TableLoadState.Content) }
                    }

                    TableDataResult.GenerationUnavailable -> {
                        cells = emptyList()
                        setState {
                            copy(
                                loadState = TableLoadState.Error,
                                editingIndex = null,
                            )
                        }
                    }
                }
            }
    }

    override fun onIntent(intent: TableIntent) {
        when (intent) {
            is TableIntent.CellClicked -> toggleColor(intent.index)
            is TableIntent.CellDoubleClicked -> openEditor(intent.index)
            is TableIntent.EditConfirmed -> confirmEdit(intent.index, intent.text)
            TableIntent.EditDismissed -> dismissEditor()
            TableIntent.RetryLoad -> loadTable()
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
