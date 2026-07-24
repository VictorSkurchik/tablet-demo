package by.vsdev.tablet.demo.ui.presentation.table

import by.vsdev.tablet.demo.domain.model.TableConfig
import by.vsdev.tablet.demo.domain.model.TableData
import by.vsdev.tablet.demo.domain.repository.TableDataRepository
import by.vsdev.tablet.demo.domain.usecase.GenerateTableDataUseCase
import by.vsdev.tablet.demo.domain.util.BackgroundDispatcher
import by.vsdev.tablet.demo.ui.MainDispatcherRule
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.coroutines.CoroutineContext

@OptIn(ExperimentalCoroutinesApi::class)
class TableViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val config = TableConfig(rows = 3, columns = 2)
    private val successfulRepository =
        object : TableDataRepository {
            override suspend fun generate(config: TableConfig): TableData = successfulTableData(config)
        }

    private fun viewModel(
        repository: TableDataRepository = successfulRepository,
        backgroundDispatcher: CoroutineDispatcher = mainDispatcherRule.dispatcher,
    ): TableViewModel =
        TableViewModel(
            config = config,
            generateTableData = GenerateTableDataUseCase(repository),
            backgroundDispatcher = BackgroundDispatcher(backgroundDispatcher),
        )

    @Test
    fun `loads the grid off the loading state`() =
        runTest(mainDispatcherRule.dispatcher) {
            val mappingDispatcher = CountingDispatcher(mainDispatcherRule.dispatcher)
            val viewModel = viewModel(backgroundDispatcher = mappingDispatcher)

            assertEquals(TableLoadState.Loading, viewModel.state.value.loadState)
            assertTrue(
                viewModel.state.value.cells
                    .isEmpty(),
            )

            advanceUntilIdle()

            assertTrue(mappingDispatcher.dispatchCount > 0)
            assertTrue(viewModel.state.value.loadState is TableLoadState.Content)
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
    fun `unexpected generation failure exposes an error and retry recovers`() =
        runTest(mainDispatcherRule.dispatcher) {
            var attempts = 0
            val repository =
                object : TableDataRepository {
                    override suspend fun generate(config: TableConfig): TableData {
                        attempts++
                        check(attempts > 1) { "synthetic generation failure" }
                        return successfulTableData(config)
                    }
                }
            val viewModel = viewModel(repository)

            advanceUntilIdle()
            assertEquals(TableLoadState.Error, viewModel.state.value.loadState)

            viewModel.onIntent(TableIntent.RetryLoad)
            advanceUntilIdle()

            assertEquals(2, attempts)
            assertTrue(viewModel.state.value.loadState is TableLoadState.Content)
        }

    @Test
    fun `retry cancels in-flight generation before starting a replacement`() =
        runTest(mainDispatcherRule.dispatcher) {
            var attempts = 0
            var cancellations = 0
            val repository =
                object : TableDataRepository {
                    override suspend fun generate(config: TableConfig): TableData {
                        attempts++
                        if (attempts == 1) {
                            try {
                                awaitCancellation()
                            } finally {
                                cancellations++
                            }
                        }
                        return successfulTableData(config, "replacement-")
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

    private class CountingDispatcher(
        private val delegate: CoroutineDispatcher,
    ) : CoroutineDispatcher() {
        var dispatchCount = 0
            private set

        override fun dispatch(
            context: CoroutineContext,
            block: Runnable,
        ) {
            dispatchCount++
            delegate.dispatch(context, block)
        }
    }
}
