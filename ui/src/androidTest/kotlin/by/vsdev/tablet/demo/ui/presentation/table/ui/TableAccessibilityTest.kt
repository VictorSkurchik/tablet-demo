package by.vsdev.tablet.demo.ui.presentation.table.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import by.vsdev.tablet.demo.domain.model.TableConfig
import by.vsdev.tablet.demo.ui.presentation.table.CellUiState
import by.vsdev.tablet.demo.ui.presentation.table.TableIntent
import by.vsdev.tablet.demo.ui.presentation.table.TableLoadState
import by.vsdev.tablet.demo.ui.presentation.table.TableUiState
import by.vsdev.tablet.demo.ui.theme.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class TableAccessibilityTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun gridExposesCollectionAndCellCoordinates() {
        composeRule.setContent {
            AppTheme {
                TableGrid(
                    columns = 2,
                    cells = List(4) { CellUiState("Cell $it") },
                    onIntent = {},
                )
            }
        }

        composeRule
            .onNode(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.CollectionInfo,
                    CollectionInfo(rowCount = 2, columnCount = 2),
                ),
            ).assertExists()
        composeRule
            .onNode(
                matcher =
                    SemanticsMatcher("second cell exposes row and column") { node ->
                        node.config
                            .getOrNull(SemanticsProperties.CollectionItemInfo)
                            ?.let { it.rowIndex == 0 && it.columnIndex == 1 } == true
                    },
                useUnmergedTree = true,
            ).assertExists()
    }

    @Test
    fun gridRestoresFocusAfterLayingOutAnOffscreenCell() {
        var restoreFocusIndex by mutableStateOf<Int?>(null)
        lateinit var inputModeManager: InputModeManager
        composeRule.setContent {
            inputModeManager = LocalInputModeManager.current
            AppTheme {
                TableGrid(
                    columns = 2,
                    cells = List(100) { CellUiState("Cell $it") },
                    restoreFocusIndex = restoreFocusIndex,
                    onIntent = {},
                )
            }
        }
        composeRule.runOnIdle {
            inputModeManager.requestInputMode(InputMode.Keyboard)
            restoreFocusIndex = 99
        }

        composeRule
            .onNodeWithContentDescription("Row 50, column 2: Cell 99")
            .assertIsFocused()
    }

    @Test
    fun gridRestoresFocusWithoutMovingAnAlreadyVisibleCell() {
        var restoreFocusIndex by mutableStateOf<Int?>(null)
        lateinit var inputModeManager: InputModeManager
        composeRule.setContent {
            inputModeManager = LocalInputModeManager.current
            AppTheme {
                Box(Modifier.height(360.dp)) {
                    TableGrid(
                        columns = 2,
                        cells = List(100) { CellUiState("Cell $it") },
                        restoreFocusIndex = restoreFocusIndex,
                        onIntent = {},
                    )
                }
            }
        }
        val target =
            composeRule.onNodeWithContentDescription("Row 3, column 1: Cell 4")
        val topBeforeRestore = target.fetchSemanticsNode().boundsInRoot.top

        composeRule.runOnIdle {
            inputModeManager.requestInputMode(InputMode.Keyboard)
            restoreFocusIndex = 4
        }

        target.assertIsFocused()
        val topAfterRestore = target.fetchSemanticsNode().boundsInRoot.top
        assertEquals(topBeforeRestore, topAfterRestore, 1f)
    }

    @Test
    fun loadingStateIsAnnouncedPolitely() {
        setTableScreen(TableUiState(config = TableConfig(2, 2)))

        composeRule
            .onNode(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.LiveRegion,
                    LiveRegionMode.Polite,
                ),
            ).assertExists()
    }

    @Test
    fun loadErrorExposesErrorAndRetryAction() {
        var receivedIntent: TableIntent? = null
        setTableScreen(
            state =
                TableUiState(
                    config = TableConfig(2, 2),
                    loadState = TableLoadState.Error,
                ),
            onIntent = { receivedIntent = it },
        )

        composeRule
            .onNode(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.Error,
                    "Couldn’t generate the table.",
                ),
            ).assertExists()
        composeRule.onNodeWithText("Try again").performClick()

        composeRule.runOnIdle { assertEquals(TableIntent.RetryLoad, receivedIntent) }
    }

    private fun setTableScreen(
        state: TableUiState,
        onIntent: (TableIntent) -> Unit = {},
    ) {
        composeRule.setContent {
            AppTheme {
                TableScreen(
                    state = state,
                    onIntent = onIntent,
                    onNavigateUp = {},
                )
            }
        }
    }
}
