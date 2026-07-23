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
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

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
    fun maximumDimensionsBuildAndReachLastCell() {
        buildTable(rows = MAXIMUM_TABLE_ROWS, columns = MAXIMUM_TABLE_COLUMNS)

        scrollToCell(
            index = MAXIMUM_TABLE_ROWS * MAXIMUM_TABLE_COLUMNS - 1,
            row = MAXIMUM_TABLE_ROWS,
            column = MAXIMUM_TABLE_COLUMNS,
        )
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

    private companion object {
        const val TIMEOUT_MILLIS = 5_000L
        const val LARGE_TABLE_ROWS = 50
        const val LARGE_TABLE_COLUMNS = 4
        const val MAXIMUM_TABLE_ROWS = 1_000
        const val MAXIMUM_TABLE_COLUMNS = 6
        const val LAST_CELL_INDEX = LARGE_TABLE_ROWS * LARGE_TABLE_COLUMNS - 1
        const val EDITED_VALUE = "Edited value"
    }
}
