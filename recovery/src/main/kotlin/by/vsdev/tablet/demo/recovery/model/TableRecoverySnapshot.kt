package by.vsdev.tablet.demo.recovery.model

import by.vsdev.tablet.demo.domain.model.TableConfig

const val MAX_RECOVERED_CELL_VALUE_LENGTH = 100

/**
 * Version-independent application recovery DTO.
 *
 * It intentionally lives outside domain: selection and the active editor are transient screen state,
 * not business entities.
 */
data class TableRecoverySnapshot(
    val config: TableConfig,
    val cells: List<RecoveredCell>,
    val editingIndex: Int? = null,
    val editorDraft: String? = null,
) {
    init {
        require(cells.size == config.cellCount) {
            "cells size ${cells.size} must equal config.cellCount (${config.cellCount})"
        }
        require(cells.all { it.text.length <= MAX_RECOVERED_CELL_VALUE_LENGTH }) {
            "cell text must not exceed $MAX_RECOVERED_CELL_VALUE_LENGTH characters"
        }
        require(editingIndex == null || editingIndex in cells.indices) {
            "editingIndex $editingIndex is outside the table"
        }
        require(editorDraft == null || editorDraft.length <= MAX_RECOVERED_CELL_VALUE_LENGTH) {
            "editorDraft must not exceed $MAX_RECOVERED_CELL_VALUE_LENGTH characters"
        }
        require((editingIndex == null) == (editorDraft == null)) {
            "editingIndex and editorDraft must either both be present or both be absent"
        }
    }
}

data class RecoveredCell(
    val text: String,
    val isSelected: Boolean,
)
