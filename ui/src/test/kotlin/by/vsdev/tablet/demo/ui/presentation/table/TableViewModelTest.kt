package by.vsdev.tablet.demo.ui.presentation.table

import by.vsdev.tablet.demo.domain.model.TableConfig
import by.vsdev.tablet.demo.domain.model.TableData
import by.vsdev.tablet.demo.domain.repository.TableDataRepository
import by.vsdev.tablet.demo.domain.usecase.GenerateTableDataUseCase
import by.vsdev.tablet.demo.domain.util.BackgroundDispatcher
import by.vsdev.tablet.demo.ui.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
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

    private fun viewModel(): TableViewModel {
        val repository =
            object : TableDataRepository {
                override suspend fun generate(config: TableConfig): TableData =
                    TableData(config, List(config.cellCount) { "c$it" })
            }
        return TableViewModel(
            config = config,
            generateTableData = GenerateTableDataUseCase(repository),
            backgroundDispatcher = BackgroundDispatcher(mainDispatcherRule.dispatcher),
        )
    }

    @Test
    fun `loads the grid off the loading state`() =
        runTest(mainDispatcherRule.dispatcher) {
            val viewModel = viewModel()

            assertTrue(viewModel.state.value.isLoading)
            assertTrue(viewModel.cells.isEmpty())

            advanceUntilIdle()

            assertFalse(viewModel.state.value.isLoading)
            assertEquals(config.cellCount, viewModel.cells.size)
            assertEquals("c0", viewModel.cells[0].text)
        }

    @Test
    fun `repeated single clicks toggle cell color`() =
        runTest(mainDispatcherRule.dispatcher) {
            val viewModel = viewModel()
            advanceUntilIdle()

            viewModel.onIntent(TableIntent.CellClicked(0))
            assertTrue(viewModel.cells[0].isSelected)

            viewModel.onIntent(TableIntent.CellClicked(0))
            assertFalse(viewModel.cells[0].isSelected)
        }

    @Test
    fun `double click opens the editor and confirm updates the cell`() =
        runTest(mainDispatcherRule.dispatcher) {
            val viewModel = viewModel()
            advanceUntilIdle()

            viewModel.onIntent(TableIntent.CellDoubleClicked(2))
            assertEquals(2, viewModel.state.value.editingIndex)

            viewModel.onIntent(TableIntent.EditConfirmed(2, "edited"))
            assertEquals("edited", viewModel.cells[2].text)
            assertNull(viewModel.state.value.editingIndex)
        }

    @Test
    fun `confirmed edit is limited to the maximum cell text length`() =
        runTest(mainDispatcherRule.dispatcher) {
            val viewModel = viewModel()
            advanceUntilIdle()
            val oversizedText = "x".repeat(MAX_CELL_TEXT_LENGTH + 20)

            viewModel.onIntent(TableIntent.EditConfirmed(2, oversizedText))

            assertEquals(MAX_CELL_TEXT_LENGTH, viewModel.cells[2].text.length)
            assertEquals(oversizedText.take(MAX_CELL_TEXT_LENGTH), viewModel.cells[2].text)
        }

    @Test
    fun `dismiss closes the editor without changing the cell`() =
        runTest(mainDispatcherRule.dispatcher) {
            val viewModel = viewModel()
            advanceUntilIdle()
            val original = viewModel.cells[1].text

            viewModel.onIntent(TableIntent.CellDoubleClicked(1))
            viewModel.onIntent(TableIntent.EditDismissed)

            assertNull(viewModel.state.value.editingIndex)
            assertEquals(original, viewModel.cells[1].text)
        }

    @Test
    fun `invalid cell indices are ignored`() =
        runTest(mainDispatcherRule.dispatcher) {
            val viewModel = viewModel()
            advanceUntilIdle()
            val originalCells = viewModel.cells.map { it.text to it.isSelected }

            viewModel.onIntent(TableIntent.CellClicked(-1))
            viewModel.onIntent(TableIntent.CellDoubleClicked(config.cellCount))
            viewModel.onIntent(TableIntent.EditConfirmed(config.cellCount, "edited"))

            assertEquals(originalCells, viewModel.cells.map { it.text to it.isSelected })
            assertNull(viewModel.state.value.editingIndex)
        }
}
