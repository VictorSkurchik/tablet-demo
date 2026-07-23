package by.vsdev.tablet.demo.ui.presentation.table

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import by.vsdev.tablet.demo.domain.model.TableConfig
import by.vsdev.tablet.demo.domain.usecase.GenerateTableDataUseCase
import by.vsdev.tablet.demo.domain.util.BackgroundDispatcher
import by.vsdev.tablet.demo.recovery.TableRecoveryRepository
import by.vsdev.tablet.demo.recovery.model.RecoveredCell
import by.vsdev.tablet.demo.recovery.model.TableRecoverySnapshot
import by.vsdev.tablet.demo.ui.mvi.MviViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class TableViewModel(
    private val config: TableConfig,
    private val sessionId: String,
    private val generateTableData: GenerateTableDataUseCase,
    private val recoveryRepository: TableRecoveryRepository,
    private val backgroundDispatcher: BackgroundDispatcher,
) : MviViewModel<TableUiState, TableIntent>(TableUiState(config = config)) {
    var cells: List<CellState> by mutableStateOf(emptyList())
        private set

    private var recoveryCells: MutableList<RecoveredCell> = mutableListOf()
    private val recoveryCoordinator =
        TableRecoveryCoordinator(
            sessionId = sessionId,
            repository = recoveryRepository,
            scope = viewModelScope,
            snapshotProvider = ::currentSnapshot,
        )

    init {
        viewModelScope.launch {
            val restored = recoveryRepository.load(sessionId)
            if (restored?.config == config) {
                restore(restored)
            } else {
                if (restored != null) recoveryRepository.delete(sessionId)
                generate()
            }
            setState { copy(isLoading = false) }
            recoveryCoordinator.markDirty()
        }
    }

    override fun onIntent(intent: TableIntent) {
        when (intent) {
            is TableIntent.CellClicked -> toggleColor(intent.index)
            is TableIntent.CellDoubleClicked -> openEditor(intent.index)
            is TableIntent.EditConfirmed -> confirmEdit(intent.index, intent.text)
            is TableIntent.EditorDraftChanged -> updateEditorDraft(intent.text)
            TableIntent.AppBackgrounded -> recoveryCoordinator.flush()
            TableIntent.EditDismissed -> dismissEditor()
        }
    }

    fun closeSession(onClosed: () -> Unit) = recoveryCoordinator.close(onClosed)

    private suspend fun generate() {
        val data = generateTableData(config)
        val recovered =
            withContext(backgroundDispatcher.value) {
                data.cells.map { RecoveredCell(text = it, isSelected = false) }
            }
        recoveryCells = recovered.toMutableList()
        cells = recovered.map { CellState(text = it.text) }
    }

    private fun restore(snapshot: TableRecoverySnapshot) {
        recoveryCells = snapshot.cells.toMutableList()
        cells =
            recoveryCells.map { cell ->
                CellState(text = cell.text, isSelected = cell.isSelected)
            }
        setState {
            copy(
                editingIndex = snapshot.editingIndex,
                editorDraft = snapshot.editorDraft,
            )
        }
    }

    private fun toggleColor(index: Int) {
        val cell = cells.getOrNull(index) ?: return
        cell.toggleSelection()
        recoveryCells[index] = recoveryCells[index].copy(isSelected = cell.isSelected)
        recoveryCoordinator.markDirty()
    }

    private fun openEditor(index: Int) {
        val cell = cells.getOrNull(index) ?: return
        setState { copy(editingIndex = index, editorDraft = cell.text) }
        recoveryCoordinator.markDirty()
    }

    private fun confirmEdit(
        index: Int,
        text: String,
    ) {
        val cell = cells.getOrNull(index) ?: return
        cell.updateText(text)
        recoveryCells[index] = recoveryCells[index].copy(text = cell.text)
        dismissEditor()
    }

    private fun updateEditorDraft(text: String) {
        if (state.value.editingIndex == null) return
        setState { copy(editorDraft = text.take(MAX_CELL_TEXT_LENGTH)) }
        recoveryCoordinator.markDirty()
    }

    private fun dismissEditor() {
        if (state.value.editingIndex == null) return
        setState { copy(editingIndex = null, editorDraft = null) }
        recoveryCoordinator.markDirty()
    }

    private fun currentSnapshot(): TableRecoverySnapshot =
        TableRecoverySnapshot(
            config = config,
            cells = recoveryCells.toList(),
            editingIndex = state.value.editingIndex,
            editorDraft = state.value.editorDraft,
        )
}
