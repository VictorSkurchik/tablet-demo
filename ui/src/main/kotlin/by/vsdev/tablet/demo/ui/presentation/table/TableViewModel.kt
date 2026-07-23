package by.vsdev.tablet.demo.ui.presentation.table

import androidx.lifecycle.viewModelScope
import by.vsdev.tablet.demo.domain.model.TableConfig
import by.vsdev.tablet.demo.domain.model.TableDataResult
import by.vsdev.tablet.demo.domain.usecase.GenerateTableDataUseCase
import by.vsdev.tablet.demo.domain.util.BackgroundDispatcher
import by.vsdev.tablet.demo.ui.mvi.MviViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class TableViewModel(
    private val config: TableConfig,
    private val generateTableData: GenerateTableDataUseCase,
    private val backgroundDispatcher: BackgroundDispatcher,
) : MviViewModel<TableUiState, TableIntent>(TableUiState(config = config)) {
    private var loadJob: Job? = null

    init {
        load()
    }

    private fun load() {
        loadJob?.cancel()
        setState { copy(loadState = TableLoadState.Loading, editingIndex = null) }
        loadJob =
            viewModelScope.launch {
                try {
                    when (val result = generateTableData(config)) {
                        is TableDataResult.Success -> {
                            val cells =
                                withContext(backgroundDispatcher.value) {
                                    result.data.cells.map { CellUiState(text = it) }
                                }
                            setState { copy(loadState = TableLoadState.Content(cells)) }
                        }

                        TableDataResult.GenerationUnavailable -> showLoadError()
                    }
                } catch (error: CancellationException) {
                    throw error
                } catch (_: Exception) {
                    showLoadError()
                }
            }
    }

    private fun showLoadError() {
        setState {
            copy(
                loadState = TableLoadState.Error,
                editingIndex = null,
            )
        }
    }

    override fun onIntent(intent: TableIntent) {
        when (intent) {
            is TableIntent.CellClicked -> toggleColor(intent.index)
            is TableIntent.CellDoubleClicked -> openEditor(intent.index)
            is TableIntent.EditConfirmed -> confirmEdit(intent.index, intent.text)
            TableIntent.EditDismissed -> dismissEditor()
            TableIntent.RetryLoad -> load()
        }
    }

    private fun toggleColor(index: Int) {
        updateCell(index) { copy(isSelected = !isSelected) }
    }

    private fun openEditor(index: Int) {
        if (index !in state.value.cells.indices) return
        setState { copy(editingIndex = index) }
    }

    private fun confirmEdit(
        index: Int,
        text: String,
    ) {
        updateCell(index) { copy(text = text.take(MAX_CELL_TEXT_LENGTH)) }
        if (index in state.value.cells.indices) {
            dismissEditor()
        }
    }

    private fun dismissEditor() {
        setState { copy(editingIndex = null) }
    }

    private fun updateCell(
        index: Int,
        transform: CellUiState.() -> CellUiState,
    ) {
        setState {
            val content = loadState as? TableLoadState.Content ?: return@setState this
            val cell = content.cells.getOrNull(index) ?: return@setState this
            val updatedCells = content.cells.toMutableList()
            updatedCells[index] = cell.transform()
            copy(loadState = TableLoadState.Content(updatedCells))
        }
    }
}
