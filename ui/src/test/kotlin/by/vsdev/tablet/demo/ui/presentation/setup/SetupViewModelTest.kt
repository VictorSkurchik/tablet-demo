package by.vsdev.tablet.demo.ui.presentation.setup

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.snapshots.Snapshot
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import by.vsdev.tablet.demo.domain.model.TableConfig
import by.vsdev.tablet.demo.domain.usecase.FieldError
import by.vsdev.tablet.demo.domain.usecase.ValidateTableConfigUseCase
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
class SetupViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun viewModel(handle: SavedStateHandle = SavedStateHandle()) =
        SetupViewModel(ValidateTableConfigUseCase(), handle)

    @Test
    fun `initial state cannot build`() {
        val state = viewModel().state.value
        assertFalse(state.canBuild)
        assertNull(state.rowsError)
        assertNull(state.columnsError)
    }

    @Test
    fun `valid inputs enable build with no errors`() =
        runTest(mainDispatcherRule.dispatcher) {
            val vm = viewModel()
            vm.enter(rows = "500", columns = "3")
            advanceUntilIdle()

            val state = vm.state.value
            assertTrue(state.canBuild)
            assertNull(state.rowsError)
            assertNull(state.columnsError)
        }

    @Test
    fun `out-of-range column shows error and blocks build`() =
        runTest(mainDispatcherRule.dispatcher) {
            val vm = viewModel()
            vm.enter(rows = "10", columns = "7")
            advanceUntilIdle()

            val state = vm.state.value
            assertFalse(state.canBuild)
            assertEquals(FieldError.ABOVE_MAX, state.columnsError)
        }

    @Test
    fun `clearing invalid field hides its error but blocks build`() =
        runTest(mainDispatcherRule.dispatcher) {
            val vm = viewModel()
            vm.enter(rows = "10", columns = "7")
            advanceUntilIdle()
            assertEquals(FieldError.ABOVE_MAX, vm.state.value.columnsError)

            vm.columnsInput.setTextAndPlaceCursorAtEnd("")
            Snapshot.sendApplyNotifications()
            advanceUntilIdle()

            val state = vm.state.value
            assertFalse(state.canBuild)
            assertNull(state.columnsError)
        }

    @Test
    fun `build with valid inputs emits navigation with the parsed config`() =
        runTest(mainDispatcherRule.dispatcher) {
            val vm = viewModel()
            vm.enter(rows = "250", columns = "4")
            advanceUntilIdle()

            vm.navigation.test {
                vm.onIntent(SetupIntent.BuildClicked)
                assertEquals(TableConfig(rows = 250, columns = 4), awaitItem())
                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    fun `build with empty inputs reveals errors and emits nothing`() =
        runTest(mainDispatcherRule.dispatcher) {
            val vm = viewModel()

            vm.navigation.test {
                vm.onIntent(SetupIntent.BuildClicked)
                expectNoEvents()
                val state = vm.state.value
                assertEquals(FieldError.EMPTY, state.rowsError)
                assertEquals(FieldError.EMPTY, state.columnsError)
                assertFalse(state.canBuild)
            }
        }

    @Test
    fun `inputs survive recreation through saved state`() =
        runTest(mainDispatcherRule.dispatcher) {
            val handle = SavedStateHandle()
            val first = viewModel(handle)
            first.enter(rows = "1000", columns = "6")
            advanceUntilIdle()

            val restoredViewModel = viewModel(handle)
            val restored = restoredViewModel.state.value

            assertEquals("1000", restoredViewModel.rowsInput.text.toString())
            assertEquals("6", restoredViewModel.columnsInput.text.toString())
            assertTrue(restored.canBuild)
        }

    private fun SetupViewModel.enter(
        rows: String,
        columns: String,
    ) {
        rowsInput.setTextAndPlaceCursorAtEnd(rows)
        columnsInput.setTextAndPlaceCursorAtEnd(columns)
    }
}
