package by.vsdev.tablet.demo.ui.presentation.table.ui

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.HingeInfo
import androidx.compose.material3.adaptive.Posture
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.WindowSize
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import androidx.window.core.layout.computeWindowSizeClass
import by.vsdev.tablet.demo.domain.model.TableConfig
import by.vsdev.tablet.demo.ui.presentation.table.CellUiState
import by.vsdev.tablet.demo.ui.presentation.table.TableLoadState
import by.vsdev.tablet.demo.ui.presentation.table.TableUiState
import by.vsdev.tablet.demo.ui.theme.AppTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.math.abs

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
class TableAdaptiveLayoutTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun editorStateSurvivesResizeAndDismissReturnsToMainPane() {
        var windowWidth by mutableStateOf(EXPANDED_WIDTH)
        var posture by mutableStateOf(Posture())
        val horizontalHingePosture = offCenterHorizontalHingePosture()
        val verticalHingeBounds = offCenterVerticalHingeBounds()
        val verticalHingePosture = verticalSeparatingHingePosture(verticalHingeBounds)
        var state by
            mutableStateOf(
                TableUiState(
                    config = TableConfig(rows = 2, columns = 2),
                    loadState = TableLoadState.Content(List(4) { CellUiState("Value $it") }),
                    editingIndex = 0,
                ),
            )
        composeRule.setContent {
            val windowSize = DpSize(width = windowWidth.dp, height = WINDOW_HEIGHT.dp)
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.WindowSize(windowSize),
            ) {
                AppTheme {
                    TableScreen(
                        state = state,
                        onIntent = {},
                        onNavigateUp = {},
                        windowAdaptiveInfo = windowSize.toAdaptiveInfo(posture),
                    )
                }
            }
        }

        composeRule.onNodeWithTag(TABLE_MAIN_PANE_TAG).assertExists()
        composeRule.onNodeWithTag(TABLE_EDITOR_PANE_TAG).assertExists()
        composeRule.onNodeWithTag(TABLE_PANE_DRAG_HANDLE_TAG).assertHasClickAction()
        composeRule.onNodeWithTag(EDITOR_FIELD_TAG).performTextReplacement(DRAFT)

        composeRule.runOnIdle { posture = horizontalHingePosture }

        composeRule.onNodeWithTag(EDITOR_FIELD_TAG).assertTextContains(DRAFT)

        composeRule.runOnIdle { posture = verticalHingePosture }

        composeRule.onNodeWithTag(EDITOR_FIELD_TAG).assertTextContains(DRAFT)
        composeRule.onNodeWithTag(TABLE_PANE_DRAG_HANDLE_TAG).assertDoesNotExist()
        assertTablePanesAvoidVerticalHinge(verticalHingeBounds)

        composeRule.runOnIdle { posture = Posture() }

        composeRule.onNodeWithTag(EDITOR_FIELD_TAG).assertTextContains(DRAFT)
        composeRule.onNodeWithTag(TABLE_PANE_DRAG_HANDLE_TAG).assertHasClickAction()

        composeRule.runOnIdle { windowWidth = COMPACT_WIDTH }

        composeRule.onNodeWithTag(TABLE_MAIN_PANE_TAG).assertDoesNotExist()
        composeRule.onNodeWithTag(TABLE_EDITOR_PANE_TAG).assertExists()
        composeRule.onNodeWithTag(EDITOR_FIELD_TAG).assertTextContains(DRAFT)

        composeRule.runOnIdle { windowWidth = EXPANDED_WIDTH }

        composeRule.onNodeWithTag(TABLE_MAIN_PANE_TAG).assertExists()
        composeRule.onNodeWithTag(TABLE_EDITOR_PANE_TAG).assertExists()
        composeRule.onNodeWithTag(EDITOR_FIELD_TAG).assertTextContains(DRAFT)

        composeRule.runOnIdle { state = state.copy(editingIndex = null) }
        composeRule.waitForIdle()
        composeRule.runOnIdle { windowWidth = COMPACT_WIDTH }

        composeRule.onNodeWithTag(TABLE_MAIN_PANE_TAG).assertExists()
        composeRule.onNodeWithTag(TABLE_EDITOR_PANE_TAG).assertDoesNotExist()
    }

    private fun offCenterHorizontalHingePosture(): Posture {
        val hingeBounds =
            with(composeRule.density) {
                Rect(
                    left = 0f,
                    top = 210.dp.toPx(),
                    right = EXPANDED_WIDTH.dp.toPx(),
                    bottom = 270.dp.toPx(),
                )
            }
        return Posture(
            hingeList =
                listOf(
                    HingeInfo(
                        bounds = hingeBounds,
                        isFlat = true,
                        isVertical = false,
                        isSeparating = true,
                        isOccluding = true,
                    ),
                ),
        )
    }

    private fun offCenterVerticalHingeBounds(): Rect =
        with(composeRule.density) {
            Rect(
                left = 260.dp.toPx(),
                top = 0f,
                right = 300.dp.toPx(),
                bottom = WINDOW_HEIGHT.dp.toPx(),
            )
        }

    private fun verticalSeparatingHingePosture(hingeBounds: Rect): Posture =
        Posture(
            hingeList =
                listOf(
                    HingeInfo(
                        bounds = hingeBounds,
                        isFlat = false,
                        isVertical = true,
                        isSeparating = true,
                        isOccluding = true,
                    ),
                ),
        )

    private fun assertTablePanesAvoidVerticalHinge(hingeBounds: Rect) {
        val tableBounds =
            composeRule.onNodeWithTag(TABLE_MAIN_PANE_TAG).fetchSemanticsNode().boundsInRoot
        val editorBounds =
            composeRule.onNodeWithTag(TABLE_EDITOR_PANE_TAG).fetchSemanticsNode().boundsInRoot
        assertTrue(
            "Table panes should occupy opposite sides of an off-center vertical hinge",
            (
                tableBounds.right <= hingeBounds.left &&
                    editorBounds.left >= hingeBounds.right
            ) ||
                (
                    editorBounds.right <= hingeBounds.left &&
                        tableBounds.left >= hingeBounds.right
                ),
        )
    }

    @Test
    fun tabletopWithActiveEditorPlacesPanesInSeparateVerticalPartitions() {
        val windowSize = DpSize(width = TABLETOP_WIDTH.dp, height = TABLETOP_HEIGHT.dp)
        composeRule.setContent {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.WindowSize(windowSize),
            ) {
                AppTheme {
                    TableScreen(
                        state =
                            TableUiState(
                                config = TableConfig(rows = 2, columns = 2),
                                loadState =
                                    TableLoadState.Content(
                                        List(4) { CellUiState("Value $it") },
                                    ),
                                editingIndex = 0,
                            ),
                        onIntent = {},
                        onNavigateUp = {},
                        windowAdaptiveInfo =
                            windowSize.toAdaptiveInfo(
                                posture = Posture(isTabletop = true),
                            ),
                    )
                }
            }
        }

        val tableBounds =
            composeRule.onNodeWithTag(TABLE_MAIN_PANE_TAG).fetchSemanticsNode().boundsInRoot
        val editorBounds =
            composeRule.onNodeWithTag(TABLE_EDITOR_PANE_TAG).fetchSemanticsNode().boundsInRoot

        assertTrue(
            "Tabletop panes should occupy separate vertical partitions",
            tableBounds.bottom <= editorBounds.top || editorBounds.bottom <= tableBounds.top,
        )
        assertTrue(
            "Tabletop panes should share the available height evenly",
            abs(tableBounds.height - editorBounds.height) <= 1f,
        )
    }

    private fun DpSize.toAdaptiveInfo(posture: Posture = Posture()): WindowAdaptiveInfo =
        WindowAdaptiveInfo(
            windowSizeClass =
                WindowSizeClass.BREAKPOINTS_V2.computeWindowSizeClass(
                    widthDp = width.value.toInt(),
                    heightDp = height.value.toInt(),
                ),
            windowPosture = posture,
        )

    private companion object {
        const val COMPACT_WIDTH = 400
        const val EXPANDED_WIDTH = 900
        const val WINDOW_HEIGHT = 700
        const val TABLETOP_WIDTH = 900
        const val TABLETOP_HEIGHT = 1_200
        const val DRAFT = "Draft survives resize"
    }
}
