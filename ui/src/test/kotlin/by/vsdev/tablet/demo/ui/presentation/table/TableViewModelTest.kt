package by.vsdev.tablet.demo.ui.presentation.table

import by.vsdev.tablet.demo.domain.model.TableConfig
import by.vsdev.tablet.demo.domain.model.TableData
import by.vsdev.tablet.demo.domain.model.TableDataResult
import by.vsdev.tablet.demo.domain.repository.TableDataRepository
import by.vsdev.tablet.demo.domain.usecase.GenerateTableDataUseCase
import by.vsdev.tablet.demo.domain.util.BackgroundDispatcher
import by.vsdev.tablet.demo.recovery.TableRecoveryRepository
import by.vsdev.tablet.demo.recovery.model.RecoveredCell
import by.vsdev.tablet.demo.recovery.model.TableRecoverySnapshot
import by.vsdev.tablet.demo.ui.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TableViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val config = TableConfig(rows = 3, columns = 2)
    private val recoveryRepository = FakeRecoveryRepository()
    private val successfulRepository =
        object : TableDataRepository {
            override suspend fun generate(config: TableConfig): TableDataResult =
                TableDataResult.Success(successfulTableData(config))
        }

    private fun viewModel(
        repository: TableDataRepository = successfulRepository,
        sessionId: String = "session",
    ): TableViewModel =
        TableViewModel(
            config = config,
            sessionId = sessionId,
            generateTableData = GenerateTableDataUseCase(repository),
            recoveryRepository = recoveryRepository,
            backgroundDispatcher = BackgroundDispatcher(mainDispatcherRule.dispatcher),
        )

    @Test
    fun `loads the grid off the loading state`() =
        runTest(mainDispatcherRule.dispatcher) {
            val viewModel = viewModel()

            assertTrue(viewModel.state.value.isLoading)
            assertTrue(
                viewModel.state.value.cells
                    .isEmpty(),
            )

            advanceUntilIdle()

            assertFalse(viewModel.state.value.isLoading)
            assertEquals(config.cellCount, viewModel.state.value.cells.size)
            assertEquals(
                "c0",
                viewModel.state.value.cells[0]
                    .text,
            )
        }

    @Test
    fun `repeated single clicks toggle cell color`() =
        runTest(mainDispatcherRule.dispatcher) {
            val viewModel = viewModel()
            advanceUntilIdle()

            viewModel.onIntent(TableIntent.CellClicked(0))
            assertTrue(
                viewModel.state.value.cells[0]
                    .isSelected,
            )

            viewModel.onIntent(TableIntent.CellClicked(0))
            assertFalse(
                viewModel.state.value.cells[0]
                    .isSelected,
            )
        }

    @Test
    fun `double click opens the editor and confirm updates the cell`() =
        runTest(mainDispatcherRule.dispatcher) {
            val viewModel = viewModel()
            advanceUntilIdle()

            viewModel.onIntent(TableIntent.CellDoubleClicked(2))
            assertEquals(2, viewModel.state.value.editingIndex)

            viewModel.onIntent(TableIntent.EditConfirmed("edited"))
            assertEquals(
                "edited",
                viewModel.state.value.cells[2]
                    .text,
            )
            assertNull(viewModel.state.value.editingIndex)
        }

    @Test
    fun `confirmed edit is limited to the maximum cell text length`() =
        runTest(mainDispatcherRule.dispatcher) {
            val viewModel = viewModel()
            advanceUntilIdle()
            val oversizedText = "x".repeat(MAX_CELL_TEXT_LENGTH + 20)
            viewModel.onIntent(TableIntent.CellDoubleClicked(2))

            viewModel.onIntent(TableIntent.EditConfirmed(oversizedText))

            assertEquals(
                MAX_CELL_TEXT_LENGTH,
                viewModel.state.value.cells[2]
                    .text.length,
            )
            assertEquals(
                oversizedText.take(MAX_CELL_TEXT_LENGTH),
                viewModel.state.value.cells[2]
                    .text,
            )
        }

    @Test
    fun `dismiss closes the editor without changing the cell`() =
        runTest(mainDispatcherRule.dispatcher) {
            val viewModel = viewModel()
            advanceUntilIdle()
            val original =
                viewModel.state.value.cells[1]
                    .text

            viewModel.onIntent(TableIntent.CellDoubleClicked(1))
            viewModel.onIntent(TableIntent.EditDismissed)

            assertNull(viewModel.state.value.editingIndex)
            assertEquals(
                original,
                viewModel.state.value.cells[1]
                    .text,
            )
        }

    @Test
    fun `invalid cell indices are ignored`() =
        runTest(mainDispatcherRule.dispatcher) {
            val viewModel = viewModel()
            advanceUntilIdle()
            val originalCells = viewModel.state.value.cells

            viewModel.onIntent(TableIntent.CellClicked(-1))
            viewModel.onIntent(TableIntent.CellDoubleClicked(config.cellCount))
            viewModel.onIntent(TableIntent.EditConfirmed("edited"))

            assertEquals(originalCells, viewModel.state.value.cells)
            assertNull(viewModel.state.value.editingIndex)
        }

    @Test
    fun `view model recreation restores generated edited selected and draft state`() =
        runTest(mainDispatcherRule.dispatcher) {
            val original = viewModel()
            advanceUntilIdle()
            original.onIntent(TableIntent.CellClicked(1))
            original.onIntent(TableIntent.CellDoubleClicked(2))
            original.onIntent(TableIntent.EditConfirmed("edited"))
            original.onIntent(TableIntent.CellDoubleClicked(3))
            original.onIntent(TableIntent.EditorDraftChanged("unfinished draft"))
            advanceUntilIdle()

            val restored = viewModel()
            advanceUntilIdle()

            assertEquals(
                "c0",
                restored.state.value.cells[0]
                    .text,
            )
            assertTrue(
                restored.state.value.cells[1]
                    .isSelected,
            )
            assertEquals(
                "edited",
                restored.state.value.cells[2]
                    .text,
            )
            assertEquals(3, restored.state.value.editingIndex)
            assertEquals("unfinished draft", restored.state.value.editorDraft)
        }

    @Test
    fun `snapshot with a different configuration is rejected`() =
        runTest(mainDispatcherRule.dispatcher) {
            recoveryRepository.snapshots["session"] =
                TableRecoverySnapshot(
                    config = TableConfig(1, 1),
                    cells =
                        listOf(
                            RecoveredCell("wrong", true),
                        ),
                )

            val viewModel = viewModel()
            advanceUntilIdle()

            assertEquals(config.cellCount, viewModel.state.value.cells.size)
            assertEquals(
                "c0",
                viewModel.state.value.cells[0]
                    .text,
            )
        }

    @Test
    fun `explicit close removes transient snapshot before navigating`() =
        runTest(mainDispatcherRule.dispatcher) {
            val viewModel = viewModel()
            advanceUntilIdle()
            var navigated = false

            viewModel.closeSession { navigated = true }
            advanceUntilIdle()

            assertFalse(recoveryRepository.snapshots.containsKey("session"))
            assertTrue(navigated)
        }

    @Test
    fun `rapid draft changes are coalesced into one bounded write`() =
        runTest(mainDispatcherRule.dispatcher) {
            val viewModel = viewModel()
            advanceUntilIdle()
            viewModel.onIntent(TableIntent.CellDoubleClicked(0))
            advanceUntilIdle()
            recoveryRepository.saveCalls = 0

            repeat(50) { index ->
                viewModel.onIntent(TableIntent.EditorDraftChanged("draft-$index"))
            }
            advanceTimeBy(700)
            runCurrent()
            assertEquals(0, recoveryRepository.saveCalls)

            advanceTimeBy(50)
            runCurrent()

            assertEquals(1, recoveryRepository.saveCalls)
            assertEquals("draft-49", recoveryRepository.snapshots["session"]?.editorDraft)
        }

    @Test
    fun `background event flushes the latest draft without waiting for throttle`() =
        runTest(mainDispatcherRule.dispatcher) {
            val viewModel = viewModel()
            advanceUntilIdle()
            viewModel.onIntent(TableIntent.CellDoubleClicked(0))
            advanceUntilIdle()
            recoveryRepository.saveCalls = 0

            viewModel.onIntent(TableIntent.EditorDraftChanged("latest draft"))
            advanceTimeBy(100)
            viewModel.onIntent(TableIntent.AppBackgrounded)
            runCurrent()

            assertEquals(1, recoveryRepository.saveCalls)
            assertEquals("latest draft", recoveryRepository.snapshots["session"]?.editorDraft)
        }

    @Test
    fun `failed writes are retried and latest state remains recoverable`() =
        runTest(mainDispatcherRule.dispatcher) {
            val viewModel = viewModel()
            advanceUntilIdle()
            recoveryRepository.saveCalls = 0
            recoveryRepository.failuresRemaining = 2

            viewModel.onIntent(TableIntent.CellClicked(1))
            advanceUntilIdle()

            assertEquals(3, recoveryRepository.saveCalls)
            assertTrue(
                recoveryRepository.snapshots
                    .getValue("session")
                    .cells[1]
                    .isSelected,
            )
        }

    private class FakeRecoveryRepository : TableRecoveryRepository {
        val snapshots = mutableMapOf<String, TableRecoverySnapshot>()
        var failuresRemaining = 0
        var saveCalls = 0

        override suspend fun load(sessionId: String): TableRecoverySnapshot? = snapshots[sessionId]

        override suspend fun save(
            sessionId: String,
            snapshot: TableRecoverySnapshot,
        ): Boolean {
            saveCalls++
            if (failuresRemaining > 0) {
                failuresRemaining--
                return false
            }
            snapshots[sessionId] = snapshot
            return true
        }

        override suspend fun delete(sessionId: String): Boolean {
            snapshots.remove(sessionId)
            return true
        }

        override suspend fun cleanupExpired(): Boolean = true
    }

    @Test
    fun `unavailable generation exposes retryable error and retry recovers`() =
        runTest(mainDispatcherRule.dispatcher) {
            var attempts = 0
            val repository =
                object : TableDataRepository {
                    override suspend fun generate(config: TableConfig): TableDataResult {
                        attempts++
                        return if (attempts == 1) {
                            TableDataResult.GenerationUnavailable
                        } else {
                            TableDataResult.Success(successfulTableData(config))
                        }
                    }
                }
            val viewModel = viewModel(repository)

            advanceUntilIdle()
            assertTrue(viewModel.state.value.hasLoadError)

            viewModel.onIntent(TableIntent.RetryLoad)
            advanceUntilIdle()

            assertEquals(2, attempts)
            assertFalse(viewModel.state.value.hasLoadError)
            assertEquals(config.cellCount, viewModel.state.value.cells.size)
        }

    @Test
    fun `unexpected generation failure exposes an error and retry recovers`() =
        runTest(mainDispatcherRule.dispatcher) {
            var attempts = 0
            val repository =
                object : TableDataRepository {
                    override suspend fun generate(config: TableConfig): TableDataResult {
                        attempts++
                        check(attempts > 1) { "synthetic generation failure" }
                        return TableDataResult.Success(successfulTableData(config))
                    }
                }
            val viewModel = viewModel(repository)

            advanceUntilIdle()
            assertTrue(viewModel.state.value.hasLoadError)

            viewModel.onIntent(TableIntent.RetryLoad)
            advanceUntilIdle()

            assertEquals(2, attempts)
            assertFalse(viewModel.state.value.hasLoadError)
        }

    @Test
    fun `retry cancels in-flight generation before starting a replacement`() =
        runTest(mainDispatcherRule.dispatcher) {
            var attempts = 0
            var cancellations = 0
            val repository =
                object : TableDataRepository {
                    override suspend fun generate(config: TableConfig): TableDataResult {
                        attempts++
                        if (attempts == 1) {
                            try {
                                awaitCancellation()
                            } finally {
                                cancellations++
                            }
                        }
                        return TableDataResult.Success(successfulTableData(config, "replacement-"))
                    }
                }
            val viewModel = viewModel(repository)
            runCurrent()

            viewModel.onIntent(TableIntent.RetryLoad)
            advanceUntilIdle()

            assertEquals(2, attempts)
            assertEquals(1, cancellations)
            assertEquals(
                "replacement-0",
                viewModel.state.value.cells
                    .first()
                    .text,
            )
        }

    private companion object {
        fun successfulTableData(
            config: TableConfig,
            prefix: String = "c",
        ) = TableData(config, List(config.cellCount) { "$prefix$it" })
    }
}
