package by.vsdev.tablet.demo.ui.presentation.table

import androidx.lifecycle.viewModelScope
import by.vsdev.tablet.demo.domain.model.TableConfig
import by.vsdev.tablet.demo.domain.usecase.GenerateTableDataUseCase
import by.vsdev.tablet.demo.domain.util.BackgroundDispatcher
import by.vsdev.tablet.demo.ui.mvi.StateViewModel
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class TableViewModel(
    private val config: TableConfig,
    private val generateTableData: GenerateTableDataUseCase,
    private val backgroundDispatcher: BackgroundDispatcher,
) : StateViewModel<TableUiState>(TableUiState(config = config)) {
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
                    val data = generateTableData(config)
                    val cells =
                        withContext(backgroundDispatcher.value) {
                            val builder = persistentListOf<CellUiState>().builder()
                            data.cells.forEach { text ->
                                builder.add(CellUiState(text = text))
                            }
                            builder.build()
                        }
                    setState { copy(loadState = TableLoadState.Content(cells)) }
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

    fun onIntent(intent: TableIntent) {
        when (intent) {
            is TableIntent.CellClicked -> toggleColor(intent.index)
            is TableIntent.CellDoubleClicked -> openEditor(intent.index)
            is TableIntent.EditConfirmed -> confirmEdit(intent.text)
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

    private fun confirmEdit(text: String) {
        setState {
            val index = editingIndex ?: return@setState this
            val content = loadState as? TableLoadState.Content
            val cell = content?.cells?.getOrNull(index)
            val updatedLoadState =
                if (content != null && cell != null) {
                    val updatedCell = cell.copy(text = text.take(MAX_CELL_TEXT_LENGTH))
                    if (updatedCell == cell) {
                        content
                    } else {
                        TableLoadState.Content(content.cells.set(index, updatedCell))
                    }
                } else {
                    loadState
                }
            copy(
                loadState = updatedLoadState,
                editingIndex = null,
            )
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
            val updatedCell = cell.transform()
            if (updatedCell == cell) return@setState this
            val updatedCells = content.cells.set(index, updatedCell)
            copy(loadState = TableLoadState.Content(updatedCells))
        }
    }
}
