package by.vsdev.tablet.demo.ui.presentation.table

import androidx.lifecycle.viewModelScope
import by.vsdev.tablet.demo.domain.model.TableConfig
import by.vsdev.tablet.demo.domain.model.TableDataResult
import by.vsdev.tablet.demo.domain.usecase.GenerateTableDataUseCase
import by.vsdev.tablet.demo.domain.util.BackgroundDispatcher
import by.vsdev.tablet.demo.recovery.TableRecoveryRepository
import by.vsdev.tablet.demo.recovery.model.RecoveredCell
import by.vsdev.tablet.demo.recovery.model.TableRecoverySnapshot
import by.vsdev.tablet.demo.ui.mvi.MviViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class TableViewModel(
    private val config: TableConfig,
    private val sessionId: String,
    private val generateTableData: GenerateTableDataUseCase,
    private val recoveryRepository: TableRecoveryRepository,
    private val backgroundDispatcher: BackgroundDispatcher,
) : MviViewModel<TableUiState, TableIntent>(TableUiState(config = config)) {
    private var loadJob: Job? = null
    private var recoveryCells: MutableList<RecoveredCell> = mutableListOf()
    private val recoveryCoordinator =
        TableRecoveryCoordinator(
            sessionId = sessionId,
            repository = recoveryRepository,
            scope = viewModelScope,
            snapshotProvider = ::currentSnapshot,
        )

    init {
        load()
    }

    override fun onIntent(intent: TableIntent) {
        when (intent) {
            is TableIntent.CellClicked ->
                updateCell(intent.index) {
                    copy(isSelected = !isSelected)
                }
            is TableIntent.CellDoubleClicked -> openEditor(intent.index)
            is TableIntent.EditConfirmed -> confirmEdit(intent.text)
            is TableIntent.EditorDraftChanged -> {
                if (state.value.editingIndex != null) {
                    setState { copy(editorDraft = intent.text.take(MAX_CELL_TEXT_LENGTH)) }
                    recoveryCoordinator.markDirty()
                }
            }
            TableIntent.AppBackgrounded -> recoveryCoordinator.flush()
            TableIntent.EditDismissed -> dismissEditor()
            TableIntent.RetryLoad -> load()
        }
    }

    fun closeSession(onClosed: () -> Unit) = recoveryCoordinator.close(onClosed)

    private fun load() {
        loadJob?.cancel()
        setState {
            copy(
                loadState = TableLoadState.Loading,
                editingIndex = null,
                editorDraft = null,
            )
        }
        loadJob =
            viewModelScope.launch {
                try {
                    val restored = recoveryRepository.load(sessionId)
                    if (restored?.config == config) {
                        restore(restored)
                    } else {
                        if (restored != null) recoveryRepository.delete(sessionId)
                        generate()
                    }
                } catch (error: CancellationException) {
                    throw error
                } catch (_: Exception) {
                    showLoadError()
                }
            }
    }

    private suspend fun generate() {
        when (val result = generateTableData(config)) {
            is TableDataResult.Success -> {
                val recovered =
                    withContext(backgroundDispatcher.value) {
                        result.data.cells.map { RecoveredCell(text = it, isSelected = false) }
                    }
                recoveryCells = recovered.toMutableList()
                setState {
                    copy(
                        loadState =
                            TableLoadState.Content(
                                recovered.map { CellUiState(text = it.text) },
                            ),
                    )
                }
                recoveryCoordinator.markDirty()
            }

            TableDataResult.GenerationUnavailable -> showLoadError()
        }
    }

    private fun restore(snapshot: TableRecoverySnapshot) {
        recoveryCells = snapshot.cells.toMutableList()
        setState {
            copy(
                loadState =
                    TableLoadState.Content(
                        snapshot.cells.map {
                            CellUiState(text = it.text, isSelected = it.isSelected)
                        },
                    ),
                editingIndex = snapshot.editingIndex,
                editorDraft = snapshot.editorDraft,
            )
        }
        recoveryCoordinator.markDirty()
    }

    private fun showLoadError() {
        recoveryCells.clear()
        setState {
            copy(
                loadState = TableLoadState.Error,
                editingIndex = null,
                editorDraft = null,
            )
        }
    }

    private fun openEditor(index: Int) {
        val cell = state.value.cells.getOrNull(index) ?: return
        setState { copy(editingIndex = index, editorDraft = cell.text) }
        recoveryCoordinator.markDirty()
    }

    private fun confirmEdit(text: String) {
        val index = state.value.editingIndex ?: return
        if (updateCell(index) { copy(text = text.take(MAX_CELL_TEXT_LENGTH)) }) {
            dismissEditor()
        }
    }

    private fun dismissEditor() {
        if (state.value.editingIndex == null) return
        setState { copy(editingIndex = null, editorDraft = null) }
        recoveryCoordinator.markDirty()
    }

    private fun updateCell(
        index: Int,
        transform: CellUiState.() -> CellUiState,
    ): Boolean {
        val content = state.value.loadState as? TableLoadState.Content
        val cell = content?.cells?.getOrNull(index)
        val wasUpdated = content != null && cell != null
        if (content != null && cell != null) {
            val updatedCell = cell.transform()
            val updatedCells = content.cells.toMutableList()
            updatedCells[index] = updatedCell
            recoveryCells[index] =
                RecoveredCell(
                    text = updatedCell.text,
                    isSelected = updatedCell.isSelected,
                )
            setState { copy(loadState = TableLoadState.Content(updatedCells)) }
            recoveryCoordinator.markDirty()
        }
        return wasUpdated
    }

    private fun currentSnapshot(): TableRecoverySnapshot =
        TableRecoverySnapshot(
            config = config,
            cells = recoveryCells.toList(),
            editingIndex = state.value.editingIndex,
            editorDraft = state.value.editorDraft,
        )
}
