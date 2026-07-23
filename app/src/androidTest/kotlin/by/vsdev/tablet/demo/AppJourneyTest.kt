package by.vsdev.tablet.demo

import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import by.vsdev.tablet.demo.recovery.TableRecoveryRepository
import by.vsdev.tablet.demo.recovery.model.TableRecoverySnapshot
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext

@RunWith(AndroidJUnit4::class)
class AppJourneyTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun validDimensionsBuildAndOpenTable() {
        buildTable(rows = 2, columns = 2)
    }

    @Test
    fun editedCellRetainsValueAfterScrollingAwayAndBack() {
        buildLargeTable()

        composeRule
            .onNodeWithContentDescription(cellPrefix(row = 1, column = 1), substring = true)
            .performTouchInput { doubleClick() }
        composeRule
            .onNode(hasSetTextAction() and hasText("Value"))
            .performTextReplacement(EDITED_VALUE)
        composeRule.onNodeWithText("Save").performClick()

        waitForCell(row = 1, column = 1, value = EDITED_VALUE)
        scrollToCell(index = LAST_CELL_INDEX, row = LARGE_TABLE_ROWS, column = LARGE_TABLE_COLUMNS)
        scrollToCell(index = 0, row = 1, column = 1, value = EDITED_VALUE)
    }

    @Test
    fun selectedCellsRemainSelectedAfterScrollingAwayAndBack() {
        buildLargeTable()
        val selectedCells = listOf(1 to 1, 2 to 2)

        selectedCells.forEach { (row, column) ->
            selectCell(row, column)
        }

        scrollToCell(index = LAST_CELL_INDEX, row = LARGE_TABLE_ROWS, column = LARGE_TABLE_COLUMNS)
        scrollToCell(index = 0, row = 1, column = 1)

        selectedCells.forEach { (row, column) ->
            composeRule
                .onNodeWithContentDescription(cellPrefix(row, column), substring = true)
                .assertIsSelected()
        }
    }

    @Test
    fun newViewModelRestoresTableSelectionAndActiveEditorDraftFromDisk() {
        recoveryDirectory().deleteRecursively()
        buildTable(rows = 2, columns = 2)
        waitForRecoverySnapshot()
        editCell(row = 1, column = 1, value = EDITED_VALUE)
        selectCell(row = 1, column = 2)
        composeRule
            .onNodeWithContentDescription(cellPrefix(row = 1, column = 1), substring = true)
            .performTouchInput { doubleClick() }
        composeRule.onNode(hasSetTextAction() and hasText(EDITED_VALUE)).performTextReplacement(DRAFT_VALUE)
        waitForRecoverySnapshot { snapshot ->
            snapshot.editorDraft == DRAFT_VALUE &&
                snapshot.cells[0].text == EDITED_VALUE &&
                snapshot.cells[1].isSelected
        }

        composeRule.activityRule.scenario.onActivity { it.viewModelStore.clear() }
        composeRule.activityRule.scenario.recreate()

        composeRule.onNodeWithText("Table · 2 × 2").assertExists()
        composeRule.onNode(hasSetTextAction() and hasText(DRAFT_VALUE)).assertExists()
        composeRule
            .onNodeWithContentDescription(cellPrefix(row = 1, column = 2), substring = true)
            .assertIsSelected()
        composeRule.onNodeWithText("Cancel").performClick()
        waitForCell(row = 1, column = 1, value = EDITED_VALUE)
    }

    @Test
    fun activityRecreationRetainsGridScrollPosition() {
        buildLargeTable()
        scrollToCell(
            index = LAST_CELL_INDEX,
            row = LARGE_TABLE_ROWS,
            column = LARGE_TABLE_COLUMNS,
        )

        composeRule.activityRule.scenario.recreate()

        waitForCell(row = LARGE_TABLE_ROWS, column = LARGE_TABLE_COLUMNS)
    }

    @Test
    fun tableBackNavigationReturnsToSetup() {
        recoveryDirectory().deleteRecursively()
        buildTable(rows = 2, columns = 2)
        waitForRecoverySnapshot()

        pressBack()

        composeRule.onNodeWithText("Build table").assertExists()
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) {
            recoveryDirectory()
                .listFiles()
                .orEmpty()
                .none { it.extension == "snapshot" }
        }
    }

    private fun selectCell(
        row: Int,
        column: Int,
    ) {
        val description = cellPrefix(row, column)
        val cell = composeRule.onNodeWithContentDescription(description, substring = true)
        cell.performClick()

        composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) {
            composeRule
                .onAllNodes(hasContentDescription(description, substring = true) and isSelected())
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        cell.assertIsSelected()
    }

    private fun editCell(
        row: Int,
        column: Int,
        value: String,
    ) {
        composeRule
            .onNodeWithContentDescription(cellPrefix(row, column), substring = true)
            .performTouchInput { doubleClick() }
        composeRule.onNode(hasSetTextAction() and hasText("Value")).performTextReplacement(value)
        composeRule.onNodeWithText("Save").performClick()
        waitForCell(row, column, value)
    }

    private fun buildLargeTable() {
        buildTable(rows = LARGE_TABLE_ROWS, columns = LARGE_TABLE_COLUMNS)
    }

    private fun buildTable(
        rows: Int,
        columns: Int,
    ) {
        composeRule.onNode(hasSetTextAction() and hasText("Rows")).performTextInput(rows.toString())
        composeRule
            .onNode(hasSetTextAction() and hasText("Columns"))
            .performTextInput(columns.toString())
        composeRule.onNodeWithText("Build table").performClick()

        val title = "Table · $rows × $columns"
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) {
            composeRule.onAllNodesWithText(title).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(title).assertExists()
        waitForCell(row = 1, column = 1)
    }

    private fun scrollToCell(
        index: Int,
        row: Int,
        column: Int,
        value: String? = null,
    ) {
        composeRule.onNode(hasScrollToIndexAction()).performScrollToIndex(index)
        waitForCell(row, column, value)
    }

    private fun waitForCell(
        row: Int,
        column: Int,
        value: String? = null,
    ) {
        val description = value?.let { "${cellPrefix(row, column)} $it" } ?: cellPrefix(row, column)
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) {
            composeRule
                .onAllNodesWithContentDescription(
                    description,
                    substring = value == null,
                ).fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeRule
            .onNodeWithContentDescription(
                description,
                substring = value == null,
            ).assertExists()
    }

    private fun cellPrefix(
        row: Int,
        column: Int,
    ): String = "Row $row, column $column:"

    private fun recoveryDirectory() =
        InstrumentationRegistry
            .getInstrumentation()
            .targetContext.noBackupFilesDir
            .resolve("table-recovery")

    private fun waitForRecoverySnapshot(predicate: (TableRecoverySnapshot) -> Boolean = { true }) {
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) {
            val file =
                recoveryDirectory()
                    .listFiles()
                    .orEmpty()
                    .firstOrNull { it.extension == "snapshot" && it.length() > 0L }
                    ?: return@waitUntil false
            val repository = GlobalContext.get().get<TableRecoveryRepository>()
            val snapshot = runBlocking { repository.load(file.nameWithoutExtension) }
            snapshot != null && predicate(snapshot)
        }
    }

    private companion object {
        const val TIMEOUT_MILLIS = 5_000L
        const val LARGE_TABLE_ROWS = 50
        const val LARGE_TABLE_COLUMNS = 4
        const val LAST_CELL_INDEX = LARGE_TABLE_ROWS * LARGE_TABLE_COLUMNS - 1
        const val EDITED_VALUE = "Edited value"
        const val DRAFT_VALUE = "Unfinished draft"
    }
}
