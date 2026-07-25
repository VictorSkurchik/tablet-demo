package by.vsdev.tablet.demo.recovery.model

import by.vsdev.tablet.demo.domain.model.TableConfig
import org.junit.Test

class TableRecoverySnapshotTest {
    @Test(expected = IllegalArgumentException::class)
    fun `cell count must match configuration`() {
        TableRecoverySnapshot(
            config = TableConfig(2, 2),
            cells = listOf(RecoveredCell("only one", false)),
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `editing index must be inside table`() {
        TableRecoverySnapshot(
            config = TableConfig(1, 1),
            cells = listOf(RecoveredCell("value", false)),
            editingIndex = 1,
            editorDraft = "draft",
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `draft cannot exist without an active editor`() {
        TableRecoverySnapshot(
            config = TableConfig(1, 1),
            cells = listOf(RecoveredCell("value", false)),
            editorDraft = "draft",
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `cell value length is bounded`() {
        TableRecoverySnapshot(
            config = TableConfig(1, 1),
            cells =
                listOf(
                    RecoveredCell("x".repeat(MAX_RECOVERED_CELL_VALUE_LENGTH + 1), false),
                ),
        )
    }
}
